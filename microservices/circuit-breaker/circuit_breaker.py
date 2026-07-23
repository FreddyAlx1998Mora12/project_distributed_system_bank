"""
PATRÓN CIRCUIT BREAKER -- una instancia independiente por nodo, para que
la caída de un nodo no afecte el enrutamiento hacia los demás.

Estados:
  CLOSED    -> tráfico normal. Cuenta fallos consecutivos.
  OPEN      -> se alcanzó failure_threshold; se rechazan peticiones de inmediato
               (fail-fast) durante recovery_timeout, sin ni siquiera intentar la
               llamada al nodo (evita saturar un nodo caído / fallos en cascada).
  HALF_OPEN -> pasado recovery_timeout, se permite un número limitado de
               peticiones de prueba. Si tienen éxito -> CLOSED. Si fallan -> OPEN.
"""
import time
import threading
from enum import Enum


class State(str, Enum):
    CLOSED = "CLOSED"
    OPEN = "OPEN"
    HALF_OPEN = "HALF_OPEN"


class CircuitBreaker:
    def __init__(self, failure_threshold: int = 3, recovery_timeout: float = 15.0,
                 half_open_max_calls: int = 2):
        self.failure_threshold = failure_threshold
        self.recovery_timeout = recovery_timeout
        self.half_open_max_calls = half_open_max_calls

        self._state = State.CLOSED
        self._failure_count = 0
        self._opened_at: float | None = None
        self._half_open_calls = 0
        self._lock = threading.Lock()

    @property
    def state(self) -> State:
        with self._lock:
            self._maybe_transition_to_half_open()
            return self._state

    def _maybe_transition_to_half_open(self):
        if self._state == State.OPEN and self._opened_at is not None:
            if time.monotonic() - self._opened_at >= self.recovery_timeout:
                self._state = State.HALF_OPEN
                self._half_open_calls = 0

    def allow_request(self) -> bool:
        with self._lock:
            self._maybe_transition_to_half_open()
            if self._state == State.CLOSED:
                return True
            if self._state == State.HALF_OPEN:
                if self._half_open_calls < self.half_open_max_calls:
                    self._half_open_calls += 1
                    return True
                return False
            return False  # OPEN

    def record_success(self):
        with self._lock:
            self._failure_count = 0
            self._state = State.CLOSED
            self._opened_at = None

    def record_failure(self):
        with self._lock:
            self._failure_count += 1
            if self._state == State.HALF_OPEN:
                self._trip()
            elif self._failure_count >= self.failure_threshold:
                self._trip()

    def _trip(self):
        self._state = State.OPEN
        self._opened_at = time.monotonic()
        self._failure_count = 0
        self._half_open_calls = 0

    def snapshot(self) -> dict:
        with self._lock:
            return {
                "state": self._state.value,
                "failure_count": self._failure_count,
            }


class CircuitBreakerRegistry:
    """Mantiene un breaker independiente por node_id (aislamiento de fallos)."""
    def __init__(self):
        self._breakers: dict[str, CircuitBreaker] = {}
        self._lock = threading.Lock()

    def get(self, node_id: str) -> CircuitBreaker:
        with self._lock:
            if node_id not in self._breakers:
                self._breakers[node_id] = CircuitBreaker()
            return self._breakers[node_id]

    def snapshot_all(self) -> dict:
        with self._lock:
            return {node_id: cb.snapshot() for node_id, cb in self._breakers.items()}
