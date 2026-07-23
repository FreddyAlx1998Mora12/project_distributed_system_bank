"""
Monitor del cluster (Máquina 5): heartbeat + quorum + logs centralizados.
Expone a los demás servicios (Load Balancer) el estado de salud/carga de
cada nodo, y evalúa continuamente el quorum para detectar caídas.
"""
from fastapi import FastAPI
from pydantic import BaseModel

from heartbeat import HeartbeatMonitor
from quorum_manager import QuorumManager
from logger import log_event

app = FastAPI(title="Cluster Monitor")
heartbeat_monitor = HeartbeatMonitor()
quorum_manager = QuorumManager()


class HeartbeatPayload(BaseModel):
    nodeId: str
    role: str
    status: str


@app.post("/heartbeat")
def receive_heartbeat(payload: HeartbeatPayload):
    heartbeat_monitor.record(payload.nodeId, payload.role)
    return {"received": True}


@app.get("/nodes/status")
def nodes_status():
    """Consumido por el Load Balancer (IA) para conocer salud/carga real de cada nodo."""
    return heartbeat_monitor.snapshot()


@app.get("/quorum")
def quorum_status():
    snapshot = heartbeat_monitor.snapshot()
    return quorum_manager.evaluate(snapshot)


@app.get("/health")
def health():
    return {"status": "UP", "service": "monitor"}
