#!/bin/bash
# ============================================================
# prepare-packages.sh
# Compila cada microservicio Java (Spring Boot) y empaqueta los
# artefactos .jar listos para copiar a las 5 máquinas físicas del cluster.
# ============================================================
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIST_DIR="$ROOT_DIR/dist"

echo "==> Limpiando dist/"
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"/{machine-1,machine-2,machine-3,machine-4,machine-5}

echo "==> Compilando api-gateway (Máquina 1)"
(cd "$ROOT_DIR/microservices/apigateway" && mvn -q -B clean package -DskipTests)
cp "$ROOT_DIR/microservices/apigateway/target/apigateway-0.0.1-SNAPSHOT.jar" "$DIST_DIR/machine-1/api-gateway.jar"

echo "==> Compilando load-balancer-service (Máquina 1)"
(cd "$ROOT_DIR/microservices/load-balancer-service" && mvn -q -B clean package -DskipTests)
cp "$ROOT_DIR/microservices/load-balancer-service/target/load-balancer-service-0.0.1-SNAPSHOT.jar" "$DIST_DIR/machine-1/load-balancer-service.jar"

echo "==> Compilando transaction-service (Máquinas 2, 3, 4)"
(cd "$ROOT_DIR/microservices/transaction-service" && mvn -q -B clean package -DskipTests)
for m in machine-2 machine-3 machine-4; do
    cp "$ROOT_DIR/microservices/transaction-service/target/transaction-service-0.0.1-SNAPSHOT.jar" "$DIST_DIR/$m/transaction-service.jar"
done

echo "==> Compilando quorum-monitor-service (Máquina 5)"
(cd "$ROOT_DIR/microservices/quorum-monitor-service" && mvn -q -B clean package -DskipTests)
cp "$ROOT_DIR/microservices/quorum-monitor-service/target/quorum-monitor-service-0.0.1-SNAPSHOT.jar" "$DIST_DIR/machine-5/quorum-monitor-service.jar"

echo "==> Copiando configuraciones de PostgreSQL"
cp "$ROOT_DIR/config/postgresql/primary.conf" "$DIST_DIR/machine-2/"
cp "$ROOT_DIR/config/postgresql/replica.conf" "$DIST_DIR/machine-3/"
cp "$ROOT_DIR/config/postgresql/replica.conf" "$DIST_DIR/machine-4/"

echo "==> Paquetes listos en $DIST_DIR"
