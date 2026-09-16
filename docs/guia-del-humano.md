# Glucurvia — Guía del humano

Versión 0.2 · 16 de septiembre de 2026 · Tus rutinas y listas de comprobación para llevar el proyecto con dos máquinas y varios agentes. El diseño (`docs/diseno-mvp.md`) dice qué construir; el plan (`docs/plan-trabajo-paralelo.md`) cómo se reparte; esta guía, qué haces tú. Los cambios de esta versión están en la sección 11.

## 0. Tu papel en una frase

Tú no escribes código: decides qué se hace (issues), vigilas que encaje (contratos y PRs), lo integras (merge) y lo pones a funcionar (despliegue en el iMac). Los agentes hacen el resto. Si un día te descubres editando un módulo a mano, es señal de que falta un issue.

Tres cosas que nunca delegas: mergear a `main`, tocar el clon de producción, y decidir un contrato.

---

## 1. Mapa: máquinas, sesiones y dónde corre cada cosa

### 1.1 Las máquinas

| Máquina | Usuario de macOS | Papel | Qué corre ahí |
|---|---|---|---|
| iMac ("iMac de Ascari") | `aromo` | Servidor de producción personal y máquina de desarrollo de los módulos de datos | Clon de producción `~/Desarrollo/glucurvia-prod` con Nightscout, MongoDB, el uploader y Postgres; repositorio de desarrollo `~/Desarrollo/glucurvia`; agentes de `cgm`, `glycemic` e `insights`; la sesión de Claude que hizo la fase 0 y sirve de asistente de revisión |
| MacBook Pro | `ascari` | Máquina de desarrollo de producto | Repositorio `~/Desarrollo/glucurvia`; agentes de `journal`, `nutrition`, `assistant` y `web` |
| Teléfono | — | Uso de la app | Llega a la API y a Nightscout del iMac por Tailscale |

Los comandos de esta guía usan `~/Desarrollo/…` y valen en las dos máquinas aunque el usuario sea distinto. Nada del repositorio depende de la ruta; los dos scripts del iMac aceptan `GLUCURVIA_PROD_DIR` si el clon de producción está en otro sitio.

### 1.2 Cómo funcionan las sesiones de Claude Code (lo que confunde al principio)

- **Una sesión vive en la máquina donde se creó y ejecuta comandos solo allí.** La lista de sesiones se sincroniza entre tus equipos, pero abrir desde la MacBook una sesión del iMac no la "traslada": sigue corriendo en el iMac.
- **"Control remoto desconectado / Claude Code en la computadora que ejecuta esta sesión está sin conexión"** es lo que muestra un equipo al mirar una sesión de otro equipo que no tiene el Control remoto activado (o cuya app está cerrada). No es un error. Con el Control remoto activado (interruptor de la barra superior de la sesión, o pidiéndoselo a la propia sesión), puedes seguirla y escribirle desde el otro Mac o desde el teléfono; sus comandos siguen corriendo en su máquina.
- **Para que un agente trabaje en una máquina, la sesión se crea en esa máquina**, con la app de Claude abierta sobre la carpeta de su worktree. Una sesión nueva no sabe nada de las conversaciones anteriores y no lo necesita: lee `CLAUDE.md` del repositorio y el issue.
- Regla práctica: **una sesión por worktree**, y el Control remoto solo en las que quieras vigilar desde fuera.

---

## 2. Preparación de una máquina, desde cero

Hecho ya en las dos máquinas. Queda como receta para una tercera o para reinstalar. Todo en orden; los pasos 1 y 4 piden tu contraseña de administrador y no los puede hacer un agente.

