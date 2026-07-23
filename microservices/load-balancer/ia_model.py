"""
Modelo de IA para selección de nodo, tal como lo pide el requerimiento:
"el modelo IA debe solamente elegir a dónde debe ir la petición de acuerdo
a los nodos disponibles según el peso".

Implementación: un scorer softmax explicable (no una caja negra). Cada nodo
se representa con un vector de features normalizadas y se calcula:

    score_i = w1*peso_configurado_i + w2*(1 - carga_i) + w3*(1 - latencia_norm_i) + w4*es_primary_i

Luego se aplica softmax sobre los scores para obtener una distribución de
probabilidad y se elige el nodo con mayor probabilidad (o se muestrea, según modo).

Este diseño es intencionalmente simple y auditable -- cumple el rol de "elegir
según peso y métricas disponibles" sin comportarse como caja negra. Es el punto
de extensión natural para reemplazar por un modelo entrenado (ej. sklearn
LogisticRegression / árbol de decisión) sin tocar el resto del sistema, ya que
la interfaz `select_node(nodes)` se mantendría igual.
"""
from dataclasses import dataclass
import numpy as np


@dataclass
class NodeMetrics:
    node_id: str
    url: str
    weight: float          # peso configurado (capacidad relativa del nodo, ej. PRIMARY=1.0, REPLICA=0.6)
    current_load: float    # 0.0 (libre) - 1.0 (saturado), reportado vía heartbeat/monitor
    latency_ms: float      # latencia observada reciente
    is_primary: bool
    healthy: bool


# Pesos del scorer -- ajustables sin reentrenar nada (modelo lineal transparente)
W_WEIGHT = 0.45
W_LOAD = 0.30
W_LATENCY = 0.15
W_PRIMARY_BONUS = 0.10

MAX_LATENCY_MS = 500.0  # normalización


class AINodeSelector:

    def score(self, node: NodeMetrics) -> float:
        latency_norm = min(node.latency_ms / MAX_LATENCY_MS, 1.0)
        return (
            W_WEIGHT * node.weight
            + W_LOAD * (1.0 - node.current_load)
            + W_LATENCY * (1.0 - latency_norm)
            + W_PRIMARY_BONUS * (1.0 if node.is_primary else 0.0)
        )

    def select_node(self, nodes: list[NodeMetrics]) -> NodeMetrics | None:
        candidates = [n for n in nodes if n.healthy]
        if not candidates:
            return None

        scores = np.array([self.score(n) for n in candidates])
        # softmax para convertir scores en probabilidades (estable numéricamente)
        exp_scores = np.exp(scores - np.max(scores))
        probabilities = exp_scores / exp_scores.sum()

        best_idx = int(np.argmax(probabilities))
        return candidates[best_idx]

    def explain(self, nodes: list[NodeMetrics]) -> list[dict]:
        """Devuelve el detalle del scoring -- útil para debugging/observabilidad."""
        return [
            {
                "node_id": n.node_id,
                "healthy": n.healthy,
                "score": round(self.score(n), 4) if n.healthy else None,
            }
            for n in nodes
        ]
