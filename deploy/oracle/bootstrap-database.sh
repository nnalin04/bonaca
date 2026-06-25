#!/usr/bin/env bash
set -euo pipefail

: "${BONACA_DATABASE_PASSWORD:?BONACA_DATABASE_PASSWORD is required}"

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-infrastructure-postgres-1}"

docker exec \
  -e BONACA_DATABASE_PASSWORD="$BONACA_DATABASE_PASSWORD" \
  "$POSTGRES_CONTAINER" \
  sh -ceu '
    psql -v ON_ERROR_STOP=1 \
      --username "$POSTGRES_USER" \
      --dbname "$POSTGRES_DB" \
      --set=bonaca_password="$BONACA_DATABASE_PASSWORD" <<'"'"'SQL'"'"'
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '"'"'bonaca'"'"') THEN
    CREATE ROLE bonaca LOGIN;
  END IF;
END
$$;

ALTER ROLE bonaca WITH LOGIN PASSWORD :'"'"'bonaca_password'"'"';
SQL

    if ! psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
      --tuples-only --no-align --command \
      "SELECT 1 FROM pg_database WHERE datname = '"'"'bonaca'"'"'" | grep -qx 1; then
      createdb --username "$POSTGRES_USER" --owner bonaca bonaca
    fi
  '
