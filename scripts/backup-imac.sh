#!/usr/bin/env bash
# Volcado nocturno de Postgres a una carpeta que Time Machine sí respalda (guía 6.3). Programar a las 03:00.
set -euo pipefail
cd "${GLUCURVIA_PROD_DIR:-$HOME/Desarrollo/glucurvia-prod}"
mkdir -p "$HOME/Backups/glucurvia"
docker compose --profile prod exec -T postgres pg_dump -U "${POSTGRES_USER:-glucurvia}" "${POSTGRES_DB:-glucurvia}" \
  | gzip > "$HOME/Backups/glucurvia/glucurvia-$(date +%F).sql.gz"
find "$HOME/Backups/glucurvia" -name 'glucurvia-*.sql.gz' -mtime +60 -delete
