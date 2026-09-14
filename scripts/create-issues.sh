#!/usr/bin/env bash
# Crea en GitHub los issues de docs/issues/<fase>/*.md. Cada archivo empieza con dos líneas:
#   title: ...
#   labels: a,b,c
# y el resto es el cuerpo. Ejecutar una vez por fase: scripts/create-issues.sh fase-1
set -euo pipefail
REPO="${REPO:-windoctor7/glucurvia}"
DIR="docs/issues/${1:?fase (p. ej. fase-1)}"
for f in "$DIR"/*.md; do
  title=$(sed -n '1s/^title: //p' "$f")
  labels=$(sed -n '2s/^labels: //p' "$f")
  body=$(tail -n +3 "$f")
  gh issue create --repo "$REPO" --title "$title" --label "$labels" --body "$body" >/dev/null && echo "creado: $title"
done
