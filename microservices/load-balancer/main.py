"""
Balanceador de carga con IA (Máquina 1).
Recibe la petición ya enrutada por el API Gateway, elige el nodo óptimo
mediante AINodeSelector, y reenvía la petición A TRAVÉS del Circuit Breaker
(nunca directo al nodo), respetando el flujo del diagrama:
    Gateway -> Load Balancer(IA) -> Circuit Breaker -> nodo Spring Boot
"""
import os
import httpx
from fastapi import FastAPI, HTTPException
from node_selector import NodeRegistry

app = FastAPI(title="AI Load Balancer")
registry = NodeRegistry()

CIRCUIT_BREAKER_URL = os.getenv("CIRCUIT_BREAKER_URL", "http://circuit-breaker:9100")


@app.on_event("startup")
async def startup():
    await registry.refresh_from_monitor()


@app.post("/route/{operation}")
async def route(operation: str, body: dict):
    await registry.refresh_from_monitor()
    node = registry.choose()
    if node is None:
        raise HTTPException(status_code=503, detail="No hay nodos saludables disponibles")

    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            resp = await client.post(
                f"{CIRCUIT_BREAKER_URL}/forward/{node.node_id}/{operation}",
                json={"targetUrl": node.url, "payload": body},
            )
        return resp.json()
    except httpx.RequestError as e:
        raise HTTPException(status_code=502, detail=f"Error comunicando con circuit breaker: {e}")


@app.get("/nodes/explain")
async def explain():
    """Endpoint de observabilidad: muestra el scoring de cada nodo (transparencia del modelo IA)."""
    await registry.refresh_from_monitor()
    return registry.explain()


@app.get("/health")
def health():
    return {"status": "UP", "service": "load-balancer"}
