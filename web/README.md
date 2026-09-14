# web

Web responsiva móvil-primero, instalable desde "Añadir a pantalla de inicio" (diseño 1.4).

- `npm run gen` genera `src/api/schema.d.ts` desde `../docs/api/openapi.yaml`. Es el contrato: no inventes campos.
- `npm run dev` con `VITE_MOCKS=true` (por defecto en `.env.development`) sirve los mocks de `src/mocks/handlers.ts`.
- Contra la API real: `VITE_MOCKS=false npm run dev` (proxy a `http://localhost:8080`, o `API_TARGET=http://<imac>:8080`).
- Antes de abrir PR: `npm run gen && npm run lint && npm run build && npm test`.
