# AGENTS.md - Distributed Banking System

## Project Overview

Fault-tolerant distributed banking system: 4 Java/Spring Boot microservices + 1 React frontend, orchestrated via Docker Compose on a simulated `192.168.100.0/24` network.

Key concepts: Write-Ahead Logging (WAL), Quorum consensus, per-node Circuit Breakers (Resilience4j), PostgreSQL physical replication.

Language: Codebase, comments, logs, and UI are in **Spanish**.

## Build & Run

### Docker (recommended — everything works out of the box)
```bash
# Full stack (9 containers)
docker compose up --build -d

# Verify
docker compose ps
curl -s http://localhost:8500/cluster/topology | jq .
```

### Frontend (Vite dev server)
```bash
cd frontend
npm install
npm run dev        # http://localhost:3000
npm run lint       # oxlint (NOT ESLint)
```

### Single microservice (Maven)
```bash
cd microservices/<service-name>
mvn clean package -DskipTests
```

### Full reset (wipe DB + WAL)
```bash
docker compose down -v && docker compose up --build -d
```

### Seed accounts (DB is empty after fresh start)
```bash
docker exec project_distributed_system_bank-postgres-primary-1 psql -U banking_user -d banking -c \
  "INSERT INTO accounts (account_id, account_number, balance, last_applied_sequence, version) \
   VALUES ('ACC-1001','SAV-001',0,0,0), ('ACC-2002','SAV-002',0,0,0), ('ACC-3003','SAV-003',0,0,0);"
```

## Architecture (non-obvious parts)

- **NOT Spring Cloud Gateway**: `apigateway` is a hand-rolled REST forwarder using `RestClient`, not Spring Cloud Gateway
- **Hexagonal (ports & adapters)**: `load-balancer-service`, `quorum-monitor-service`, `transaction-service` — domain layer has zero Spring/JPA annotations
- **Command Pattern**: `transaction-service` uses `Command` interface for WAL serialization and crash recovery replay
- **Port overlap**: Both `apigateway` and `transaction-service` default to `:8080`. Works in Docker (separate containers). For bare-metal, pass `--server.port=<N>` explicitly
- **WAL is append-only with tombstones**: `markCommitted()` appends a new record, not in-place update
- **Node health starts as DOWN**: `NodeHealthRegistry` never assumes liveness without evidence

## Key Service Ports (Docker network)

| Service | Host Port | Docker IP |
|---|---|---|
| api-gateway | 8080 | 192.168.100.10 |
| load-balancer | 9000 | 192.168.100.11 |
| monitor | 8500 | 192.168.100.30 |
| transaction-service-{2,3,4} | internal | .21, .23, .25 |
| postgres-primary | internal | 192.168.100.20 |

## Conventions

- **Spring Boot 4.1.0** with Jakarta namespace (`jakarta.persistence.*`)
- **Java 17** target (local JDK may be newer, e.g. 21 — compatible)
- `spring.jpa.hibernate.ddl-auto=update` — no migration tool (no Flyway/Liquibase)
- Lombok used in all Java services
- **Tailwind CSS v4** in frontend (new `@tailwindcss/vite` plugin, no `tailwind.config.js`)
- **oxlint** for frontend linting (NOT ESLint)
- **React 19** + **Vite 8** — plain JSX, no TypeScript

## Gotchas

- **Frontend field mismatch**: `bankingApi.processTransaction()` must send `fromAccountId`/`toAccountId` (not `sourceAccount`/`targetAccount`) to match `TransactionRequest` DTO
- **Transaction status values**: Backend returns `COMMITTED`/`FAILED` (not `SUCCESS`)
- **Cluster topology fields**: Monitor returns `leaderId`, `hasQuorum`, `nodes[]` with `nodeId`/`health`/`priority` (not `leader`/`activeNodes`/`id`/`active`)
- **WebFlux + RestClient = crash**: `apigateway` and `load-balancer-service` must use `spring-boot-starter-web` (servlet), NOT `webflux`. `RestClient` is blocking and throws on reactor threads
- **`RestClient.Builder` not auto-configured**: In Spring Boot 4.x with webflux, inject `RestClient.Builder` fails. Must use `spring-boot-starter-web` or define the builder bean manually
- **Missing PostgreSQL driver**: `transaction-service` pom.xml needs explicit `org.postgresql:postgresql` dependency (not transitively included by `spring-boot-starter-data-jpa`)
- **`LogWriter` is NOT a Spring bean**: Despite old `@Component` annotation, it's manually instantiated by `WriteAheadLog` with `new LogWriter(walFile)`. Removing `@Component` is required
- **`NoAvaliableNodeException`** has a typo (should be "Available")
- **No CI/CD pipeline** (no `.github/workflows/`)

## Deployment

- **Docker Compose** for local development (recommended)
- **Bare-metal**: `./deploy/prepare-packages.sh` builds JARs → `./deploy/deploy-to-cluster.sh` distributes via SSH/SCP to 5 machines on `192.168.100.0/24`

## Fault Tolerance Testing

```bash
# Stop primary node
docker stop transaction-service-2
# Verify quorum changes
curl -s http://localhost:8500/cluster/topology | jq '.leaderId, .hasQuorum'
# Restart → triggers crash recovery
docker start transaction-service-2
docker compose logs transaction-service-2 | grep "Crash recovery"
```
