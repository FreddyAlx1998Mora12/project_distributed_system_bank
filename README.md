# 🏦 Sistema Distribuido de Transacciones Bancarias Tolerante a Fallos

> **Sistema de Microservicios en Java 17 / Spring Boot** que garantiza **consistencia estricta (ACID)** y **alta disponibilidad** mediante **Write-Ahead Logging (WAL)**, **Crash Recovery**, algoritmo de **Quorum (Prevención de Split-Brain)**, **Balanceo de Carga con Circuit Breaker por Nodo (Resilience4j)** y **Replicación Física PostgreSQL (Primary / Réplicas)**.

---

## 📌 Tabla de Contenidos
- [1. Arquitectura del Sistema](#1-arquitectura-del-sistema)
- [2. Mapeo de Nodos, IPs y Puertos](#2-mapeo-de-nodos-ips-y-puertos)
- [3. Requisitos Previos](#3-requisitos-previos)
- [4. 🚀 Guía de Ejecución Rápida con Docker (Recomendada)](#4--guía-de-ejecución-rápida-con-docker-recomendada)
- [5. 🧪 Ejemplos de Peticiones HTTP (Copy-Paste Ready)](#5--ejemplos-de-peticiones-http-copy-paste-ready)
- [6. 💥 Cómo Probar Tolerancia a Fallos y Recuperación (Crash Recovery)](#6--cómo-probar-tolerancia-a-fallos-y-recuperación-crash-recovery)
- [7. 🌐 Despliegue en 5 Máquinas Físicas (Bare-Metal / SSH)](#7--despliegue-en-5-máquinas-físicas-bare-metal--ssh)
- [8. 🛠️ Explicación de los Componentes Clave](#8-️-explicación-de-los-componentes-clave)
- [9. ❓ Solución de Problemas Comunes](#9--solución-de-problemas-comunes)

---

## 1. Arquitectura del Sistema

```
                                  [ Cliente HTTP / Postman ]
                                              │
                                              ▼
┌────────────────── MÁQUINA 1 (192.168.100.10) ──────────────────┐
│ API Gateway (Puerto 8080)                                      │
│  └─► Load Balancer (Puerto 9000)                               │
│        └─► Circuit Breaker por Nodo (Resilience4j)             │
└─────────┬──────────────────────────────────────────────────────┘
          │
  ┌───────┼──────────────────────┐
  ▼       ▼                      ▼
MÁQUINA 2 (192.168.100.20)  MÁQUINA 3 (192.168.100.22)  MÁQUINA 4 (192.168.100.24)
node-2 (PRIMARY)            node-3 (REPLICA 1)        node-4 (REPLICA 2)
Transaction Service         Transaction Service       Transaction Service
+ WAL append-only           + WAL append-only         + WAL append-only
+ PostgreSQL Primary        + PostgreSQL Replica 1    + PostgreSQL Replica 2
  │                           │                         │
  └───────────────── Heartbeat continuo (2s) ───────────┘
                              │
                              ▼
           MÁQUINA 5 (192.168.100.30 - Quorum & Monitor)
           Cluster Monitor (Puerto 8500): Consenso y Salud
```

---

## 2. Mapeo de Nodos, IPs y Puertos

| Servicio | Máquina / IP (Docker) | Puerto | Descripción |
|---|---|---|---|
| **api-gateway** | Máquina 1 (192.168.100.10) | `8080` | Punto de entrada único del sistema |
| **load-balancer** | Máquina 1 (192.168.100.11) | `9000` | Selección de nodo y Circuit Breaker por nodo |
| **transaction-service-2** | Máquina 2 (192.168.100.21) | Interno | Nodo Primario de Transacciones (`node-2`) |
| **postgres-primary** | Máquina 2 (192.168.100.20) | `5432` | Base de datos PostgreSQL Primaria (`wal_level=replica`) |
| **transaction-service-3** | Máquina 3 (192.168.100.23) | Interno | Nodo Réplica 1 (`node-3`) |
| **postgres-replica-1** | Máquina 3 (192.168.100.22) | Interno | Base de datos PostgreSQL Réplica 1 |
| **transaction-service-4** | Máquina 4 (192.168.100.25) | Interno | Nodo Réplica 2 (`node-4`) |
| **postgres-replica-2** | Máquina 4 (192.168.100.24) | Interno | Base de datos PostgreSQL Réplica 2 |
| **quorum-monitor** | Máquina 5 (192.168.100.30) | `8500` | Monitor de Salud y Algoritmo de Quorum |

---

## 3. Requisitos Previos

Antes de empezar, asegúrate de tener instalado en tu computadora:

1. **Docker Desktop** o **Docker Engine** + **Docker Compose** (`docker compose version` debe ser 2.0+).
2. **cURL** o **Postman** para probar las peticiones.
3. *(Opcional para desarrollo)* **Java 17 JDK** y **Maven 3.9+**.

---

## 4. 🚀 Guía de Ejecución Rápida con Docker (Recomendada)

Sigue estos 3 pasos simples para poner a correr **TODO** el clúster con un solo comando:

### Paso 1: Clonar/Entrar al proyecto
Abre una terminal y ubícate en la carpeta raíz del proyecto:
```bash
cd banking-distributed-system
```

### Paso 2: Compilar e Iniciar Contenedores
Ejecuta el siguiente comando (compilará e iniciará los 8 contenedores):
```bash
docker compose up --build -d
```
> ⏱️ *Nota: La primera vez puede tardar unos 2-3 minutos mientras descarga las imágenes de Java y Maven.*

### Paso 3: Verificar que los contenedores están corriendo
Ejecuta:
```bash
docker compose ps
```
Deberías ver los 8 servicios en estado `running` (Up):
- `api-gateway`
- `load-balancer`
- `monitor`
- `postgres-primary`
- `postgres-replica-1`
- `postgres-replica-2`
- `transaction-service-2`
- `transaction-service-3`
- `transaction-service-4`

---

## 5. 🧪 Ejemplos de Peticiones HTTP (Copy-Paste Ready)

Puedes probar el sistema usando `curl` desde tu terminal o importando las rutas a Postman:

### 1. Consultar la Topología del Clúster y Quorum
Verifica qué nodo es el líder actual y si hay quorum suficiente:
```bash
curl -s http://localhost:8500/cluster/topology | jq .
```
**Respuesta esperada:**
```json
{
  "leaderId": "node-2",
  "hasQuorum": true,
  "quorumRequired": 2,
  "totalNodes": 3,
  "nodes": [...]
}
```

---

### 2. Realizar un Depósito (Deposit)
Envía un depósito de $500.00 a la cuenta `ACC-1001`:
```bash
curl -X POST http://localhost:8080/api/transactions/deposit \
  -H "Content-Type: application/json" \
  -d '{
    "toAccountId": "ACC-1001",
    "amount": 500.00
  }'
```
**Respuesta esperada:**
```json
{
  "transactionId": "tx-8f4b-...",
  "status": "COMMITTED",
  "message": "Depósito procesado exitosamente",
  "sequence": 1
}
```

---

### 3. Realizar un Retiro (Withdraw)
Retira $150.00 de la cuenta `ACC-1001`:
```bash
curl -X POST http://localhost:8080/api/transactions/withdraw \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "ACC-1001",
    "amount": 150.00
  }'
```

---

### 4. Realizar una Transferencia (Transfer)
Transfiere $100.00 de la cuenta `ACC-1001` a la cuenta `ACC-2002`:
```bash
curl -X POST http://localhost:8080/api/transactions/transfer \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "ACC-1001",
    "toAccountId": "ACC-2002",
    "amount": 100.00
  }'
```

---

## 6. 💥 Cómo Probar Tolerancia a Fallos y Recuperación (Crash Recovery)

El sistema soporta caídas repentinas de nodos sin perder información financiera.

### Prueba 1: Simular Caída del Nodo Primario (`node-2`)
Simula un apagado repentino del nodo primario mientras está en producción:
```bash
docker compose stop transaction-service-2
```

1. Consulta el estado del Quorum:
   ```bash
   curl -s http://localhost:8500/cluster/topology | jq .
   ```
2. Observa cómo el Quorum detecta que `node-2` cayó y **reelige un nuevo nodo líder** (ej: `node-3`) manteniendo el sistema operativo.

### Prueba 2: Probar el Crash Recovery Engine al Reiniciar
Cuando `transaction-service-2` vuelve a arrancar:
```bash
docker compose start transaction-service-2
```
1. El evento `ApplicationReadyEvent` activa automáticamente el [CrashRecoveryEngine](file:///home/freddy/Documentos/ProyectosDesarrollo/STS4-4.25.0/banking-distributed-system/microservices/transaction-service/src/main/java/unl/project/distributed/transaction_service/infrastructure/wal/CrashRecoveryEngine.java).
2. Lee el archivo Write-Ahead Log (`.wal`), identifica transacciones pendientes en estado `WRITTEN` y las reaplica en la base de datos de manera **idempotente** usando `Account.lastAppliedSequence`.
3. Revisa los logs del contenedor para verificar la reconstrucción:
   ```bash
   docker compose logs transaction-service-2 | grep "Crash recovery"
   ```

---

## 7. 🌐 Despliegue en 5 Máquinas Físicas (Bare-Metal / SSH)

Si vas a desplegar el clúster en 5 servidores/máquinas independientes (IPs de la subred `192.168.100.0/24`):

1. **Compilar y empaquetar los artefactos `.jar`**:
   ```bash
   ./deploy/prepare-packages.sh
   ```
   *Generará los ejecutables en el directorio `dist/` organizados por máquina.*

2. **Distribuir y ejecutar por SSH/SCP**:
   ```bash
   ./deploy/deploy-to-cluster.sh
   ```

---

## 8. 🛠️ Explicación de los Componentes Clave

- **Write-Ahead Logging (WAL)**: Cada transacción realiza un *append* en disco en estado `WRITTEN` **antes** de tocar PostgreSQL. Al confirmarse en la BD, la entrada se marca como `COMMITTED`. Si el servidor pierde energía en medio de la operación, el estado se recupera sin pérdidas al reiniciar.
- **Quorum ($N/2 + 1$)**: Para evitar el problema de *Split-Brain* (donde dos partes de la red creen ser el líder), las escrituras requieren el consenso de la mayoría simple de nodos sanos.
- **Circuit Breaker Aislado por Nodo**: Configurado con Resilience4j en el `load-balancer-service`. Si un nodo de transacción se vuelve lento o falla, se aísla automáticamente ese nodo sin afectar el tráfico hacia los demás nodos sanos.
- **Arquitectura Hexagonal**: La lógica de negocio (`domain`) en `transaction-service` no tiene acoplamiento con Spring, JPA o PostgreSQL, permitiendo pruebas unitarias con JUnit sin levantar contenedores.

---

## 9. ❓ Solución de Problemas Comunes

### Error: "Port is already allocated" (Puerto ocupado)
- Si los puertos `8080`, `9000` o `8500` están en uso por otra aplicación en tu computadora, detén esos servicios o cámbialos en `docker-compose.yml`.

### Reiniciar el sistema desde cero (Limpieza total)
Si deseas borrar todas las bases de datos y archivos WAL de prueba para empezar de cero:
```bash
docker compose down -v
docker compose up --build -d
```

### Ver logs en tiempo real de un servicio específico
```bash
docker compose logs -f load-balancer
docker compose logs -f transaction-service-2
```

---
