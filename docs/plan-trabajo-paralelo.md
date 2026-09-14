# Glucurvia — Plan de trabajo en paralelo

Versión 0.2 · 13 de septiembre de 2026 · Dos máquinas (MacBook e iMac), varios agentes por máquina, un solo humano que revisa e integra. Complementa el diseño en `docs/diseno-mvp.md`. Los cambios respecto a la 0.1 y los problemas que corrigen están en el apéndice F.

## 0. La idea en cinco reglas

1. **Git es el único canal.** Los agentes de una máquina no hablan con los de la otra: se comunican por contratos en `core`, por el contrato REST en `docs/api/openapi.yaml`, por issues en GitHub, por PRs pequeñas y por un CI que compila y prueba todo en cada PR. Lo que no está en el repositorio no existe.
2. **Un módulo, un dueño a la vez.** Cada módulo Maven pertenece a una máquina y, dentro de ella, a un agente. Nadie edita archivos fuera de su módulo salvo por una PR de contrato.
3. **Lo compartido es contrato y cambia primero.** `core` (tipos, adaptadores, eventos, utilidades puras), `core-testing` (fakes y tests de contrato), `schema` (migraciones) y `openapi.yaml`. Se modifican con PRs pequeñas etiquetadas `contract`, sin código de feature, que se mergean antes que las features que las usan.
4. **Si necesitas algo del otro lado que no existe, abre tú la PR de contrato con el adaptador y su fake, y sigue contra el fake.** Nunca implementes el módulo del otro. La app siempre arranca porque cada adaptador tiene un *stub* hasta que llega la implementación real.
5. **Main siempre verde, ramas de horas, no de días.** Rebase sobre `main` al empezar, PR de menos de 400 líneas al terminar (los archivos de datos no cuentan), squash, y a la siguiente.

---

## 1. Reparto: máquinas, módulos, agentes

El iMac es el servidor de "producción personal" y tiene al lado la base de datos y Nightscout reales: se queda con los módulos de datos. La MacBook se queda con producto, LLM e interfaz. Lo transversal es del humano.

| Módulo | Dueño | Contratos que consume | Qué entrega |
|---|---|---|---|
| `core` | humano (por PRs de contrato que abren los agentes) | — | tipos de valor, enums, adaptadores, eventos de dominio, utilidades puras |
| `core-testing` | humano (ídem) | — | fakes de cada adaptador, tests de contrato abstractos, constructores, entradas de curvas sintéticas |
| `schema` | humano el baseline; cada módulo su carpeta | — | todas las migraciones Flyway, en carpetas por módulo |
| `api` | humano | todos | aplicación Spring Boot que cablea los módulos, autoconfiguración de stubs, `application.yml`, Flyway, `docker-compose`, CI, servir la web |
| `cgm` | iMac · agente A | — | `NightscoutAdapter`, `LibreViewCsvAdapter`, ingesta, reversión de lotes, endpoints `/cgm/*`; publica `ReadingsIngested` |
| `glycemic` | iMac · agente B | `MealEventReader`, `GlucoseSeriesReader` | algoritmo, recomputación, `GET /events/{id}/glycemic-response`, valores esperados de las curvas sintéticas |
| `insights` | iMac · agente B (fase 2) | lectura directa de tablas (5.4) | consultas, comparaciones, ruido propio, `GET /insights/summary` |
| `journal` | MacBook · agente C | — | eventos, comidas, ítems, correcciones, endpoints `/events/*`; implementa `EventJournal` y `MealEventReader`; publica `EventLogged`, `EventUpdated` |
| `nutrition` | MacBook · agente C | — | catálogo es-MX, resolución, cálculo con rangos, confianza, política de aclaración, alias del usuario; implementa `NutrientEstimator` |
| `assistant` | MacBook · agente D (fase 2) | `EventJournal`, `NutrientEstimator`, `InsightsQueries`, `CgmSyncPort`, `GlucoseSeriesReader` | bucle de tools, prompt, rutas fijas, robustez, conversaciones, endpoint `/chat/*` |
| `web` | MacBook · agente E | `openapi.yaml` | web responsiva instalable: chat, línea de tiempo, curvas |

Los controladores REST viven en el módulo dueño de la funcionalidad, no en `api`; `api` solo los descubre por escaneo de paquetes. Que dos módulos sirvan rutas bajo el mismo prefijo (`/events/{id}` de `journal` y `/events/{id}/glycemic-response` de `glycemic`) es normal en Spring: la frontera de propiedad es el archivo `openapi.yaml`, no el prefijo de la URL.

Cuántos agentes: **dos por máquina en la fase 1, hasta tres en la fase 2**. El límite no es la máquina, es el humano: más de cuatro o cinco PRs al día no se revisan bien.

Cada agente trabaja en su propio *worktree* de Git (una carpeta por agente, una rama por issue), nunca dos agentes sobre la misma carpeta, y cada worktree tiene **su propia base de datos** (6.1):

```bash
git worktree add -b feat/cgm/nightscout-adapter ../glucurvia-cgm main
```

y se abre una sesión de Claude Code por carpeta.

---

## 2. Estructura del repositorio que hace posible el paralelismo

Maven multi-módulo. La dirección de las dependencias la impone el build: un módulo solo ve `core`, `core-testing` y `schema`, así que un agente no puede acoplarse a otro módulo aunque quiera, y un cambio en `cgm` no puede romper la compilación de `nutrition`.

```
glucurvia/
├── pom.xml                      # padre: versiones compartidas; lo edita solo el humano
├── glucurvia-core/              # tipos, adaptadores, eventos, utilidades puras; SIN Spring, SIN JPA, SIN I/O
├── glucurvia-core-testing/      # fakes, tests de contrato abstractos, constructores, curvas (scope test en módulos)
├── glucurvia-schema/            # solo recursos: db/migration/<módulo>/V<timestamp>__<desc>.sql
├── glucurvia-cgm/
├── glucurvia-glycemic/
├── glucurvia-insights/
├── glucurvia-journal/
├── glucurvia-nutrition/
├── glucurvia-assistant/
├── glucurvia-api/               # @SpringBootApplication, stubs, application.yml; único módulo con jar ejecutable
├── web/                         # React + Vite + TypeScript; build independiente; tipos generados de openapi.yaml
├── docs/api/openapi.yaml        # contrato REST
├── docker-compose.yml           # producción personal (iMac); el uploader va bajo el perfil `prod`
└── docker-compose.dev.yml       # solo Postgres, parametrizado por worktree
```

