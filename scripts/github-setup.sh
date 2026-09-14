#!/usr/bin/env bash
# Etiquetas y protección de main (plan, apéndices C y D). Ejecutar una vez, con `gh auth login` hecho
# y DESPUÉS de que el CI haya corrido al menos una vez en una PR (para que existan los checks backend y web).
set -euo pipefail
REPO="${REPO:-windoctor7/glucurvia}"
for l in core schema cgm glycemic insights journal nutrition assistant api web; do
  gh label create "module:$l" --repo "$REPO" --color 0e8a16 --force >/dev/null && echo "module:$l"
done
gh label create "machine:imac" --repo "$REPO" --color 1d76db --force >/dev/null
gh label create "machine:macbook" --repo "$REPO" --color 5319e7 --force >/dev/null
gh label create "machine:human" --repo "$REPO" --color 000000 --force >/dev/null
gh label create "contract" --repo "$REPO" --color d93f0b --force >/dev/null
gh label create "blocked" --repo "$REPO" --color b60205 --force >/dev/null
for p in 1 2 3; do gh label create "phase:$p" --repo "$REPO" --color fbca04 --force >/dev/null; done
echo "etiquetas listas"
if [ "${PROTECT:-no}" = "yes" ]; then
  gh api -X PUT "repos/$REPO/branches/main/protection" --input - <<'JSON'
{"required_status_checks":{"strict":true,"contexts":["backend","web"]},"enforce_admins":false,"required_pull_request_reviews":null,"restrictions":null,"allow_force_pushes":false,"allow_deletions":false}
JSON
  echo "main protegida: PR obligatoria y checks backend + web"
else
  echo "protección de main no aplicada (ejecuta con PROTECT=yes cuando el CI haya corrido una vez)"
fi