1. Herramientas base:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)" && brew install git gh && brew install --cask docker tailscale
```

2. SDKMAN (instalará el JDK y el Maven exactos que fija `.sdkmanrc`):

```bash
curl -s "https://get.sdkman.io" | bash
```

3. El repositorio:

```bash
git clone https://github.com/windoctor7/glucurvia.git ~/Desarrollo/glucurvia
```

4. Sesión de GitHub para `gh` (abre el navegador; cuenta `windoctor7`):

```bash
gh auth login --hostname github.com --git-protocol https --web
```

5. Abre Docker Desktop y espera a que la barra inferior diga **"Engine running"** (acepta el acuerdo y los permisos del sistema si los pide; el primer arranque tarda). Abre Tailscale e inicia sesión. Comprueba el motor:

```bash
docker info --format 'motor {{.ServerVersion}}' && docker run --rm hello-world | head -2
```

6. JDK, Maven y la primera compilación completa:

```bash
cd ~/Desarrollo/glucurvia && source ~/.sdkman/bin/sdkman-init.sh && sdk env install && ./mvnw -q verify
```

Si termina sin errores, la máquina está lista (hoy son 43 pruebas; el número crecerá).

### 2.1 Tropiezos conocidos

| Síntoma | Causa | Qué hacer |
|---|---|---|
| Testcontainers: "Could not find a valid Docker environment" con Docker abierto | Casi siempre el motor aún no está en "running". Antes también lo causaba Docker 29, que exige la API 1.44 y rechazaba la que pedía el cliente Java | Espera al motor y repite. La versión de API ya está fijada en el repositorio (`glucurvia-core-testing/src/main/resources/docker-java.properties`, PR #17): no hace falta ningún archivo local. Si aun así falla, las pruebas de `core-testing` ahora imprimen qué estrategias probó Testcontainers y por qué fallaron; pega esa salida a un agente |
| `gh: command not found` | `gh` no viene con macOS | `brew install gh` y el paso 4 |
| `brew install --cask …` falla con "sudo: a password is required" | Docker y Tailscale instalan componentes del sistema | Ejecútalo tú en tu terminal |
| `./mvnw` compila pero las pruebas tardan minutos la primera vez | Descarga de imágenes (`postgres:17-alpine`, Ryuk) | Normal; solo la primera vez |
| Al arrancar la app: "Adaptadores en stub" en el log | Faltan implementaciones reales; es el estado esperado hasta que los módulos lleguen a `main` | Nada; con perfil `prod` la app se niega a arrancar, y eso es correcto |

### 2.2 Solo en el iMac (servidor)

- [x] Reposo desactivado (`pmset -g` muestra `sleep 0`).
- [ ] Docker Desktop → Settings → General → "Start Docker Desktop when you sign in".
- [x] Clon de producción `~/Desarrollo/glucurvia-prod`, separado de cualquier worktree de agente.
- [x] `.env` de producción: `NS_API_SECRET` y `POSTGRES_PASSWORD` generados aleatoriamente, `COMPOSE_PROJECT_NAME=glucurvia-prod` (el ejemplo trae `glucurvia-dev`, que es para los worktrees).
- [x] `mongo`, `nightscout` y `postgres` arriba (Nightscout 15.0.8, mg/dL, acceso denegado sin token).
- [ ] Tokens de Nightscout y cuenta seguidora de LibreLinkUp en el `.env`; uploader arrancado (sección 7.1).
- [ ] Copia nocturna programada y restauración probada (sección 7.3).
- [ ] Runner autohospedado de GitHub Actions cuando se acaben los minutos gratuitos (plan, apéndice C).

---

## 3. Estado de la fase 0 (hecha)

La fase 0 se construyó el 13 de septiembre en el iMac, en una sesión con el humano presente, y entró en `main` por la PR #1 con el CI en verde. Lo que hay:

| Qué | Dónde |
|---|---|
| Diez módulos Maven compilando; `api` arranca contra Postgres y responde `UP` | raíz del repositorio |
| Todos los contratos de `core`, sus fakes, tests de contrato y curvas sintéticas | `glucurvia-core`, `glucurvia-core-testing` |
| Baseline del esquema con las trece tablas del diseño y el usuario sembrado | `glucurvia-schema/…/db/migration/core/V202609130000__baseline.sql` |
| Stubs por adaptador con informe al arrancar y rechazo en `prod` | `glucurvia-api/…/stubs` |
| Contrato REST (19 endpoints) y test de deriva | `docs/api/openapi.yaml`, `OpenApiDriftTest` |
| Web con tipos generados, mocks, lint, build y pruebas | `web/` |
| `CLAUDE.md`, plantilla de PR, CI, scripts, compose de desarrollo por worktree | raíz, `.github/`, `scripts/` |
| `main` protegida (PR obligatoria, checks `backend` y `web`), etiquetas, 15 issues de la fase 1 (#2 a #16), etiqueta `skeleton` | GitHub |
| Mantenimiento tras el primer arranque en la MacBook (API de Docker 1.44, logs de Testcontainers, `tsbuildinfo` fuera) | PR #17 |

Cómo se verifica que una máquina tiene todo esto bien: `./mvnw -q verify` en verde y, en la app, el log de arranque listando los siete adaptadores en stub.

---

## 4. Cómo arrancas un agente

Cada vez, en este orden, **en la máquina donde debe trabajar ese agente** (sección 1.1). Tarda cinco minutos.

1. **Elige el issue.** `gh issue list --label machine:imac --label phase:1 --state open` (o `machine:macbook`). Uno por agente, del módulo que le toca.
2. **Crea su worktree** desde `main` actualizado, con la rama del issue. Un worktree es una segunda carpeta del mismo repositorio, al lado de la principal, con su propia rama abierta; dos agentes nunca comparten carpeta:

```bash
cd ~/Desarrollo/glucurvia && git pull --ff-only && git worktree add -b feat/journal/eventos-y-comidas ../glucurvia-journal main
```

3. **Dale su base de datos** (solo módulos Java). En la carpeta nueva, un `.env` con `COMPOSE_PROJECT_NAME=glucurvia-<módulo>` y un `PG_PORT` distinto por worktree (5433, 5434, 5435…):

```bash
printf 'COMPOSE_PROJECT_NAME=glucurvia-journal\nPG_PORT=5433\n' > ~/Desarrollo/glucurvia-journal/.env && cd ~/Desarrollo/glucurvia-journal && docker compose -f docker-compose.dev.yml up -d
```

4. **Abre en la app de Claude de esa máquina una sesión nueva sobre esa carpeta** (no reutilices una sesión existente ni la de otra máquina). Elige el modo de permisos que acepte ediciones y comandos dentro del worktree sin preguntar por cada uno (en la CLI, `claude --permission-mode acceptEdits`). Si el agente va a quedarse solo en el iMac, es imprescindible. Si quieres vigilarlo desde el otro Mac o el teléfono, activa el Control remoto de esa sesión.
5. **Pega el mensaje de arranque** (apéndice A), con el módulo, la rama y el número de issue. Los cuatro primeros ya están escritos.
6. **Déjalo trabajar.** Un issue bien escrito termina en una PR abierta y un comentario en el issue. Si a la hora no ha avanzado, párale, lee lo que escribió, y reescribe el issue más pequeño.
7. **Cuando la PR esté mergeada**, limpia desde la carpeta principal:

```bash
cd ~/Desarrollo/glucurvia && git worktree remove ../glucurvia-journal && git branch -d feat/journal/eventos-y-comidas
```

Cuántos a la vez: dos por máquina en la fase 1, tres en la fase 2. Si tienes más de cinco PRs abiertas sin revisar, no arranques ninguno más.

Los cuatro primeros: en el iMac, `cgm-1` (#2, carpeta `glucurvia-cgm`, puerto 5433) y `gly-1` (#6, `glucurvia-glycemic`, 5434); en la MacBook, `jr-1` (#9, `glucurvia-journal`, 5433) y `web-1` (#13, `glucurvia-web`, sin base de datos).

---

## 5. Tu día

### Mañana (15 minutos, desde cualquier máquina)

- [ ] `git pull --ff-only` en el repo principal de esa máquina.
- [ ] `gh pr list --label contract`: las PRs de contrato van primero. Revisa cada una con la lista de 5.3 y mergéala. **Una PR de contrato no duerme sin mergear.**
- [ ] `gh issue list --state open --search "sort:updated-desc"`: lee los comentarios de cierre de ayer. Cada "necesito que el otro lado…" se convierte en un issue nuevo o en una PR de contrato hoy.
- [ ] Asigna los issues del día: uno por agente activo.

### Mediodía (30 minutos)

- [ ] `gh pr list`: para cada PR de módulo, `gh pr checks <n>` en verde y la lista de 5.2. Mergea con `gh pr merge <n> --squash --delete-branch`.
- [ ] Si una PR no pasa la lista, comenta en ella qué falta (`gh pr comment <n> --body "..."`) y díselo al agente en su sesión.

### Tarde (15 minutos, en el iMac)

- [ ] Mergea lo que quede verde.
- [ ] Despliega con `scripts/deploy-imac.sh` y comprueba `GET /cgm/status` y una pregunta desde el teléfono.
- [ ] Si hubo cambios en `core` o en `schema`, etiqueta: `git tag v0.1.<n> && git push --tags`. Es tu punto de retorno.

### 5.2 Lista para revisar una PR de módulo (cinco minutos)

- [ ] La plantilla está completa y enlaza un issue.
- [ ] CI verde (`gh pr checks <n>`).
- [ ] `gh pr diff <n> --name-only`: **solo archivos de su módulo** (más su carpeta en `schema` si añade migración). Si toca `core`, `core-testing`, `openapi.yaml`, `api` o carpetas ajenas, se rechaza: eso va en una PR de contrato aparte.
- [ ] Menos de 400 líneas sin contar archivos de datos. Si no, pide que la parta.
- [ ] Hay tests nuevos para lo nuevo; ninguno usa la red.
- [ ] Si implementa un adaptador, extiende el `Abstract…Contract`.
- [ ] Pasada de `/code-review` hecha (tú o un agente) y sin hallazgos graves sin responder.

Lo que no revisas: estilo (lo hace Spotless), nombres de variables, si tú lo habrías hecho distinto. Si funciona, está probado y respeta las fronteras, entra.

### 5.3 Lista para revisar una PR de contrato (diez minutos)

- [ ] Contiene solo interfaz o record en `core`, fake y `Abstract…Contract` en `core-testing`, y stub en `api`. Cero lógica de negocio.
- [ ] La firma es implementable por el dueño con los datos que tiene, y consumible por quien la pide. Si dudas, pide al agente del módulo dueño que la comente: abre su sesión y dile "revisa la PR #n y comenta si puedes implementar esa firma".
- [ ] Es aditiva. Si quita o renombra algo, exige las dos PRs de adaptación el mismo día.
- [ ] El test de contrato cubre los casos límite que definirán el comportamiento (sin datos, n pequeño, valores nulos).

### 5.4 Lista para escribir un issue

Un agente trabaja bien con un issue que tenga estas cinco cosas; sin alguna, se inventa el resto. Los 15 de la fase 1 siguen este formato y sus borradores están en `docs/issues/fase-1/`.

```
Título: cgm-1 · NightscoutAdapter y sondeo cada minuto
Módulo: glucurvia-cgm · Máquina: iMac · Fase: 1
Contexto: diseño 4.4 y 4.3; plan 7 (fase 1, agente A).
Contratos que usa: GlucoseSeriesReader (implementa), ReadingsIngested (publica).
Alcance: solo glucurvia-cgm y glucurvia-schema/db/migration/cgm. Sin tocar core.
Terminado cuando:
- lee entries desde last_reading_ts y guarda sgv como REALTIME y mbg como STRIP
- POST /cgm/sync y GET /cgm/status responden como dice openapi.yaml
- tres fallos seguidos quedan reflejados en cgm_pull_state
- tests con Testcontainers y con un servidor Nightscout simulado; ninguno usa la red
- ./mvnw -q -pl glucurvia-cgm -am verify en verde
```

Para crear los de una fase nueva: borradores en `docs/issues/<fase>/` y `scripts/create-issues.sh <fase>`.

---

## 6. Cómo respondes a lo que te piden los agentes

| El agente dice… | Tú haces |
|---|---|
| "Necesito X de `core` que no existe" | Le contestas: "abre la PR de contrato con la firma, el fake, el test y el stub, y apila tu rama encima". No la escribes tú. |
| "Necesito que `journal` devuelva también Y" | Es un contrato: PR de contrato aditiva por parte del que pide; issue para el dueño de `journal` con la implementación. |
| "El test de otro módulo falla en mi PR" | Si es por su cambio en `core`, adapta él en la misma tanda. Si no, `main` estaba roto: revierte el último merge y abre un `fix/…`. |
| "¿Puedo tocar `application.yml`?" | No. Que lea la configuración con `@ConfigurationProperties(prefix="glucurvia.<módulo>")` y valores por defecto en código, y añada su bloque a `.env.example`. |
| "Me hace falta una librería" | Que la declare en el `pom.xml` de su módulo. Si otro módulo ya la usa, la subes tú al padre. |
| "Necesito datos reales" | Que apunte `GLUCURVIA_CGM_NIGHTSCOUT_URL` al iMac por Tailscale con el token de lectura, o restaura tú el volcado de anoche en su base de worktree. |
| "Flyway se queja del checksum" | `docker compose -f docker-compose.dev.yml down -v` en su worktree y a empezar. Nunca `repair` en el iMac. |
| "Testcontainers no ve Docker" | Que mire que el motor de Docker Desktop diga "running" y pegue la salida de las pruebas de `core-testing`, que ahora explica qué probó (sección 2.1). |
| "¿Puedo arreglar de paso algo de `nutrition`?" | No. Que abra un issue en `nutrition` con lo que vio. |
| Lleva una hora dando vueltas | Párale. Léelo. Reescribe el issue más pequeño o con el dato que le faltaba. Reinicia la sesión con el mensaje de arranque. |

---

## 7. Producción en el iMac

### 7.1 Estado y lo que falta

Levantado: Nightscout, MongoDB y el Postgres de producción, bajo el nombre de proyecto `glucurvia-prod`, con secretos generados en `~/Desarrollo/glucurvia-prod/.env`. Falta lo que solo tú puedes hacer, en este orden:

1. Abre http://localhost:1337 en el iMac y autentícate con el valor de `NS_API_SECRET` del `.env`.
2. Menú → Admin Tools → Subjects: un sujeto con rol `readable` (su token a `NS_READ_TOKEN`, para nuestra API) y otro con rol `api` (su token a `NIGHTSCOUT_API_TOKEN`, para el uploader).
3. En FreeStyle LibreLink: Aplicaciones conectadas → LibreLinkUp → invita a una segunda cuenta de correo tuya; acepta la invitación en la app LibreLinkUp. Correo y contraseña a `LINK_UP_USERNAME` y `LINK_UP_PASSWORD`.
4. Arranca el uploader y mira sus logs hasta ver lecturas:

```bash
cd ~/Desarrollo/glucurvia-prod && docker compose --profile prod up -d librelink-up && docker compose logs -f librelink-up
```

Si se queja de región o de versión de la app, ajusta `LINK_UP_REGION` o `LINK_UP_VERSION` en el `.env` y reinicia el servicio.

5. Desde el teléfono, por Tailscale, abre `http://<nombre-del-imac-en-tailscale>:1337` y comprueba que ves tu glucosa. A partir de aquí usa Nightscout unos días: es el "día 1" del diseño.