```mermaid
flowchart TB
  API["api · humano<br/>cablea todo + stubs"] --> CGM & GLY & INS & JR & NU & AS
  CGM["cgm · iMac"] --> CORE
  GLY["glycemic · iMac"] --> CORE
  INS["insights · iMac"] --> CORE
  JR["journal · MacBook"] --> CORE
  NU["nutrition · MacBook"] --> CORE
  AS["assistant · MacBook"] --> CORE
  CORE["core<br/>tipos · adaptadores · eventos · utilidades puras"]
  SCH["schema<br/>migraciones"] -. test .-> CGM & GLY & INS & JR & NU & AS
  API --> SCH
  WEB["web · MacBook"] -. openapi.yaml .-> API
```

Reglas de dependencia:

- Ningún módulo depende de otro módulo salvo de `core` (compilación) y de `core-testing` y `schema` (tests). Si `assistant` necesita comidas, no importa `journal`: usa el adaptador `EventJournal` de `core`, que `journal` implementa y `api` cablea.
- Las llamadas entre módulos son de dos tipos. **Consultas** por adaptador: interfaz en `core`, implementación `@Service` en el módulo dueño. **Disparadores** por evento de dominio: record en `core`, publicado con `ApplicationEventPublisher` por el emisor; el receptor escucha con `@Async @TransactionalEventListener(phase = AFTER_COMMIT)`. Nunca un listener síncrono: un fallo en `glycemic` (iMac) revertiría la transacción de ingesta de `cgm`, y un fallo en `glycemic` rompería el registro de comidas de `journal` (MacBook). Lo que un listener pierda lo recoge el reconciliador `@Scheduled` del diseño.
- `core` no tiene Spring, JPA ni I/O; sí tiene lógica pura compartida (`Series` con sus operaciones, `Resampler`, conversiones de unidades). Si dos módulos necesitan la misma utilidad y es pura, va a `core` por PR de contrato; si tiene I/O, no se comparte: cada módulo la tiene.
- Las entidades JPA son privadas de cada módulo. Entre módulos solo circulan records de `core`.
- Cada adaptador de `core` tiene un **stub** en `api`: una autoconfiguración (`META-INF/spring/…AutoConfiguration.imports`) con un bean `@ConditionalOnMissingBean` por adaptador, que envuelve el fake de `core-testing`. Tiene que ser autoconfiguración y no una `@Configuration` normal, porque solo así Spring la evalúa después de los `@Service` reales. Al arrancar, `api` escribe en el log qué adaptadores siguen en stub; con el perfil `prod` activo, si queda alguno, la aplicación no arranca.

---

## 3. El canal de comunicación: Git y GitHub

| Mecanismo | Para qué | Quién lo lee |
|---|---|---|
| `CLAUDE.md` en la raíz (apéndice A) | Las reglas de trabajo. Claude Code lo carga en cada sesión: es la forma de "hablar" con todos los agentes a la vez. Cada módulo puede tener además su propio `CLAUDE.md`, del dueño. | todos los agentes, siempre |
| `core`, `core-testing`, `schema`, `openapi.yaml` | El contrato: qué existe, con qué firma, cómo se simula, qué tablas hay, qué devuelve cada endpoint. | agentes que consumen o implementan |
| Issues de GitHub | Una tarea por issue, etiquetada por módulo, máquina y fase. El agente lee su issue al empezar y comenta al terminar: qué hizo, qué no, qué contrato necesita. Es el tablero. | humano y agentes |
| PRs con plantilla (apéndice B) | Cómo se hizo y cómo se probó; si es contrato; qué migraciones añade. | humano |
| CI en GitHub Actions (apéndice C) | Formato, compilación y pruebas de **todos** los módulos en cada PR, más la deriva del OpenAPI (un test de `api` falla si hay un endpoint implementado que no está en el contrato y lista los pendientes). Es la única forma de que un agente del iMac sepa que no rompió a uno de la MacBook. | todos, automáticamente |
| Protección de `main` | Solo se entra por PR con CI verde. Sin exigir aprobaciones: con una sola cuenta de GitHub no podrías aprobar tus propias PRs. | GitHub |
| Comentario de cierre en el issue | El "informe" del agente. Sustituye a cualquier chat. | humano y el agente del otro lado |

Ramas: `feat/<módulo>/<tema>`, `contract/<qué>`, `fix/<módulo>/<qué>`, `chore/<qué>`. Squash al mergear; el título de la PR es el mensaje.

Archivos del humano, que ningún agente edita: `pom.xml` padre, `docker-compose*.yml`, `application.yml` de `api`, `.github/`, `CLAUDE.md` de la raíz y `docs/` (salvo `openapi.yaml`, que cambia por PR de contrato). `.env.example` lo edita cada dueño solo en el bloque de su módulo.

Dos refuerzos baratos: antes de que el humano mire una PR, un agente le pasa `/code-review` y deja los hallazgos como comentario; y toda PR de contrato la comenta también el agente dueño del módulo que la implementará ("¿puedo implementar esta firma?"), con `gh pr view` y `gh pr comment`. Esa revisión cruzada es lo más parecido a que las dos máquinas hablen.

---

## 4. Librerías compartidas: la política de `core`

**Vocabulario.** Un *adaptador* es una interfaz en `core` que un módulo implementa con un `@Service` y otros consumen, con tres implementaciones intercambiables: la real, el fake de `core-testing` y el stub de `api`. Las clases `NightscoutAdapter` o `LibreViewCsvAdapter` de `cgm` son la misma idea a escala de módulo: implementaciones intercambiables de su interfaz interna `CgmSourceAdapter`.

