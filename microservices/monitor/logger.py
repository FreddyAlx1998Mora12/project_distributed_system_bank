"""
Logs centralizados (Máquina 5). Registra en formato JSON estructurado todos
los eventos de heartbeat, cambios de quorum y elecciones de líder, para que
puedan correlacionarse junto a las transacciones procesadas en los nodos.
"""
import json
import logging
import os
from datetime import datetime, timezone

LOG_PATH = os.getenv("CENTRAL_LOG_PATH", "/data/logs/cluster.log")
os.makedirs(os.path.dirname(LOG_PATH), exist_ok=True)

logger = logging.getLogger("cluster_monitor")
logger.setLevel(logging.INFO)

_file_handler = logging.FileHandler(LOG_PATH)
_console_handler = logging.StreamHandler()
_formatter = logging.Formatter("%(message)s")
_file_handler.setFormatter(_formatter)
_console_handler.setFormatter(_formatter)
logger.addHandler(_file_handler)
logger.addHandler(_console_handler)


def log_event(event_type: str, **fields):
    entry = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "event": event_type,
        **fields,
    }
    logger.info(json.dumps(entry, ensure_ascii=False))