### 7.2 Desplegar la API (cuando exista, fase 1 en adelante)

Desde el clon de producción, `scripts/deploy-imac.sh`: `git pull`, `docker compose --profile prod up -d --build` y comprobación de `GET /cgm/status`. Si `api` no arranca por un stub activo en `prod`, es que se mergeó un contrato sin su implementación: no es un fallo del despliegue, es una PR pendiente en el otro lado. Vuelve atrás (7.4) y crea el issue.

### 7.3 Copias de seguridad

`scripts/backup-imac.sh` vuelca Postgres comprimido a `~/Backups/glucurvia/` y borra los de más de 60 días. Prográmalo a las 03:00 con `launchd` o `cron` y **prueba una restauración una vez al mes** en la base de un worktree: una copia que nunca se ha restaurado no es una copia. Time Machine no respalda bien los volúmenes de Docker; por eso el volcado va a una carpeta normal. La MongoDB de Nightscout no necesita volcado: todo lo que ha visto está en Postgres al minuto (cuando `cgm` esté en marcha).

### 7.4 Volver atrás

```bash
cd ~/Desarrollo/glucurvia-prod && git checkout v0.1.<n-1> && docker compose --profile prod up -d --build
```

Las migraciones ya aplicadas no se deshacen: por eso los cambios de esquema son aditivos y alterar columnas es contrato. Cuando `main` esté arreglado, `git checkout main` y despliega de nuevo.