1. **Solo lo compartido se comparte.** Si dos módulos necesitan lo mismo, va a `core` (si es puro) o se duplica (si tiene I/O). Si solo lo necesita uno, se queda en ese módulo aunque parezca genérico.
2. **Cambios aditivos por defecto.** Añadir un record, un campo con valor por defecto, un método `default` en la interfaz. Quitar o renombrar exige una PR de contrato con los dos lados adaptados en PRs inmediatamente posteriores, el mismo día.
3. **Una PR de contrato contiene exactamente**: la interfaz o el record en `core`, el fake y el test de contrato abstracto en `core-testing`, y el bean stub en `api`. Sin lógica de negocio. Se revisa mirando la firma: ¿la puede implementar el dueño? ¿la puede consumir el otro? Se mergea antes que cualquier feature que la use.
4. **El consumidor abre la PR de contrato y apila su feature encima.** Rama `contract/<qué>` desde `main`, PR; rama `feat/…` creada **desde** `contract/<qué>`, donde sigue trabajando contra el fake. Cuando el contrato se mergea (squash), `git rebase --onto main contract/<qué> feat/…`. El dueño implementa cuando le toque; nadie espera a nadie.
5. **Fake y real pasan el mismo test.** `core-testing` tiene por adaptador una clase abstracta `AbstractXxxContract` con los casos que definen el comportamiento (qué devuelve con n < 5, qué pasa sin lecturas, etc.). El fake la extiende en `core-testing`; la implementación real la extiende en su módulo. Así la fase 3 no descubre que el fake "mentía".
6. **Sincronía por `main`, no por versiones.** `core` no se publica a ningún repositorio de artefactos: es un módulo del mismo build. Rebase sobre `main` al empezar cada tarea; si `core` cambió, compilar antes de seguir.
7. **Versiones de librerías**: en el `pom.xml` del módulo mientras solo la use ese módulo; cuando la usen dos, sube al `dependencyManagement` del padre por PR de contrato. Así un agente no se bloquea por añadir una dependencia.
8. **Sin Lombok**: records y clases planas. Dos máquinas y cinco agentes con estilos distintos se pisan en los diffs; el formateador (6.3) y esta regla lo evitan.

### 4.1 Contratos iniciales (se crean todos en la fase 0)

| Tipo | Nombre | Dueño de la implementación | Consumidores | Firma esencial |
|---|---|---|---|---|
| valor | `Mgdl`, `Grams`, `Range<T>` (punto, bajo, alto), `Confidence`, `UserId`, `EventId`, `Series` (lecturas ordenadas con `between`, `median`, `slope`…), `Resampler` | core | todos | records inmutables y utilidades puras |
| valor | `ExtractedMeal`, `ExtractedItem` (la salida del LLM del diseño 8.4) y `EstimatedMeal`, `EstimatedItem` (con rangos, confianza, `macro_source`) | core | assistant, nutrition, journal | son la frontera entre el LLM, el estimador y el registro |
| valor | `MealEvent(eventId, startedAt, localTz, timeConfidence, carbsG, dominantFoodRefId, tags)`, `ContextEvent` | core | glycemic, insights | lo que `insights` necesita para el ruido propio va aquí, no en otra consulta |
| enum | `ReadingType`, `SeriesSource`, `EventType`, `TimeConfidence`, `Quality`, `MacroSource` | core | todos | según el diseño (3, 6) |
| adaptador | `GlucoseSeriesReader` | cgm | glycemic, insights, assistant | `Series between(UserId, Instant, Instant)`; `Optional<Reading> latest(UserId)` |
| adaptador | `CgmSyncPort` | cgm | assistant | `SyncResult syncNow(UserId, Duration timeout)` |
| adaptador | `MealEventReader` | journal | glycemic, insights | `List<MealEvent> mealsBetween(...)`; `List<ContextEvent> contextBetween(...)` |
| adaptador | `EventJournal` | journal | assistant | `Event log(NewEvent)`; `Event update(EventId, EventPatch)`; `List<Event> find(EventQuery)` |
| adaptador | `NutrientEstimator` | nutrition | assistant | `EstimatedMeal estimate(ExtractedMeal, UserId)`; `void rememberCorrection(UserId, CorrectedItem)` |
| adaptador | `GlycemicResponseReader` | glycemic | insights, assistant | `Optional<GlycemicResponse> forMeal(EventId)` |
| adaptador | `InsightsQueries` | insights | assistant | `List<MealSummary> meals(MealFilter)`; `Comparison compare(MealFilter, MealFilter, Metric)`; `GlucoseSummary summary(UserId, LocalDate, LocalDate)` |
| evento | `ReadingsIngested(userId, from, to, source)` | cgm | glycemic | dispara recomputación |
| evento | `EventLogged(userId, eventId, type, startedAt)`, `EventUpdated(...)` | journal | glycemic, nutrition | recomputación; aprendizaje de alias |
| fakes + contratos | `InMemoryGlucoseSeries`, `FakeMealEvents`, `FakeEventJournal`, `FakeNutrientEstimator`, `FakeInsights` y sus `Abstract…Contract`; constructores `aMeal()`, `aCurve()`; entradas de 8–10 curvas sintéticas (sin valores esperados: esos los produce `glycemic`) | core-testing | tests de todos | comportamiento simple y determinista |

Quién orquesta el registro de una comida: `assistant` llama a `NutrientEstimator.estimate` y después a `EventJournal.log` con la comida ya estimada; `journal` guarda lo que recibe y no llama al estimador. Una corrección sigue el mismo camino más `rememberCorrection`.

El adaptador hacia Claude (`LlmGateway`) **no** va en `core`: solo lo usa `assistant`, así que vive allí, con un fake propio para sus tests. Ningún test del repositorio llama a la API de Claude: sin secreto en CI, una prueba así pondría `main` en rojo para las dos máquinas. La evaluación con el modelo real es un job manual (apéndice C).

---

## 5. La base de datos compartida

Una sola PostgreSQL y seis módulos escribiendo migraciones es el segundo punto de choque.

