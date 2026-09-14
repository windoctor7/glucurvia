# Glucurvia

Seguimiento metabólico conversacional para usuarios de FreeStyle Libre. Uso personal (Ciudad de México), móvil primero, glucosa casi en tiempo real con Nightscout como capa de ingesta.

- Diseño completo: [docs/diseno-mvp.md](docs/diseno-mvp.md) (versión 0.5; el registro de cambios está en su sección 15).
- Plan de trabajo en paralelo (MacBook + iMac, varios agentes): [docs/plan-trabajo-paralelo.md](docs/plan-trabajo-paralelo.md).
- Guía del humano (rutinas, listas de comprobación y comandos): [docs/guia-del-humano.md](docs/guia-del-humano.md).

## Día 1 (antes de escribir código)

1. `cp .env.example .env` y rellenar: secreto de Nightscout y la cuenta seguidora de LibreLinkUp.
2. `docker compose up -d mongo nightscout postgres`
3. Abrir http://localhost:1337, entrar con el `API_SECRET`, crear en Admin Tools los dos tokens (uno de escritura para el uploader, uno de solo lectura para la API) y copiarlos al `.env`.
4. `docker compose --profile prod up -d librelink-up` y comprobar que aparece la glucosa en el panel (el uploader solo corre en el iMac).
5. Usarlo unos días desde el teléfono (con Tailscale) antes de construir la API.

## Desarrollo

- Java 21 y Maven por SDKMAN (`sdk env install`), Node 22 (`.nvmrc`), Docker.
- Todo el backend: `./mvnw -q verify` (Testcontainers levanta Postgres solo). Un módulo: `./mvnw -q -pl glucurvia-<módulo> -am verify`. Nunca `mvn install`.
- Postgres local por worktree: `docker compose -f docker-compose.dev.yml up -d` (con `COMPOSE_PROJECT_NAME` y `PG_PORT` en el `.env` del worktree).
- Web: `cd web && npm ci && npm run gen && npm run dev`.
- Reglas para agentes en `CLAUDE.md`; reparto y coordinación en `docs/plan-trabajo-paralelo.md`; rutinas del humano en `docs/guia-del-humano.md`.

## Estructura

- `glucurvia-core` tipos, adaptadores, eventos y utilidades puras · `glucurvia-core-testing` fakes, tests de contrato, Testcontainers · `glucurvia-schema` migraciones Flyway por módulo.
- `glucurvia-cgm`, `-glycemic`, `-insights` (iMac) · `glucurvia-journal`, `-nutrition`, `-assistant` (MacBook) · `glucurvia-api` cablea todo y sirve stubs por adaptador.
- `web/` React + Vite, tipos generados de `docs/api/openapi.yaml`.
- `docs/` diseño, plan, guía y contrato REST.
- `docker-compose.yml` producción personal (iMac); `docker-compose.dev.yml` Postgres de desarrollo.