### 7.5 Lo que nunca haces en el clon de producción

Abrir agentes, cambiar de rama, editar archivos, `flyway repair`, `docker compose down -v`.

---

## 8. Cómo sabes que vas bien

Cada viernes, cinco números:

| Número | Cómo lo sacas | Qué quieres ver |
|---|---|---|
| PRs mergeadas en la semana | `gh pr list --state merged --search "merged:>=$(date -v-7d +%F)"` | entre 8 y 20; menos, los agentes se atascan; más, tú no estás revisando de verdad |
| PRs abiertas más de dos días | `gh pr list --search "created:<$(date -v-2d +%F)"` | cero; una PR vieja es un agente bloqueado |
| Contratos abiertos | `gh pr list --label contract` | cero al terminar el día |
| Adaptadores en stub en producción | el log de arranque del iMac | baja cada semana hasta cero al final de la fase 2 |
| `main` roto | `gh run list --branch main --limit 10` | ningún rojo; si lo hay, se revierte el mismo día |

Y las salidas de fase, tal cual las define el plan: fase 1 termina cuando el iMac recibe lecturas al minuto e importa CSV sin duplicar, y en la MacBook una comida escrita en el chat simulado aparece con rangos; fase 2 cuando las cuatro preguntas del enunciado responden contra fakes en la MacBook y `insights` real pasa su contrato en el iMac; fase 3 cuando usas la app a diario desde el teléfono y solo abres el ordenador para el CSV de respaldo.

