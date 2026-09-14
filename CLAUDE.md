# Glucurvia — reglas para agentes

## Qué es
App personal de seguimiento metabólico conversacional (CGM + LLM). Diseño completo en `docs/diseno-mvp.md`;
plan de trabajo y reparto en `docs/plan-trabajo-paralelo.md`; rutinas del humano en `docs/guia-del-humano.md`.
Java 21, Spring Boot 3, Maven multi-módulo, PostgreSQL, React + Vite. Todo en español.

## Tu módulo
Trabajas en UN módulo, el que indica tu issue. No edites archivos de otros módulos ni de lo compartido
(`glucurvia-core`, `glucurvia-core-testing`, carpetas ajenas de `glucurvia-schema`, `docs/api/openapi.yaml`,
`glucurvia-api`) salvo en una PR de contrato. Módulos: core, core-testing, schema, cgm, glycemic, insights,
journal, nutrition, assistant, api, web.

## Vocabulario
**Adaptador**: interfaz en `glucurvia-core` (`mx.glucurvia.core.adapter`) que un módulo implementa con un `@Service` y otros consumen. Tiene tres implementaciones intercambiables: la real (en el módulo dueño), el fake (`core-testing`) y el stub (`api`, hasta que llega la real). `NightscoutAdapter` y `LibreViewCsvAdapter` son implementaciones de la interfaz interna `CgmSourceAdapter` de `cgm`, con la misma idea a escala de módulo.

## Reglas duras
1. Nunca push a `main`. Rama `feat/<módulo>/<tema>` desde `main` actualizado; PR pequeña (< 400 líneas,
   sin contar archivos de datos); squash.
2. Una PR de contrato (rama `contract/<qué>`, etiqueta `contract`) contiene solo: interfaz o record en
   `core`, fake y test de contrato abstracto en `core-testing`, y stub en `glucurvia-api/.../stubs`. Sin
   código de feature. Se mergea antes que la feature que la usa.
3. Si necesitas algo de otro módulo que no existe: abre tú la PR de contrato con la firma que necesitas y
   apila tu rama de feature sobre `contract/<qué>`. Sigue contra el fake. No implementes el módulo del otro.
4. Migraciones: solo en `glucurvia-schema/src/main/resources/db/migration/<tu módulo>/V<yyyyMMddHHmm>__<desc>.sql`
   y solo sobre tus tablas (propiedad en el plan, 5.1). Alterar o borrar una columna existente es PR de
   contrato. Nunca edites una migración que ya está en `main`.
5. Eventos entre módulos: publicar records de `mx.glucurvia.core.event` con `ApplicationEventPublisher`;
   escuchar solo con `@Async @TransactionalEventListener(phase = AFTER_COMMIT)`.
6. Antes de abrir PR: `./mvnw -q spotless:apply verify` en verde. La PR usa la plantilla y enlaza el issue.
7. Nunca `mvn install`. Siempre `-am` al construir un módulo suelto: `./mvnw -q -pl glucurvia-<módulo> -am verify`.
8. Archivos del humano: `pom.xml` padre, `docker-compose*.yml`, `application.yml` de `api`, `.github/`,
   este `CLAUDE.md`, `docs/` (salvo `openapi.yaml` por contrato). En `.env.example` solo tu bloque.
9. Records para DTOs; sin Lombok; sin Spring, JPA ni I/O en `core`; entidades JPA privadas del módulo;
   `java.time.Clock` inyectado, nunca `Instant.now()` en dominio; sin excepciones tragadas; sin `System.out`.
10. Ningún test llama a la API de Claude ni a ninguna red. `LlmGateway` (en `assistant`) tiene fake.
11. El LLM (módulo `assistant`) nunca calcula métricas ni decide confianzas: las tools llaman a servicios Java.
12. Controladores REST en tu módulo bajo `/api/v1/...`, exactamente como los describe `docs/api/openapi.yaml`.
    Un test de `api` falla si implementas un endpoint que no está en el contrato.
13. Si implementas un adaptador de `core`, tu clase de test extiende el `Abstract…Contract` de `core-testing`.
    Al arrancar la app, tu `@Service` sustituye al stub automáticamente.

## Cómo correr
- Postgres local de tu worktree: `docker compose -f docker-compose.dev.yml up -d` (usa el `.env` del worktree:
  `COMPOSE_PROJECT_NAME=glucurvia-<módulo>` y `PG_PORT` propios). Recrear: `... down -v`.
- Todo: `./mvnw -q verify` (Testcontainers levanta Postgres solo; requiere Docker).
- Un módulo: `./mvnw -q -pl glucurvia-<módulo> -am verify`.
- App local: `PG_PORT=<tu puerto> ./mvnw -q -pl glucurvia-api spring-boot:run` → `http://localhost:8080/actuator/health`.
- Web: `cd web && npm ci && npm run gen && npm run dev` (mocks activos por `.env.development`; `VITE_MOCKS=false` contra la API).
- Web antes de PR: `npm run gen && npm run lint && npm run format:check && npm run build && npm test`.

## Al terminar
Comenta en el issue (`gh issue comment <n>`): qué hiciste, qué no, qué contratos necesitas del otro lado. Enlaza la PR.
