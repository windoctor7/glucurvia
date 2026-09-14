#!/usr/bin/env bash
# Despliegue de producción personal. Solo desde el clon de prod en el iMac (guía 6.1).
set -euo pipefail
cd "${GLUCURVIA_PROD_DIR:-$HOME/Desarrollo/glucurvia-prod}"
git pull --ff-only origin main
docker compose --profile prod up -d --build
sleep 15
curl -fsS http://localhost:8080/api/v1/cgm/status && echo && echo "OK $(git rev-parse --short HEAD)"