### 5.1 Propiedad de tablas

| Módulo | Tablas |
|---|---|
| `api` | `users` |
| `cgm` | `import_batches`, `glucose_readings`, `cgm_pull_state` |
| `journal` | `events`, `meals`, `meal_items` |
| `nutrition` | `food_references`, `user_food_aliases` |
| `assistant` | `conversations`, `conversation_messages` |
| `glycemic` | `glycemic_responses` |
| `insights` | ninguna; solo lee (5.4) |

`user_food_aliases` es de `nutrition` porque el estimador las lee en cada resolución; `conversations` es de `assistant` porque es quien las escribe. `events.message_id` es una clave foránea de `journal` a la clave primaria de `assistant`: permitido, porque solo apunta a la PK.

### 5.2 Migraciones

- **Todas en `glucurvia-schema`**, un módulo de solo recursos del que dependen `api` (compilación) y cada módulo (tests). Carpeta por módulo y versiones con marca de tiempo: `db/migration/<módulo>/V202609141530__anade_scan_fraction.sql`. Cada dueño edita solo su carpeta.
- **El baseline lo escribe el humano en la fase 0 con el DDL completo del diseño (sección 3)**: `db/migration/core/V202609130000__baseline.sql`. Como todas las tablas existen desde el principio, las claves foráneas entre módulos no dependen del orden de merge, y los módulos solo añaden migraciones para cambios.
- **`outOfOrder=true` en todos los entornos**, también en el iMac. El orden de merge de dos ramas nunca coincide con el orden de las marcas de tiempo, y eso pasa igual en producción. Lo que lo hace seguro es el invariante anterior: cada módulo toca solo sus tablas, así que dos migraciones de módulos distintos son independientes por construcción.
- **Alterar o borrar una columna existente es una PR de contrato** (etiqueta `contract`), aunque la tabla sea tuya: `insights` la lee, y el `openapi.yaml` puede exponerla. Añadir columnas con valor por defecto no lo es.
- **Nunca se edita una migración que ya está en `main`.** Antes de mergear sí se puede reescribir; si tu base de datos local se queja del checksum, se recrea (`docker compose … down -v`). `flyway repair` no se usa nunca en el iMac.

### 5.3 Tests de integración

`core-testing` trae una extensión de JUnit que levanta un Postgres efímero con Testcontainers y aplica **todas** las migraciones de `schema`. Cada módulo la usa; no hay "solo mis migraciones", porque las claves foráneas cruzan módulos. Un test de `nutrition` no depende de que `glycemic` compile, pero sí de que su migración sea válida, que es exactamente lo que queremos saber.

### 5.4 La excepción de `insights`

`insights` es un módulo de **solo lectura** que consulta con SQL las tablas de `glycemic`, `journal` y `cgm` para agregados (percentiles, medianas, ruido propio). Pasar esos agregados por adaptadores Java sería absurdo. A cambio: no escribe nunca, y cualquier cambio de columna en las tablas que lee es PR de contrato (5.2), de modo que el CI de `insights` avisa antes de que llegue a `main`.

---

## 6. Entornos

### 6.1 Una base de datos por worktree

Dos agentes en la misma máquina compartiendo el Postgres de `docker-compose.dev.yml` es la forma más rápida de pisarse: Flyway aplica las migraciones de la rama de uno sobre la base del otro y ninguno entiende el error. Cada worktree lleva su `.env` (ignorado por Git) con `COMPOSE_PROJECT_NAME=glucurvia-<módulo>` y `PG_PORT=<puerto libre>`; `docker-compose.dev.yml` los usa para nombrar el contenedor y el volumen, y `application.yml` lee `${PG_PORT:5432}`. Los tests no necesitan nada de esto: Testcontainers da una base a cada ejecución.

### 6.2 Dónde corre qué

| Dónde | Qué corre | Para qué |
|---|---|---|
| iMac, clon dedicado `~/Desarrollo/glucurvia-prod` con `docker-compose.yml --profile prod` | API, Postgres, Nightscout, MongoDB, uploader | producción personal. El clon de prod solo hace `git pull` de `main`; nunca es un worktree de agente, porque `docker compose up --build` desplegaría la rama que tuviera abierta. **El uploader corre solo aquí**: va bajo `profiles: [prod]` para que un `docker compose up` en la MacBook no arranque un segundo uploader por accidente. |
| iMac y MacBook, un `docker-compose.dev.yml` por worktree | solo Postgres | desarrollo y ejecución local |
| CI | Postgres efímero por Testcontainers | pruebas de todos los módulos en cada PR |
| Tailscale | red privada entre iMac, MacBook y teléfono | la MacBook lee datos reales apuntando `NIGHTSCOUT_URL` al Nightscout del iMac con el token de solo lectura (varios sondeos de lectura son inofensivos), y el teléfono llega a la API |

Configuración: cada módulo lee lo suyo con `@ConfigurationProperties(prefix = "glucurvia.<módulo>")` y valores por defecto en código; los valores de entorno llegan por variables (`GLUCURVIA_CGM_NIGHTSCOUT_URL`) sin tocar `application.yml`. El `.env.example` tiene un bloque por módulo que edita su dueño.

Despliegue al iMac: un script de tres líneas que el humano ejecuta al final del día en el clon de prod: `git pull`, `docker compose --profile prod up -d --build`, y comprobar `GET /cgm/status`. El arranque falla si algún adaptador sigue en stub (2).

### 6.3 Mismas herramientas en las dos máquinas

- `.sdkmanrc` con el JDK 21 exacto y `.nvmrc` con Node 22. Sin esto, "en mi máquina compila".
- Formato automático y verificado en CI: Spotless con google-java-format para Java, Prettier para `web`, `.editorconfig` para todo. Dos máquinas formateando distinto generan diffs de cientos de líneas y conflictos que no son conflictos.
- `java.time.Clock` inyectado en todo lo que mira la hora; nada de `Instant.now()` en dominio. Tests deterministas en Mac y en CI (`TZ=UTC` en CI).
- Docker: OrbStack o Docker Desktop; Testcontainers los detecta sin configuración.
- `gh auth login` en las dos máquinas, porque los agentes leen issues y abren PRs con `gh`.

