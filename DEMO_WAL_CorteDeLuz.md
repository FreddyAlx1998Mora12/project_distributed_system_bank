# Demostración WAL + Corte de Luz

## Guion paso a paso para la defensa de la práctica

---

### Paso 1: Abrir el WAL en vivo

En una terminal, ejecuta:
```bash
docker exec -it project_distributed_system_bank-transaction-service-2-1 tail -f /data/wal/transactions.wal
```

> **Explica**: Este es el archivo de log físico donde se registran TODAS las transacciones ANTES de tocar la base de datos. Es la esencia del Write-Ahead Logging.

---

### Paso 2: Consultar saldo actual de ACC-1001

```bash
docker exec project_distributed_system_bank-postgres-primary-1 psql -U banking_user -d banking -c "SELECT account_id, balance FROM accounts WHERE account_id='ACC-1001';"
```

> Saldo actual: **$3,785** (esto es lo que debemos recuperar tras el "corte de luz")

---

### Paso 3: Hacer un depósito desde la UI

- En el navegador (`http://localhost:3000`), selecciona **Depósito**
- Cuenta destino: `ACC-1001`, Monto: `100`
- Envía la transacción

---

### Paso 4: Mostrar el WAL en vivo

En la terminal del paso 1 verás algo así:

```
13|2026-07-27T05:10:30.123456789Z|uuid-aqui|DepositCommand|WRITTEN|{"cmd":"DEPOSIT","txId":"uuid","to":"ACC-1001","amount":100}
13|2026-07-27T05:10:30.456789012Z|uuid-aqui|DepositCommand|COMMITTED|{"cmd":"DEPOSIT","txId":"uuid","to":"ACC-1001","amount":100}
```

> **Explica**: Cada transacción tiene 2 entradas:
> - `WRITTEN`: Se grabó en el log
> - `COMMITTED`: Se confirmó la operación
> - El saldo se actualiza **después** de `COMMITTED`

---

### Paso 5: Verificar el saldo nuevo

```bash
docker exec project_distributed_system_bank-postgres-primary-1 psql -U banking_user -d banking -c "SELECT account_id, balance FROM accounts WHERE account_id='ACC-1001';"
```

> Saldo ahora: **$3,885** (+$100)

---

### Paso 6: Simular el "corte de luz" (matar el backend)

**Terminal 2** - Detener el nodo primario del transaction service:

```bash
docker compose stop transaction-service-2
```

**Terminal 3** - Hacer un depósito que NO se completará (simula transacción a medias):

```bash
curl -X POST http://localhost:8080/api/transactions/process \
  -H "Content-Type: application/json" \
  -d '{"fromAccountId":"ACC-1001","toAccountId":"ACC-2002","amount":500,"type":"TRANSFER"}'
```

> Este request fallará porque el nodo está caído. El sistema aún tiene quorum con los otros 2 nodos, pero la transacción se escribe en el WAL de los nodos disponibles.

---

### Paso 7: Mostrar el estado del sistema

```bash
curl -s http://localhost:8500/cluster/topology | python3 -m json.tool
```

> Muestra que un nodo está DOWN pero el sistema sigue operando con quorum

---

### Paso 8: Simular el "regreso de la luz" (reiniciar)

```bash
docker compose start transaction-service-2
```

---

### Paso 9: Mostrar la recuperación

Espera 5 segundos y luego revisa los logs:

```bash
docker logs project_distributed_system_bank-transaction-service-2-1 2>&1 | grep -i "recovery\|Crash\|WAL\|replaying"
```

> Deberías ver algo como:
> ```
> Iniciando recuperación por crash...
> WAL recuperado: 15 entradas procesadas
> Recovering command: DepositCommand...
> Recovering command: TransferCommand...
> Recuperación completada: 3 comandos ejecutados
> ```

---

### Paso 10: Verificar saldo final

```bash
docker exec project_distributed_system_bank-postgres-primary-1 psql -U banking_user -d banking -c "SELECT account_id, balance FROM accounts WHERE account_id='ACC-1001';"
```

> El saldo es **consistente** — el sistema recuperó todas las transacciones del WAL

---

### Paso 11: Verificar el WAL después del recovery

```bash
docker exec project_distributed_system_bank-transaction-service-2-1 cat /data/wal/transactions.wal
```

> El WAL ahora tiene entradas `COMMITTED` que estaban solo como `WRITTEN` antes del crash

---

## Explicación para el video

### Conceptos clave a mencionar:

1. **WAL (Write-Ahead Logging)**: Cada transacción se escribe en el archivo de texto plano **antes** de modificar la base de datos. Si el sistema falla, las transacciones se recuperan leyendo el log.

2. **Consistencia**: El WAL garantiza que una transacción que fue `WRITTEN` pero no `COMMITTED` antes del crash se re-ejecuta durante la recuperación.

3. **Quorum**: Con 3 nodos, necesitas al menos 2 (`N/2 + 1`) para escribir. Si un nodo falla, los demás continúan operando.

4. **Recuperación**: Al reiniciar, el nodo lee su archivo WAL y re-ejecuta las transacciones pendientes para reconstruir el estado consistente.

---

## Comandos útiles para verificar

| Comando | Propósito |
|---------|-----------|
| `docker exec -it transaction-service-2-1 tail -f /data/wal/transactions.wal` | Ver WAL en tiempo real |
| `docker compose stop transaction-service-2` | Simular apagón |
| `docker compose start transaction-service-2` | Simular encendido |
| `docker logs transaction-service-2-1 2>&1 \| grep -i recovery` | Ver logs de recuperación |
| `docker exec postgres-primary-1 psql -U banking_user -d banking -c "SELECT ..."` | Verificar saldos |
| `curl -s http://localhost:8500/cluster/topology \| jq .` | Ver estado del cluster |

---

## Estructura del WAL (formato)

```
secuencia|timestamp|txId|commandType|status|jsonPayload
```

**Ejemplo**:
```
1|2026-07-27T01:02:30.308234205Z|a1c98704-...|DepositCommand|WRITTEN|{"cmd":"DEPOSIT","txId":"...","to":"ACC-1001","amount":500}
1|2026-07-27T01:02:30.358234205Z|a1c98704-...|DepositCommand|COMMITTED|{"cmd":"DEPOSIT","txId":"...","to":"ACC-1001","amount":500}
```

> **Nota**: Las entradas con `WRITTEN` pero sin `COMMITTED` corresponden a transacciones que se estaban procesando cuando ocurrió el "corte de luz". El sistema las re-ejecuta al recuperarse.
