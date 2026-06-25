#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
SOURCE_DIR="${1:-$DEPLOY_DIR/source}"

test -f "$DEPLOY_DIR/.env"
test -f "$SOURCE_DIR/backend/Dockerfile"

docker build \
  --tag bonaca-backend:0.0.1-SNAPSHOT \
  --tag bonaca-backend:remote-dev \
  "$SOURCE_DIR/backend"

docker compose --project-directory "$DEPLOY_DIR" \
  -f "$DEPLOY_DIR/docker-compose.yml" \
  up -d --force-recreate backend

for attempt in $(seq 1 30); do
  status="$(docker inspect bonaca-backend --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}starting{{end}}')"
  if [ "$status" = "healthy" ]; then
    curl --fail --silent http://localhost:8090/health
    exit 0
  fi
  sleep 2
done

docker logs --tail 200 bonaca-backend
exit 1