---

## 7. Paso a paso

### Fase 0 — Esqueleto (secuencial; una máquina, un agente, el humano presente; 2–3 días)

Nada se paraleliza antes de esto. Es lo que convierte "trabajar juntos" en algo posible.

1. `pom.xml` padre (Java 21, Spring Boot 3, Maven Wrapper, Spotless) y los diez módulos vacíos que compilan; solo `api` empaqueta jar ejecutable. `api` arranca con `@SpringBootApplication(scanBasePackages = "mx.glucurvia")`, `@EnableAsync`, `@EnableScheduling`, `@EntityScan` y `@EnableJpaRepositories` sobre `mx.glucurvia`, y responde `GET /actuator/health`.
2. `core` con todos los tipos, enums, adaptadores, eventos y utilidades de la tabla 4.1. `core-testing` con los fakes, los tests de contrato abstractos, los constructores, las entradas de las curvas sintéticas y la extensión de JUnit con Testcontainers.
3. `schema` con el baseline completo del diseño y la fila de `users` sembrada (`America/Mexico_City`, `es-MX`); Flyway en `api` con las carpetas por módulo y `outOfOrder=true`.
4. Stubs: la autoconfiguración de `api` con un bean `@ConditionalOnMissingBean` por adaptador, el informe al arrancar y el fallo con perfil `prod`.
5. `docs/api/openapi.yaml` v0 con los endpoints de la sección 7 del diseño; en `web`, generación de tipos TypeScript y mocks a partir del archivo; en CI, el test de deriva (apéndice C).
6. `docker-compose.dev.yml` parametrizado por worktree; `docker-compose.yml` con el uploader bajo el perfil `prod`; clon de prod en el iMac.
7. `.sdkmanrc`, `.nvmrc`, `.editorconfig`, Spotless y Prettier configurados y aplicados una vez a todo.
8. CI (apéndice C) y protección de `main`: PR obligatoria y checks verdes, sin aprobaciones.
9. `CLAUDE.md` (apéndice A), plantilla de PR (apéndice B), etiquetas (apéndice D), `gh auth` en las dos máquinas.
10. Issues de la fase 1, uno por tarea, con módulo, máquina, fase, contratos que usa y criterio de terminado.
11. Etiqueta `skeleton` en Git. Las dos máquinas hacen pull. A partir de aquí, cada agente en su worktree con su base de datos.

### Fase 1 — En paralelo (dos agentes por máquina)

**iMac · agente A · `cgm`**

- `cgm-1` `NightscoutAdapter`: lectura de `entries` desde `last_reading_ts`, mapeo `sgv`/`mbg`, `cgm_pull_state`, sondeo cada minuto, `POST /cgm/sync`, `GET /cgm/status`. Publica `ReadingsIngested`.
- `cgm-2` `LibreViewCsvAdapter`: una generación, un idioma, falla ruidosa; `assumed_timezone`; corpus de dos archivos.
- `cgm-3` `CgmIngestionService`: dedup por PK natural, lotes, `DELETE /cgm/imports/{id}` con recomputación.
- `cgm-4` `GET /cgm/readings` con `raw|5m`, usando el `Resampler` de `core`.

**iMac · agente B · `glycemic`**

- `gly-1` `Series` y `Resampler` en `core` (PR de contrato, la abre B), selección de serie, rejilla anclada, cobertura por cadencia nominal (diseño 6.1, pasos 0–3), contra `InMemoryGlucoseSeries`.
- `gly-2` Basal, pico, valores fijos, recuperación, iAUC, pendientes, minutos sobre p90, calidad, etiquetas (pasos 4–13). Valores esperados de las curvas sintéticas, calculados y justificados a mano en el test.
- `gly-3` Recomputación: escucha `ReadingsIngested` y `EventLogged` (asíncrono, tras commit), ventana abierta con `IN_PROGRESS`, reconciliador; `GET /events/{id}/glycemic-response`; `GlycemicResponseReader` real pasando `AbstractGlycemicResponseReaderContract`. Usa el stub de `MealEventReader` hasta que `journal` esté en `main`.

**MacBook · agente C · `journal` y `nutrition`**

- `jr-1` Servicios de eventos, comidas e ítems sobre las tablas del baseline; `EventJournal` y `MealEventReader` reales pasando sus contratos; `client_message_id`; `EventLogged`.
- `nu-1` Catálogo es-MX: `carb_convention`, semilla de ~150 alimentos generada y revisada (archivo de datos, exento del límite de líneas), `search_text` y búsqueda por trigramas.
- `nu-2` `NutrientEstimator` real: resolución con alias, cálculo con rangos, confianza derivada, banda RSS×1,3, política de aclaración, `rememberCorrection`. Sin LLM, con el set de 25–40 frases como fixtures, pasando `AbstractNutrientEstimatorContract`.
- `jr-2` Correcciones, `repeat_of_event_ref`, `PATCH /events/{id}`, `EventUpdated`.

**MacBook · agente E · `web`**

- `web-1` Esqueleto React + Vite móvil-primero, instalable, chat con la caja fija abajo, contra los mocks generados del `openapi.yaml`.
- `web-2` Detalle de comida con curva de una ventana (eje relativo −1 h … +4 h) y *bottom sheet* "¿por qué este número?", con datos sintéticos.
- `web-3` Línea de tiempo y subida de CSV; enlace al panel de Nightscout.

**Humano**: `pg_dump` nocturno en el compose de prod; contraseña única o Tailscale delante de la API; `api` sirviendo `web/dist` con un Dockerfile multietapa.

### Fase 2 — En paralelo (hasta tres agentes por máquina)

