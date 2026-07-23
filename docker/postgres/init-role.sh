#!/bin/sh
# Script de inicialización condicional según el rol del nodo PostgreSQL.
set -e

if [ "$POSTGRES_ROLE" = "primary" ]; then
    echo "Configurando nodo PRIMARY..."
    cat >> "$PGDATA/postgresql.conf" <<-EOSQL
        wal_level = replica
        max_wal_senders = 5
        wal_keep_size = 512MB
EOSQL
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        CREATE ROLE replicator WITH REPLICATION LOGIN PASSWORD '${REPLICATOR_PASSWORD:-replica_pass}';
EOSQL
    echo "host replication replicator 0.0.0.0/0 scram-sha-256" >> "$PGDATA/pg_hba.conf"

elif [ "$POSTGRES_ROLE" = "replica" ]; then
    echo "Nodo configurado como REPLICA -- se espera pg_basebackup externo (ver deploy/deploy-to-cluster.sh)"
fi
