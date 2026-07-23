#!/bin/bash
# ============================================================
# prepare-packages.sh
# Compila cada servicio y genera los artefactos (.jar / imagen Docker)
# listos para copiar a cada una de las 5 máquinas físicas del cluster.
# ============================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST_DIR="$ROOT_DIR/dist"

echo "==> Limpiando dist/"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"/{machine-1,machine-2,machine-3,machine-4,machine-5}

echo "==> Compilando api-gateway (Máquina 1)"
(cd "$ROOT_DIR/services/api-gateway" && mvn -q -B clean package -DskipTests)
cp "$ROOT_DIR/services/api-gateway/target/api-gateway.jar" "$DIST_DIR/machine-1/"

echo "==> Empaquetando load-balancer + circuit-breaker (Máquina 1, Python)"
cp -r "$ROOT_DIR/services/load-balancer" "$DIST_DIR/machine-1/"
cp -r "$ROOT_DIR/services/circuit-breaker" "$DIST_DIR/machine-1/"

echo "==> Compilando transaction-service (Máquinas 2, 3, 4)"
(cd "$ROOT_DIR/services/transaction-service" && mvn -q -B clean package -DskipTests)
for m in machine-2 machine-3 machine-4; do
    cp "$ROOT_DIR/services/transaction-service/target/transaction-service.jar" "$DIST_DIR/$m/"
done

echo "==> Empaquetando monitor (Máquina 5, Python)"
cp -r "$ROOT_DIR/services/monitor" "$DIST_DIR/machine-5/"

echo "==> Copiando configuraciones de PostgreSQL"
cp "$ROOT_DIR/config/postgresql/primary.conf" "$DIST_DIR/machine-2/"
cp "$ROOT_DIR/config/postgresql/replica.conf" "$DIST_DIR/machine-3/"
cp "$ROOT_DIR/config/postgresql/replica.conf" "$DIST_DIR/machine-4/"

echo "==> Paquetes listos en $DIST_DIR"
