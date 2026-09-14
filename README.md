# Glucurvia

Seguimiento metabólico conversacional para usuarios de FreeStyle Libre. Uso personal (Ciudad de México), móvil primero, glucosa casi en tiempo real con Nightscout como capa de ingesta.

- Diseño completo: [docs/diseno-mvp.md](docs/diseno-mvp.md) (versión 0.5; el registro de cambios está en su sección 15).
- Plan de trabajo en paralelo (MacBook + iMac, varios agentes): [docs/plan-trabajo-paralelo.md](docs/plan-trabajo-paralelo.md).
- Guía del humano (rutinas, listas de comprobación y comandos): [docs/guia-del-humano.md](docs/guia-del-humano.md).
- Versión publicada del diseño: https://claude.ai/code/artifact/497f928b-39bd-4b73-90c4-c51e31e42dfc

## Día 1 (antes de escribir código)

1. `cp .env.example .env` y rellenar: secreto de Nightscout y la cuenta seguidora de LibreLinkUp.
2. `docker compose up -d mongo nightscout postgres`
3. Abrir http://localhost:1337, entrar con el `API_SECRET`, crear en Admin Tools los dos tokens (uno de escritura para el uploader, uno de solo lectura para la API) y copiarlos al `.env`.
4. `docker compose up -d librelink-up` y comprobar que aparece la glucosa en el panel.
5. Usarlo unos días desde el teléfono (con Tailscale) antes de construir la API.

## Estructura

- `docs/` — diseño del MVP. `tools/docs-render/` lo convierte a HTML.
- `docker-compose.yml` — los cinco servicios; `api` queda comentada hasta la fase 1.
- A partir de la fase 1, el proyecto Spring Boot vive en la raíz (roadmap en la sección 12 del diseño).

## Renderizar el documento

```bash
cd tools/docs-render && npm install && node render.js ../../docs/diseno-mvp.md ../../docs/diseno-mvp.html
```
