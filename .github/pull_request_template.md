## Qué hace

## Módulo

## Tipo
- [ ] Feature de módulo (no toca `core`, `core-testing`, `openapi.yaml`, `api` ni carpetas ajenas de `schema`)
- [ ] Contrato (solo interfaz/record + fake + test de contrato + stub, o solo `openapi.yaml`, o solo migración que altera columna)

## Migraciones añadidas

## Cómo lo probé
- [ ] `./mvnw -q spotless:apply verify` en verde (o `npm run gen && npm run lint && npm run format:check && npm run build && npm test` en web)
- [ ] Sin llamadas de red en tests
- [ ] La implementación real (si la hay) extiende el `Abstract…Contract` del adaptador

## Issue
Closes #
