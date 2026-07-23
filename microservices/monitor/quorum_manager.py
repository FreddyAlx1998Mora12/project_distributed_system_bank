"""
ALGORITMO DE QUORUM Y ELECCIÓN DE LÍDER (simplificado, tipo Bully).

Reglas:
  - El cluster de nodos de transacción tiene N=3 (node-2, node-3, node-4).
  - Se requiere mayoría simple viva (>= N//2 + 1 = 2) para considerar al
    cluster "consistente" y aceptar escrituras.
  - Si el nodo PRIMARY cae, se elige automáticamente como nuevo PRIMARY
    al nodo vivo con mayor prioridad (por convención, menor node_id numérico
    entre los sobrevivientes) -- variante simplificada del algoritmo Bully,
    donde "mayor prioridad" determina quién gana la elección.
"""
import threading
from logger import log_event

CLUSTER_NODES = ["node-2", "node-3", "node-4"]
QUORUM_SIZE = len(CLUSTER_NODES) // 2 + 1  # = 2 de 3


class QuorumManager:
    def __init__(self):
        self._current_primary: str | None = None
        self._lock = threading.Lock()

    def evaluate(self, heartbeat_snapshot: dict) -> dict:
        with self._lock:
            alive = [n for n in CLUSTER_NODES if heartbeat_snapshot.get(n, {}).get("healthy")]
            has_quorum = len(alive) >= QUORUM_SIZE

            if has_quorum:
                if self._current_primary not in alive:
                    # el primary actual cayó (o no había uno aún) -> elegir nuevo líder
                    new_primary = self._elect_leader(alive)
                    if new_primary != self._current_primary:
                        log_event("LEADER_ELECTED", new_primary=new_primary,
                                  previous_primary=self._current_primary, alive_nodes=alive)
                    self._current_primary = new_primary
            else:
                if self._current_primary is not None:
                    log_event("QUORUM_LOST", alive_nodes=alive, required=QUORUM_SIZE)
                self._current_primary = None  # sin quorum -> el cluster no acepta escrituras

            return {
                "has_quorum": has_quorum,
                "quorum_required": QUORUM_SIZE,
                "alive_nodes": alive,
                "current_primary": self._current_primary,
            }

    def _elect_leader(self, alive_nodes: list[str]) -> str:
        # Algoritmo Bully simplificado: gana el nodo de mayor prioridad (menor sufijo numérico)
        return sorted(alive_nodes, key=lambda n: int(n.split("-")[1]))[0]
