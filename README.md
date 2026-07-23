# Sistema de Recuperación de Transacciones Bancarias Tolerante a Fallos (WAL)

Sistema distribuido de microservicios que garantiza durabilidad y consistencia
de transacciones bancarias mediante **Write-Ahead Logging**, con balanceo de
carga asistido por un modelo de scoring ("IA"), Circuit Breaker por nodo,
replicación PostgreSQL, quorum y monitoreo centralizado.

## 1. Arquitectura

```
Cliente externo
      │
      ▼
┌─────────────── MÁQUINA 1 (192.168.100.10) ───────────────┐
│ API Gateway (Java/Spring Boot, hexagonal)                │
│        │                                                  │
│        ▼                                                  │
│ Load Balancer con IA (Python) ── elige nodo por peso/carga│
│        │                                                  │
│        ▼                                                  │
│ Circuit Breaker (Python) ── CLOSED/OPEN/HALF_OPEN por nodo│
└────────┬───────────────────────────────────────────────────┘
         │
  ┌──────┼───────────────┐
  ▼               ▼               ▼
MÁQUINA 2      MÁQUINA 3      MÁQUINA 4
node-2         node-3         node-4
(PRIMARY)      (REPLICA)      (REPLICA)
Spring Boot    Spring Boot    Spring Boot
+ WAL propio   + WAL propio   + WAL propio
+ Postgres     + Postgres     + Postgres
  PRIMARY        REPLICA 1      REPLICA 2
  (streaming replication física, wal_level=replica)
  │               │               │
  └───────heartbeat cada 2s───────┘
                  ▼
         MÁQUINA 5 (192.168.100.30)
         Monitor: Heartbeat + Quorum + Logs centralizados
```

## 2. Por qué cada pieza está donde está

| Componente | Por qué |
|---|---|
| **WAL antes que DB** | Garantiza durabilidad: si el proceso muere entre escribir el log y confirmar en Postgres, el estado se reconstruye al reiniciar. Es el mismo principio que usa Postgres/InnoDB internamente, aplicado aquí a nivel de aplicación para las cuentas bancarias. |
| **Command pattern** | Cada operación (Deposit/Withdraw/Transfer) es serializable de forma uniforme al WAL y reconstruible por `CommandFactory` durante el recovery — sin este patrón, el recovery necesitaría lógica ad-hoc por tipo de operación. |
| **Hexagonal en transaction-service** | El dominio (`domain/`) no conoce JPA ni PostgreSQL. Los puertos (`AccountRepository`, `TransactionRepository`) se implementan en `infrastructure/persistence`. Esto permite, por ejemplo, cambiar de Postgres a otro motor sin tocar la lógica de negocio ni el WAL. |
| **Balanceador con "IA" en Python separado del Gateway** | Aísla la lógica de selección de nodo (que cambia con frecuencia — pesos, métricas) del enrutamiento HTTP puro del Gateway (que casi no cambia). Al ser Python, es trivial evolucionar el scorer a un modelo entrenado real. |
| **Circuit Breaker independiente por nodo** | Si `node-3` está fallando, no debe afectar el enrutamiento hacia `node-2` o `node-4`. Un breaker global tumbaría todo el cluster ante el fallo de un solo nodo. |
| **Quorum en máquina separada (Monitor)** | Evita que la decisión de "quién es el líder" dependa de un nodo que podría ser justamente el que está fallando (single point of failure evitado). |
| **PostgreSQL con streaming replication real** | No se simula la replicación: se usa `wal_level=replica` + `pg_basebackup` + `primary_conninfo`, el mecanismo real de Postgres, para que el failover de base de datos sea genuino. |

## 3. El algoritmo de Crash Recovery (pieza central)

Implementado en `CrashRecoveryEngine.recover()`:

1. Al arrancar (`ApplicationReadyEvent`), se lee **todo el WAL secuencialmente**, ordenado por `sequence`.
2. Se identifica qué `transactionId` llegaron a estado `COMMITTED` (ya reflejados en la BD antes del crash).
3. Para las entradas `WRITTEN` sin su `COMMITTED` correspondiente → se reconstruye el `Command` original vía `CommandFactory` y se reaplica (`redo`/roll-forward).
4. La reaplicación es **idempotente**: `Account.lastAppliedSequence` evita duplicar un cambio si ya estaba parcialmente reflejado en la BD.
5. Solo entonces el nodo empieza a aceptar tráfico de negocio.

Esto reconstruye el último estado consistente de las cuentas sin necesitar snapshots externos.

## 4. Cómo correrlo localmente (simulación con Docker)

```bash
docker compose build
docker compose up -d
curl -X POST http://localhost:8080/api/transactions/deposit \
  -H "Content-Type: application/json" \
  -d '{"toAccountId":"acc-1","amount":100.00}'

# ver estado del quorum
curl http://localhost:8500/quorum

# probar tolerancia a fallos
python tests/failover_test.py
```

## 5. Despliegue en las 5 máquinas físicas reales

```bash
./deploy/prepare-packages.sh      # compila y empaqueta cada servicio
./deploy/deploy-to-cluster.sh     # distribuye por SSH/SCP a 192.168.100.10-30
```

`config/cluster.yml` documenta el mapeo IP ↔ máquina ↔ servicios, usado como referencia tanto por `docker-compose.yml` (simulación) como por `deploy-to-cluster.sh` (bare-metal).

## 6. Dockers ligeros — decisiones

- **Java**: build multi-stage (`maven:3.9-eclipse-temurin-17-alpine` para compilar → `eclipse-temurin:17-jre-alpine` para runtime). El JDK y Maven no viajan a producción, solo el JRE + el jar.
- **Python**: `python:3.12-slim`, sin capas de build innecesarias, dependencias mínimas (`fastapi`, `uvicorn`, `httpx`, `numpy` solo en load-balancer).
- **PostgreSQL**: `postgres:16-alpine`, misma imagen para primary/réplica, rol decidido en runtime vía `POSTGRES_ROLE`.
- Todos los contenedores corren con **usuario no-root**.

## 7. Extensión: modelo de IA real

`services/load-balancer/ia_model.py` implementa un scorer softmax explicable (`select_node`). Es el punto de extensión natural: reemplazar por un `sklearn.LogisticRegression` entrenado con históricos de latencia/carga, manteniendo la misma interfaz `select_node(nodes) -> NodeMetrics`, sin tocar `main.py` ni el resto del sistema.

## 8. Limitaciones conocidas (honestidad técnica)

- El parser de payload del WAL (`CommandFactory`) usa regex simple en vez de una librería JSON completa, por rendimiento en la ruta crítica de recovery — no soporta payloads anidados complejos.
- `ddl-auto: update` en Hibernate es aceptable para esta simulación; en producción se recomienda Flyway/Liquibase con migraciones versionadas.
- El `pg_basebackup` para levantar réplicas se deja como paso manual en `deploy-to-cluster.sh` (bare-metal); en `docker-compose.yml` las réplicas arrancan como Postgres independientes para simplificar la simulación local — para replicación streaming real en Docker, habría que automatizar `pg_basebackup` en el entrypoint de los contenedores réplica.