- **iMac · B · `insights`**: `ins-1` `get_meals` con filtros y calidad; `ins-2` `compare` con n ≥ 5, control de carga de hidratos y ruido propio; `ins-3` resumen diario y `GET /insights/summary`. `InsightsQueries` real pasando su contrato.
- **MacBook · D · `assistant`**: `as-1` bucle manual de tools con el SDK, registro de llamadas, límite de 5 iteraciones, `LlmGateway` y su fake; `as-2` `log_events` con esquema estricto, una llamada por mensaje, orquestación estimar → registrar; `as-3` prompt dosificado, router de entrada, rutas fijas de síntomas y diagnóstico; `as-4` tools de consulta contra `FakeInsights`, después contra la real.
- **MacBook · C**: cierre de huecos que salgan de `assistant`; set de evaluación de extracción en CI.
- **MacBook · E · `web`**: cablear el chat a la API real (local o por Tailscale); cobertura de sensor y aviso "sin datos nuevos".

### Fase 3 — Integración (ambas máquinas, sin agentes nuevos; 3–5 días)

En el iMac, con datos reales y perfil `prod` (ningún stub permitido): correr las cuatro preguntas del enunciado de punta a punta, revisar los `caveats`, y abrir issues `fix/…` para lo que no encaje. Gracias a los tests de contrato, lo que aparece aquí son sorpresas de datos reales, no de firmas. Se sabe que terminó cuando el humano usa la app a diario desde el teléfono y no abre el ordenador para nada más que el CSV de respaldo.

### Cadencia diaria del humano

- **Mañana (15 min)**: `git pull` en los worktrees y en el clon de prod; leer los comentarios de cierre de los issues; mergear primero las PRs de contrato (ya comentadas por el agente del otro lado); asignar los issues del día.
- **Mediodía**: revisar PRs de módulo con la pasada de `/code-review` ya hecha; las que tengan CI verde y plantilla completa se mergean sin ceremonia.
- **Tarde**: mergear lo que quede, desplegar al iMac desde el clon de prod, comprobar `GET /cgm/status` y una consulta desde el teléfono.

Regla de oro: **una PR de contrato nunca duerme sin mergear**, porque bloquea al otro lado.

---

## 8. Cómo arrancar un agente

Un mensaje inicial por sesión, siempre con la misma forma:

```
Trabajas en el módulo `glycemic` del proyecto Glucurvia, en la carpeta de este worktree, rama `feat/glycemic/grid-coverage`.
Tu tarea es el issue #14 (gly-1). Lee CLAUDE.md, docs/diseno-mvp.md sección 6, el issue, y `gh issue list --label contract --state open` para saber qué contratos están en vuelo.
Restricciones: solo archivos de glucurvia-glycemic; si necesitas cambiar core, abre una rama `contract/...` con la interfaz, el fake, el test de contrato y el stub, y apila tu feature encima.
Termina con `./mvnw -q -pl glucurvia-glycemic -am verify` en verde, abre la PR con la plantilla y comenta en el issue qué hiciste y qué falta.
```

Dos agentes en la misma máquina se distinguen por worktree, rama y base de datos; no comparten carpeta ni `target/`. Lo que sí comparten es `~/.m2`: por eso está prohibido `mvn install` y toda ejecución lleva `-am`, para que Maven construya `core` desde las fuentes del worktree y no tome el de otra rama del repositorio local.

Agentes desatendidos en el iMac: la sesión corre en modo de permisos que no pregunte por cada comando dentro de su worktree, con `caffeinate` para que el iMac no duerma, y el humano revisa en la cadencia diaria. Nunca se les da acceso al clon de prod.

---

## 9. Qué hacer cuando…

| Situación | Acción |
|---|---|
| Dos agentes necesitan el mismo cambio en `core` | Uno abre la PR de contrato; el otro la referencia en su issue y apila su rama sobre la misma `contract/…`. |
| Un cambio de `core` rompe el CI del otro lado | El dueño del cambio adapta al consumidor en una PR `fix/…` inmediata, o se revierte el contrato. Nunca se deja `main` rojo. |
| El fake y la implementación real se comportan distinto | El test de contrato tiene que cubrir ese caso: se añade al `Abstract…Contract` (PR de contrato) y ambos lo pasan. |
| Conflicto de merge en un módulo | No debería ocurrir con un dueño por módulo. Si ocurre, dos issues del mismo módulo se solaparon: el humano los separa mejor. |
| Dos migraciones chocan (misma tabla, dos ramas) | Misma causa. Se mergea una, la otra se reescribe con nueva marca de tiempo. |
| Flyway se queja del checksum en la base local | Se rehizo una migración no mergeada: `docker compose -f docker-compose.dev.yml down -v` y a empezar. Nunca `repair` en el iMac. |
| La app local no arranca por un bean que falta | Falta el stub del adaptador: la PR de contrato estaba incompleta. Se añade el stub en `api`. |
| El agente necesita datos reales | Apunta al Nightscout del iMac por Tailscale con el token de lectura, o restaura el `pg_dump` de anoche en su base de worktree. No arranca un segundo uploader (está bajo el perfil `prod`). |
| El agente quiere "arreglar de paso" algo de otro módulo | No. Abre un issue en ese módulo con lo que vio. |
| El agente necesita una librería nueva | Versión en el `pom.xml` de su módulo. Si otro módulo ya la tiene, se sube al padre por PR de contrato. |
| Una PR supera las 400 líneas | Se parte, salvo que el exceso sea un archivo de datos (semilla del catálogo, fixtures). |
| El humano no tiene tiempo de revisar | Los agentes siguen con el siguiente issue de su módulo; nada de lo suyo depende de mergear hoy salvo los contratos. |
| Se acaban los minutos de GitHub Actions | Runner autohospedado en el iMac (apéndice C): siempre encendido, con Docker, y sin límite de minutos. |

---

## Apéndice A — `CLAUDE.md` propuesto para la raíz