---

## 9. Costes y límites que conviene vigilar

- **Claude.** Los agentes de módulo trabajan bien con el esfuerzo por defecto; el esfuerzo máximo resérvalo para revisiones de diseño y contratos, que es donde una omisión cuesta días. Un agente que da vueltas quema más que uno que se para y pregunta: por eso el mensaje de arranque le dice que se detenga si necesita un contrato.
- **API de Anthropic** (la del producto, no la de Claude Code): límite de gasto mensual en la consola del proveedor desde el primer día. Ningún test la llama; si ves consumo un día sin haber usado la app, algo la está llamando y hay que mirarlo.
- **GitHub Actions**: 2 000 minutos al mes en el plan gratuito; cada ejecución de este proyecto ronda los 4–6 minutos. La página de facturación de GitHub te dice cuánto llevas. Cuando ronde el 70 %, runner autohospedado en el iMac.
- **Disco del iMac**: los volúmenes de Docker crecen (Mongo, Postgres, imágenes viejas). `docker system prune -f` una vez al mes, nunca con `-a --volumes` en producción.

---

## 10. Seguridad mínima (la que sí aplica a uso personal)

- Contraseña única o Tailscale delante de la API y de Nightscout; ningún puerto abierto a internet. Tailscale ya está en las dos máquinas; comprueba desde datos móviles, sin Tailscale, que `http://<ip-pública>:1337` no responde.
- Secretos solo en los `.env` (ignorados por Git) y en la consola del proveedor. Nunca pegues una clave en el chat de un agente: si la necesita, está en el `.env` de su worktree. Las instalaciones y los inicios de sesión que piden tu contraseña los haces tú.
- La cuenta seguidora de LibreLinkUp tiene su propia contraseña, distinta de la de tu cuenta principal de Abbott.
- Copia de seguridad probada (7.3). Es el único riesgo irreversible del proyecto.

