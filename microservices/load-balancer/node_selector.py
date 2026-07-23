"""
Registro de nodos disponibles. Se alimenta de:
  - configuración estática inicial (config/cluster.yml montado como env/JSON)
  - actualizaciones periódicas desde el Monitor (heartbeats -> salud/carga real)
"""
import os
import json
import httpx
from ia_model import NodeMetrics, AINodeSelector

MONITOR_URL = os.getenv("MONITOR_URL", "http://monitor:8500")


class NodeRegistry:
    def __init__(self):
        self.selector = AINodeSelector()
        self.nodes: dict[str, NodeMetrics] = self._load_static_config()

    def _load_static_config(self) -> dict[str, NodeMetrics]:
        raw = os.getenv("NODES_CONFIG", json.dumps([
            {"node_id": "node-2", "url": "http://transaction-service-2:8080", "weight": 1.0, "is_primary": True},
            {"node_id": "node-3", "url": "http://transaction-service-3:8080", "weight": 0.6, "is_primary": False},
            {"node_id": "node-4", "url": "http://transaction-service-4:8080", "weight": 0.6, "is_primary": False},
        ]))
        parsed = json.loads(raw)
        return {
            n["node_id"]: NodeMetrics(
                node_id=n["node_id"], url=n["url"], weight=n["weight"],
                current_load=0.0, latency_ms=50.0, is_primary=n["is_primary"], healthy=True,
            )
            for n in parsed
        }

    async def refresh_from_monitor(self):
        """Consulta al Monitor (Máquina 5) el estado real de salud/carga de cada nodo."""
        try:
            async with httpx.AsyncClient(timeout=2.0) as client:
                resp = await client.get(f"{MONITOR_URL}/nodes/status")
                resp.raise_for_status()
                statuses = resp.json()
            for node_id, status in statuses.items():
                if node_id in self.nodes:
                    node = self.nodes[node_id]
                    node.healthy = status.get("healthy", node.healthy)
                    node.current_load = status.get("load", node.current_load)
                    node.latency_ms = status.get("latency_ms", node.latency_ms)
        except Exception:
            # Si el monitor no responde, se conserva el último estado conocido
            # (fail-safe: no se tumba el balanceador por falta de métricas frescas).
            pass

    def choose(self) -> NodeMetrics | None:
        return self.selector.select_node(list(self.nodes.values()))

    def explain(self) -> list[dict]:
        return self.selector.explain(list(self.nodes.values()))