```markdown
# Glucurvia — reglas para agentes

## Qué es
App personal de seguimiento metabólico conversacional (CGM + LLM). Diseño completo en docs/diseno-mvp.md;
plan de trabajo y reparto en docs/plan-trabajo-paralelo.md. Java 21, Spring Boot 3, Maven multi-módulo,
PostgreSQL, React + Vite. Todo en español.

## Tu módulo
Trabajas en UN módulo, el que indica tu issue. No edites archivos de otros módulos ni de lo compartido
(`glucurvia-core`, `glucurvia-core-testing`, carpetas ajenas de `glucurvia-schema`, `docs/api/openapi.yaml`)
salvo en una PR de contrato. Módulos: core, core-testing, schema, cgm, glycemic, insights, journal,
nutrition, assistant, api, web.

## Reglas duras
1. Nunca push a `main`. Rama `feat/<módulo>/<tema>` desde `main` actualizado; PR pequeña (< 400 líneas,
   sin contar archivos de datos); squash.
2. Una PR de contrato (rama `contract/<qué>`, etiqueta `contract`) contiene solo: interfaz o record en
   `core`, fake y test de contrato abstracto en `core-testing`, y stub en `api`. Sin código de feature.
   Se mergea antes que la feature que la usa.
3. Si necesitas algo de otro módulo que no existe: abre tú la PR de contrato con la firma que necesitas y
   apila tu rama de feature sobre `contract/<qué>`. Sigue contra el fake. No implementes el módulo del otro.
4. Migraciones: solo en `glucurvia-schema/src/main/resources/db/migration/<tu módulo>/V<yyyyMMddHHmm>__<desc>.sql`
   y solo sobre tus tablas. Alterar o borrar una columna existente es PR de contrato. Nunca edites una
   migración que ya está en `main`.
5. Eventos entre módulos: publicar records de `core` con `ApplicationEventPublisher`; escuchar solo con
   `@Async @TransactionalEventListener(phase = AFTER_COMMIT)`.
6. Antes de abrir PR: `./mvnw -q spotless:apply verify` en verde. La PR usa la plantilla y enlaza el issue.
7. Nunca `mvn install`. Siempre `-am` al construir un módulo suelto: `./mvnw -q -pl glucurvia-<módulo> -am verify`.
8. Archivos del humano: `pom.xml` padre, `docker-compose*.yml`, `application.yml` de `api`, `.github/`,
   `CLAUDE.md` de la raíz, `docs/` (salvo `openapi.yaml` por contrato). En `.env.example` solo tu bloque.
9. Records para DTOs; sin Lombok; sin Spring, JPA ni I/O en `core`; entidades JPA privadas del módulo;
   `java.time.Clock` inyectado, nunca `Instant.now()` en dominio; sin excepciones tragadas; sin `System.out`.
10. Ningún test llama a la API de Claude ni a ninguna red. `LlmGateway` tiene fake.
11. El LLM (módulo `assistant`) nunca calcula métricas ni decide confianzas: las tools llaman a servicios Java.

## Cómo correr
- Base local de tu worktree: `docker compose -f docker-compose.dev.yml up -d` (usa el `.env` del worktree:
  `COMPOSE_PROJECT_NAME` y `PG_PORT` propios)
- Todo: `./mvnw -q verify` (Testcontainers levanta Postgres solo)
- Un módulo: `./mvnw -q -pl glucurvia-<módulo> -am verify`
- Web: `cd web && npm ci && npm run gen && npm run dev`

## Al terminar
Comenta en el issue: qué hiciste, qué no, qué contratos necesitas del otro lado. Enlaza la PR.
```

## Apéndice B — Plantilla de PR (`.github/pull_request_template.md`)

```markdown
## Qué hace

## Módulo

## Tipo
- [ ] Feature de módulo (no toca `core`, `core-testing`, `openapi.yaml` ni carpetas ajenas de `schema`)
- [ ] Contrato (solo interfaz/record + fake + test de contrato + stub, o solo `openapi.yaml`, o solo migración que altera columna)

## Migraciones añadidas

## Cómo lo probé
- [ ] `./mvnw -q spotless:apply verify` en verde
- [ ] Sin llamadas de red en tests
- [ ] La implementación real (si la hay) extiende el `Abstract…Contract` del adaptador

## Issue
Closes #
```

## Apéndice C — CI (`.github/workflows/ci.yml`)

```yaml
name: ci
on:
  pull_request:
  push:
    branches: [main]
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
env:
  TZ: UTC
jobs:
  backend:
    runs-on: ubuntu-latest        # o [self-hosted, macOS] en el iMac: sin límite de minutos y con Docker
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21', cache: maven }
      - run: ./mvnw -B -q verify   # incluye spotless:check y el test de deriva del OpenAPI (OpenApiDriftTest en api)
  web:
    runs-on: ubuntu-latest
    defaults: { run: { working-directory: web } }
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '22', cache: npm, cache-dependency-path: web/package-lock.json }
      - run: npm ci
      - run: npm run gen && npm run lint && npm run build
      - run: npm test --if-present
  eval-llm:                       # manual: usa el modelo real; nunca en PRs
    if: github.event_name == 'workflow_dispatch'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: echo "set de evaluación de extracción y seguridad con ANTHROPIC_API_KEY del secreto"
```

Protección de `main` en GitHub: *Require a pull request before merging* y *Require status checks to pass* con `backend` y `web`. **Sin** *Require approvals*: con una sola cuenta no puedes aprobar tus propias PRs y bloquearías todo.

Minutos: el plan gratuito de GitHub da 2 000 minutos al mes para repositorios privados; con dos máquinas produciendo PRs se agotan. La salida natural es un runner autohospedado en el iMac, que ya está siempre encendido y tiene Docker. Solo corre PRs de este repositorio privado.

## Apéndice D — Etiquetas de issues

`module:core` `module:schema` `module:cgm` `module:glycemic` `module:insights` `module:journal` `module:nutrition` `module:assistant` `module:api` `module:web` · `machine:imac` `machine:macbook` `machine:human` · `contract` · `blocked` · `phase:1` `phase:2` `phase:3`

Cada issue lleva exactamente una etiqueta de módulo, una de máquina y una de fase. `contract` se añade a las PRs, no a los issues.