---

## 11. Registro de cambios

### Versión 0.2 (16 de septiembre de 2026)

Lo que la 0.1 no contaba y hubo que descubrir al arrancar la segunda máquina:

- Nueva sección 1: qué máquina es cuál (con sus usuarios de macOS), que cada sesión de Claude Code vive y ejecuta en la máquina donde se creó, qué significa "Control remoto desconectado", y que los agentes de una máquina necesitan sesiones creadas allí sobre su worktree.
- Sección 2 reescrita como receta completa desde cero, con los comandos exactos en orden y una tabla de tropiezos: Docker 29 y Testcontainers (resuelto en el repositorio por la PR #17), `gh` no viene con macOS, las instalaciones que piden contraseña, la primera descarga de imágenes, el log "Adaptadores en stub".
- Sección 3: la fase 0 pasa de lista de tareas a inventario de lo que ya está en `main` y en GitHub.
- Sección 4: qué es un worktree, la sesión se abre en la máquina del agente, Control remoto opcional por sesión, y los cuatro primeros agentes con carpeta y puerto.
- Sección 7: estado real de producción en el iMac (qué está levantado, qué falta y en qué orden) y `COMPOSE_PROJECT_NAME=glucurvia-prod` para no chocar con los worktrees.
- Fila nueva en la tabla de respuestas a los agentes (Testcontainers) y referencias a `scripts/create-issues.sh` y `scripts/backup-imac.sh`.

---

## Apéndice A — Mensajes de arranque

Cambia solo lo que va entre `<>`. El resto es fijo a propósito: los agentes rinden mejor con instrucciones idénticas.

**Módulo de backend (cgm, glycemic, insights, journal, nutrition, assistant)**

```
Trabajas en el módulo `glucurvia-<módulo>` del proyecto Glucurvia, en la carpeta de este worktree, rama `feat/<módulo>/<tema>`.
Tu tarea es el issue #<n>. Lee CLAUDE.md, las secciones del diseño que cita el issue, el issue completo, y `gh issue list --label contract --state open` para conocer los contratos en vuelo.
Restricciones: solo archivos de glucurvia-<módulo> y de glucurvia-schema/src/main/resources/db/migration/<módulo>. Si necesitas cambiar core, core-testing, openapi.yaml o api, detente: abre una rama `contract/<qué>` con la interfaz, el fake, el test de contrato y el stub, abre su PR etiquetada `contract`, apila tu rama de feature encima y sigue contra el fake.
Ningún test usa la red. Inyecta `Clock`; no uses `Instant.now()` en dominio.
Termina con `./mvnw -q -pl glucurvia-<módulo> -am spotless:apply verify` en verde, abre la PR con la plantilla (`gh pr create`), y comenta en el issue qué hiciste, qué no y qué contratos necesitas del otro lado.
```

**Módulo web**

```
Trabajas en `web/` del proyecto Glucurvia, en la carpeta de este worktree, rama `feat/web/<tema>`.
Tu tarea es el issue #<n>. Lee CLAUDE.md, la sección 1.4 del diseño, el issue completo y docs/api/openapi.yaml, que es el contrato: los tipos y los mocks se generan de ahí con `npm run gen`; no inventes campos.
Restricciones: solo archivos de web/. Si el contrato REST no te da algo que necesitas, detente y abre una PR de contrato que cambie solo openapi.yaml.
Móvil primero (390 px), instalable, sin service worker. Termina con `npm run gen && npm run lint && npm run format:check && npm run build && npm test` en verde, abre la PR con la plantilla y comenta en el issue.
```

**Los cuatro primeros, ya rellenados**

| Agente | Máquina | Módulo y rama | Issue | Secciones del diseño |
|---|---|---|---|---|
| A | iMac | `glucurvia-cgm`, `feat/cgm/nightscout-adapter` | #2 | 4.3, 4.4 |
| B | iMac | `glucurvia-glycemic`, `feat/glycemic/serie-rejilla-cobertura` | #6 | 6.1, 6.2 |
| C | MacBook | `glucurvia-journal`, `feat/journal/eventos-y-comidas` | #9 | 5, 3 |
| E | MacBook | `web/`, `feat/web/chat-movil` | #13 | 1.4, 8.1 |

**Revisión de una PR de contrato por el agente dueño**

```
Lee la PR #<n> (`gh pr view <n> --diff`). Es una PR de contrato que tu módulo `glucurvia-<módulo>` tendrá que implementar. Comenta en la PR (`gh pr comment <n>`) si puedes implementar esa firma con los datos que tiene tu módulo, y qué cambiarías. No edites nada.
```

## Apéndice B — Comandos que usarás a diario

```bash
gh pr list                                   # qué hay abierto
gh pr checks <n>                             # CI de una PR
gh pr diff <n> --name-only                   # qué archivos toca (¿solo su módulo?)
gh pr view <n> --web                         # leerla en el navegador
gh pr merge <n> --squash --delete-branch     # integrar
gh pr comment <n> --body "..."               # pedir un cambio
gh issue list --label phase:1 --state open   # tablero
scripts/create-issues.sh fase-2              # crear los issues de una fase desde docs/issues/fase-2/
gh run list --branch main --limit 10         # ¿main verde?
git worktree list                            # qué agentes tienen carpeta
git worktree remove ../glucurvia-<módulo>    # limpiar al mergear
git tag v0.1.<n> && git push --tags          # punto de retorno tras cambios de core o schema
docker info --format '{{.ServerVersion}}'    # ¿el motor de Docker está arriba?
docker compose -f docker-compose.dev.yml down -v   # recrear la base de un worktree
```

## Apéndice C — La sesión del iMac

La sesión de Claude que construyó la fase 0 sigue abierta en el iMac con el Control remoto activado: puedes hablar con ella desde la MacBook o el teléfono para revisar PRs, redactar contratos o issues, o pedirle que arranque los agentes del iMac. Sus comandos corren en el iMac. No la uses como agente de módulo: eso es una sesión nueva por worktree.
