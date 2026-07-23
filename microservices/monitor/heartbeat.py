"""
Receptor y evaluador de heartbeats de los nodos de transacción.
Cada nodo hace POST /heartbeat cada `heartbeat.interval-ms` (2s por defecto).
Si un nodo no reporta en más de HEARTBEAT_TIMEOUT segundos, se marca DOWN.
"""
import time
import threading
from logger import log_event

HEARTBEAT_TIMEOUT_SECONDS = 6.0  # ~3x el intervalo de envío


class HeartbeatMonitor:
    def __init__(self):
        self._last_seen: dict[str, float] = {}
        self._roles: dict[str, str] = {}
        self._lock = threading.Lock()

    def record(self, node_id: str, role: str):
        with self._lock:
            was_down = self.is_down(node_id)
            self._last_seen[node_id] = time.monotonic()
            self._roles[node_id] = role
            if was_down:
                log_event("NODE_RECOVERED", node_id=node_id, role=role)

    def is_down(self, node_id: str) -> bool:
        last = self._last_seen.get(node_id)
        if last is None:
            return True
        return (time.monotonic() - last) > HEARTBEAT_TIMEOUT_SECONDS

    def snapshot(self) -> dict:
        with self._lock:
            result = {}
            for node_id, last in self._last_seen.items():
                down = (time.monotonic() - last) > HEARTBEAT_TIMEOUT_SECONDS
                result[node_id] = {
                    "healthy": not down,
                    "role": self._roles.get(node_id, "UNKNOWN"),
                    "seconds_since_last_heartbeat": round(time.monotonic() - last, 2),
                    # 'load' y 'latency_ms' son simulados aquí; en un despliegue real
                    # vendrían de métricas reales (ej. Micrometer/Prometheus por nodo).
                    "load": 0.0,
                    "latency_ms": 50.0,
                }
            return result

    def newly_down_nodes(self) -> list[str]:
        with self._lock:
            return [n for n in self._last_seen if self.is_down(n)]