## Apéndice E — Esqueleto del `pom.xml` padre (lo esencial)

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>mx.glucurvia</groupId>
  <artifactId>glucurvia</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.x</version>
  </parent>
  <properties><java.version>21</java.version></properties>
  <modules>
    <module>glucurvia-core</module>
    <module>glucurvia-core-testing</module>
    <module>glucurvia-schema</module>
    <module>glucurvia-cgm</module>
    <module>glucurvia-glycemic</module>
    <module>glucurvia-insights</module>
    <module>glucurvia-journal</module>
    <module>glucurvia-nutrition</module>
    <module>glucurvia-assistant</module>
    <module>glucurvia-api</module>
  </modules>
  <dependencyManagement>
    <!-- solo lo que usan dos o más módulos: anthropic-java, testcontainers, flyway, springdoc -->
  </dependencyManagement>
  <build><plugins><!-- spotless con google-java-format; el spring-boot-maven-plugin solo en glucurvia-api --></plugins></build>
</project>
```

Cada módulo declara `glucurvia-core` (compilación) y `glucurvia-core-testing` y `glucurvia-schema` (`scope=test`), y nada más del proyecto. `api` declara todos los módulos, `schema` en compilación y `core-testing` en compilación por los stubs.

## Apéndice F — Registro de cambios

### Versión 0.2 (segunda revisión: dónde se pisaban las dos máquinas)

Contradicciones de la 0.1 que habrían bloqueado el trabajo:

- `api`, `docker-compose` y `application.yml` estaban asignados al agente A y a la vez declarados "solo del humano". Ahora `api` es del humano; los controladores viven en cada módulo; `GET /cgm/readings` pasa a `cgm` y el `pg_dump` al humano.
- La regla 3 del `CLAUDE.md` decía "escribe la firma en tu issue, no la implementes" mientras la sección 4 decía "el consumidor abre la PR de contrato". Ahora el consumidor abre la PR de contrato y apila su feature encima (rama sobre rama, rebase `--onto` tras el squash).
- `core` "sin lógica" chocaba con "las utilidades compartidas van a `core`" y con `Series`. Ahora `core` admite lógica pura (sin Spring, JPA ni I/O).
- Los fakes tenían alcance de test, pero la fase 3 hablaba de "quitar los fakes del cableado" y en la fase 1 la app del iMac no habría arrancado sin `MealEventReader`. Ahora hay stubs por adaptador en `api`, como autoconfiguración (la única forma de que `@ConditionalOnMissingBean` se evalúe después de los `@Service` reales), con informe al arrancar y fallo en `prod`.
- "Cada módulo prueba solo sus migraciones" era imposible: `glycemic_responses → meals → events → users`, `meal_items → food_references`, `events → conversation_messages`. Ahora hay un módulo `schema` con el baseline completo del diseño y carpetas por módulo, y todos los tests aplican el esquema entero.
- `outOfOrder=true` "en desarrollo" ignoraba que el orden de merge también difiere del de las marcas de tiempo en el iMac. Ahora es global, y se explicita el invariante que lo hace seguro (cada módulo toca solo sus tablas; alterar columnas es contrato).
- `conversations` era de `journal` pero la escribe `assistant`; `user_food_aliases` era de `journal` pero la lee `nutrition` en cada estimación; `NutrientEstimator` tenía dos consumidores sin decir quién orquesta. Ahora `assistant` tiene las conversaciones, `nutrition` los alias, y `assistant` orquesta estimar → registrar.
- `insights` no podía cumplir "solo lee sus tablas": consulta agregados de tres módulos. Ahora es una excepción explícita de solo lectura, protegida por la regla de contrato al alterar columnas.
- Los eventos de dominio con listeners síncronos harían que un fallo en `glycemic` (iMac) revirtiera la ingesta de `cgm` o el registro de `journal` (MacBook). Ahora los listeners son asíncronos y tras commit, obligatorio.
- El contrato REST no tenía dueño: `web` simulaba "el contrato del diseño" que implementan controladores en las dos máquinas. Ahora `docs/api/openapi.yaml` es contrato, genera tipos y mocks, y el CI detecta la deriva.
- Dos agentes en la misma máquina compartían el Postgres de desarrollo: Flyway de dos ramas sobre la misma base. Ahora una base por worktree.
- `~/.m2` compartido entre worktrees: `mvn install` o `-pl` sin `-am` mezclaba el `core` de otra rama. Prohibido `install`, obligatorio `-am`.
- Las curvas sintéticas "con resultado esperado" estaban en la fase 0, pero los valores esperados solo pueden salir del algoritmo (gly-2). Ahora la fase 0 crea las entradas y `glycemic` los esperados.
- El compose de producción podía ejecutarse desde el worktree de un agente y desplegar su rama, y un `docker compose up` en la MacBook arrancaba un segundo uploader. Ahora hay un clon dedicado de prod y el uploader va bajo el perfil `prod`.
- Un test de `assistant` contra la API real habría puesto `main` en rojo para el iMac por falta de secreto en CI. Ahora ningún test usa la red; la evaluación con el modelo es un job manual.
- `docs/` y `.env.example` no tenían dueño; las versiones de librerías solo en el padre bloqueaban a un agente que necesitara una. Resuelto (sección 3 y regla 7 de la sección 4).
- La fase 0 pasa de 1–2 a 2–3 días con todo lo anterior.
- El diseño (1.1) decía "un paquete por dominio" y este plan "un módulo Maven por dominio": se actualiza el diseño.

Huecos que no eran contradicciones pero sí choques seguros: formateador único con Spotless y Prettier; toolchain fijado con `.sdkmanrc` y `.nvmrc`; `Clock` inyectado y `TZ=UTC` en CI; tests de contrato que pasan fake y real; recrear la base local ante checksums de Flyway; agentes desatendidos en el iMac; revisar contratos en vuelo al arrancar; excepción de tamaño para archivos de datos; quién sirve la web en producción.

Oportunidades añadidas: runner autohospedado en el iMac; revisión cruzada de PRs de contrato por el agente del otro lado; pasada de `/code-review` antes del humano; `CLAUDE.md` por módulo; informe de stubs al arrancar.
