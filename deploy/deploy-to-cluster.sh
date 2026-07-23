#!/bin/bash
# ============================================================
# deploy-to-cluster.sh
# Distribuye los paquetes generados por prepare-packages.sh hacia
# las 5 máquinas físicas (o VMs) del cluster mediante SSH/SCP, sobre
# la red estática 192.168.100.0/24 definida en config/cluster.yml.
#
# Requiere: acceso SSH por clave pública ya configurado hacia cada IP.
# ============================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST_DIR="$ROOT_DIR/dist"
REMOTE_USER="${REMOTE_USER:-banking}"
REMOTE_APP_DIR="/opt/banking-system"

declare -A MACHINES=(
  [machine-1]="192.168.100.10"
  [machine-2]="192.168.100.20"
  [machine-3]="192.168.100.21"
  [machine-4]="192.168.100.22"
  [machine-5]="192.168.100.30"
)

for machine in "${!MACHINES[@]}"; do
    ip="${MACHINES[$machine]}"
    echo "==> Desplegando $machine ($ip)"
    ssh "$REMOTE_USER@$ip" "mkdir -p $REMOTE_APP_DIR"
    scp -r "$DIST_DIR/$machine/"* "$REMOTE_USER@$ip:$REMOTE_APP_DIR/"
done

echo "==> Iniciando PostgreSQL primario (Máquina 2)"
ssh "$REMOTE_USER@${MACHINES[machine-2]}" "sudo systemctl restart postgresql"

echo "==> Iniciando réplicas PostgreSQL (Máquinas 3 y 4) vía pg_basebackup"
for m in machine-3 machine-4; do
    ip="${MACHINES[$m]}"
    ssh "$REMOTE_USER@$ip" "
        sudo systemctl stop postgresql &&
        sudo -u postgres pg_basebackup -h ${MACHINES[machine-2]} -D /var/lib/postgresql/16/main \
             -U replicator -Fp -Xs -P -R &&
        sudo systemctl start postgresql
    "
done

echo "==> Iniciando microservicios Java (Máquinas 2, 3, 4)"
for m in machine-2 machine-3 machine-4; do
    ip="${MACHINES[$m]}"
    node_id="node-${m##*-}"
    ssh "$REMOTE_USER@$ip" "
        nohup java -jar $REMOTE_APP_DIR/transaction-service.jar \
            --NODE_ID=$node_id > $REMOTE_APP_DIR/service.log 2>&1 &
    "
done

echo "==> Iniciando Monitor (Máquina 5)"
ssh "$REMOTE_USER@${MACHINES[machine-5]}" "
    cd $REMOTE_APP_DIR/monitor && pip install -r requirements.txt &&
    nohup uvicorn main:app --host 0.0.0.0 --port 8500 > monitor.log 2>&1 &
"

echo "==> Iniciando Gateway + Load Balancer + Circuit Breaker (Máquina 1)"
ssh "$REMOTE_USER@${MACHINES[machine-1]}" "
    nohup java -jar $REMOTE_APP_DIR/api-gateway.jar > gateway.log 2>&1 &
    cd $REMOTE_APP_DIR/load-balancer && pip install -r requirements.txt &&
    nohup uvicorn main:app --host 0.0.0.0 --port 9000 > lb.log 2>&1 &
    cd $REMOTE_APP_DIR/circuit-breaker && pip install -r requirements.txt &&
    nohup uvicorn main:app --host 0.0.0.0 --port 9100 > cb.log 2>&1 &
"

echo "==> Despliegue completo. Verificar con: curl http://192.168.100.10:8080/actuator/health"
