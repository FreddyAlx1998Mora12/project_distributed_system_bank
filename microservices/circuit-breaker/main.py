"""
Circuit Breaker (Máquina 1). Recibe la petición ya enrutada por el balanceador
y decide si reenviarla al nodo Spring Boot elegido o rechazarla de inmediato
según el estado del breaker de ese nodo.
"""
import httpx
from fastapi import FastAPI, HTTPException
from circuit_breaker import CircuitBreakerRegistry

app = FastAPI(title="Circuit Breaker Service")
registry = CircuitBreakerRegistry()

OPERATION_PATHS = {
    "deposit": "/transactions/deposit",
    "withdraw": "/transactions/withdraw",
    "transfer": "/transactions/transfer",
}


@app.post("/forward/{node_id}/{operation}")
async def forward(node_id: str, operation: str, body: dict):
    breaker = registry.get(node_id)

    if not breaker.allow_request():
        raise HTTPException(
            status_code=503,
            detail=f"Circuit breaker OPEN para nodo {node_id}: petición rechazada (fail-fast)",
        )

    target_url = body["targetUrl"]
    payload = body["payload"]
    path = OPERATION_PATHS.get(operation)
    if path is None:
        raise HTTPException(status_code=400, detail=f"Operación desconocida: {operation}")

    try:
        async with httpx.AsyncClient(timeout=4.0) as client:
            resp = await client.post(f"{target_url}{path}", json=payload)
        if resp.status_code >= 500:
            breaker.record_failure()
        else:
            breaker.record_success()
        return resp.json()
    except httpx.RequestError as e:
        breaker.record_failure()
        raise HTTPException(status_code=502, detail=f"Nodo {node_id} no responde: {e}")


@app.get("/status")
def status():
    return registry.snapshot_all()


@app.get("/health")
def health():
    return {"status": "UP", "service": "circuit-breaker"}
