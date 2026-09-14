# Glucurvia — Guía del humano

Versión 0.1 · 13 de septiembre de 2026 · Tus rutinas y listas de comprobación para llevar el proyecto con dos máquinas y varios agentes. El diseño (`docs/diseno-mvp.md`) dice qué construir; el plan (`docs/plan-trabajo-paralelo.md`) cómo se reparte; esta guía, qué haces tú cada día.

## 0. Tu papel en una frase

Tú no escribes código: decides qué se hace (issues), vigilas que encaje (contratos y PRs), lo integras (merge) y lo pones a funcionar (despliegue en el iMac). Los agentes hacen el resto. Si un día te descubres editando un módulo a mano, es señal de que falta un issue.

Tres cosas que nunca delegas: mergear a `main`, tocar el clon de producción, y decidir un contrato.

---

## 1. Preparación única

### 1.1 En las dos máquinas

Marca cada punto en cada máquina.

- [ ] Git, y `gh auth login` con tu cuenta (`windoctor7`). Comprueba con `gh repo view windoctor7/glucurvia`.
- [ ] Docker: OrbStack (más ligero) o Docker Desktop. Comprueba con `docker run --rm hello-world`.
- [ ] Tailscale instalado y con sesión iniciada; anota el nombre del iMac en la red (`tailscale status`).
- [ ] JDK 21 con SDKMAN (`sdk env install` dentro del repo lee `.sdkmanrc`) y Node 22 con nvm (`nvm use` lee `.nvmrc`).
- [ ] Claude Code, la app de escritorio.
- [ ] El repositorio en `~/Desarrollo/glucurvia`, y `./mvnw -q verify` en verde (después de la fase 0).

### 1.2 Solo en el iMac (servidor)

- [ ] Que no duerma: Ajustes → Batería/Energía → impedir reposo con la pantalla apagada, o `caffeinate -dims &` en un terminal que dejes abierto.
- [ ] Docker arrancando al iniciar sesión.
- [ ] Clon de producción, separado de cualquier worktree de agente: `git clone https://github.com/windoctor7/glucurvia.git ~/Desarrollo/glucurvia-prod`.
- [ ] `.env` de producción en ese clon, copiado de `.env.example` y rellenado (Nightscout, cuenta seguidora de LibreLinkUp, Postgres, clave de la API).
- [ ] Stack de Nightscout arriba siguiendo el "día 1" del README; comprobado que ves tu glucosa en `http://<imac>:1337` desde el teléfono por Tailscale.
- [ ] Carpeta de copias `~/Backups/glucurvia/` y el volcado nocturno programado (sección 6.3).
- [ ] Runner autohospedado de GitHub Actions cuando se acaben los minutos gratuitos (apéndice C del plan); no hace falta el primer día.

---

## 2. Semana 0: la fase 0 contigo al lado

Se hace en una sola máquina, con una sola sesión de Claude Code y tú presente. Pide cada bloque por separado, comprueba lo que se indica y solo entonces pide el siguiente. No abras agentes en paralelo hasta la etiqueta `skeleton`.

| # | Bloque | Cómo compruebas que está |
|---|---|---|
| 1 | `pom.xml` padre y los diez módulos vacíos; `api` arranca | `./mvnw -q verify` verde; `./mvnw -q -pl glucurvia-api spring-boot:run` y `curl localhost:8080/actuator/health` responde `UP` |
| 2 | `core` con todos los contratos de la tabla 4.1 del plan; `core-testing` con fakes, tests de contrato y curvas de entrada | Abres `glucurvia-core` y encuentras cada adaptador de la tabla; `./mvnw -q -pl glucurvia-core-testing -am verify` verde |
| 3 | `schema` con el baseline del diseño y `users` sembrado; Flyway con `outOfOrder` | Arrancas la app contra la base de desarrollo y `psql` muestra todas las tablas de la sección 3 del diseño |
| 4 | Stubs en `api` | En el log de arranque aparece la lista de adaptadores en stub (todos, por ahora); con `SPRING_PROFILES_ACTIVE=prod` la app se niega a arrancar |
| 5 | `docs/api/openapi.yaml` y generación de tipos en `web` | `cd web && npm run gen` produce los tipos; el CI tiene el paso de deriva |
| 6 | `docker-compose.dev.yml` por worktree; uploader bajo perfil `prod` | Dos carpetas con `.env` distintos levantan dos Postgres a la vez; `docker compose up -d` sin `--profile prod` no arranca el uploader |
| 7 | `.sdkmanrc`, `.nvmrc`, `.editorconfig`, Spotless, Prettier | `./mvnw -q spotless:check` verde; `cd web && npm run lint` verde |
| 8 | CI y protección de `main` | Una PR de prueba muestra los checks `backend` y `web`; un push directo a `main` es rechazado |
| 9 | `CLAUDE.md`, plantilla de PR, etiquetas | `gh label list` muestra las del apéndice D del plan |
| 10 | Issues de la fase 1 | `gh issue list --label phase:1` muestra uno por tarea, cada uno con módulo, máquina, contratos y criterio de terminado |
| 11 | Etiqueta `skeleton` | `git tag` la muestra; la otra máquina hace pull y `./mvnw -q verify` está verde allí también |

Protección de `main` con `gh` (ejecútalo una vez, después de que el CI haya corrido al menos una vez para que existan los nombres de los checks):

```bash
gh api -X PUT repos/windoctor7/glucurvia/branches/main/protection --input - <<'JSON'
{"required_status_checks":{"strict":true,"contexts":["backend","web"]},"enforce_admins":false,"required_pull_request_reviews":null,"restrictions":null,"allow_force_pushes":false,"allow_deletions":false}
JSON
```

Sin `required_pull_request_reviews`: con una sola cuenta no puedes aprobar tus propias PRs.

---

## 3. Cómo arrancas un agente

Cada vez, en este orden. Tarda cinco minutos.

1. **Elige el issue.** `gh issue list --label machine:imac --label phase:1 --state open` (o `machine:macbook`). Uno por agente, del módulo que le toca.
2. **Crea su worktree** desde `main` actualizado, con la rama del issue:

```bash
cd ~/Desarrollo/glucurvia && git pull --ff-only && git worktree add -b feat/cgm/nightscout-adapter ../glucurvia-cgm main
```

3. **Dale su base de datos.** En la carpeta nueva, un `.env` con dos líneas: `COMPOSE_PROJECT_NAME=glucurvia-cgm` y `PG_PORT=5433` (un puerto distinto por worktree: 5433, 5434, 5435…). Luego `docker compose -f docker-compose.dev.yml up -d`.
4. **Abre Claude Code en esa carpeta** y elige el modo de permisos que acepte ediciones y comandos dentro del worktree sin preguntar por cada uno (en la CLI, `claude --permission-mode acceptEdits`). Si el agente va a quedarse solo en el iMac, es imprescindible; si estás delante, puedes dejar el modo normal.
5. **Pega el mensaje de arranque** (apéndice A de esta guía), con el módulo, la rama y el número de issue.
6. **Déjalo trabajar.** Un issue bien escrito termina en una PR abierta y un comentario en el issue. Si a la hora no ha avanzado, párale, lee lo que escribió, y reescribe el issue más pequeño.
7. **Cuando termine**, no cierres el worktree hasta que la PR esté mergeada; después:

```bash
cd ~/Desarrollo/glucurvia && git worktree remove ../glucurvia-cgm && git branch -d feat/cgm/nightscout-adapter
```

Cuántos a la vez: dos por máquina en la fase 1, tres en la fase 2. Si tienes más de cinco PRs abiertas sin revisar, no arranques ninguno más.

---

## 4. Tu día

### Mañana (15 minutos, desde cualquier máquina)

- [ ] `git pull --ff-only` en el repo principal de esa máquina.
- [ ] `gh pr list --label contract`: las PRs de contrato van primero. Revisa cada una con la lista de 4.3 y mergéala. **Una PR de contrato no duerme sin mergear.**
- [ ] `gh issue list --state open --search "sort:updated-desc"`: lee los comentarios de cierre de ayer. Cada "necesito que el otro lado…" se convierte en un issue nuevo o en una PR de contrato hoy.
- [ ] Asigna los issues del día: uno por agente activo.

### Mediodía (30 minutos)

- [ ] `gh pr list`: para cada PR de módulo, `gh pr checks <n>` en verde y la lista de 4.2. Mergea con `gh pr merge <n> --squash --delete-branch`.
- [ ] Si una PR no pasa la lista, comenta en ella qué falta (`gh pr comment <n> --body "..."`) y díselo al agente en su sesión.

### Tarde (15 minutos, en el iMac)

- [ ] Mergea lo que quede verde.
- [ ] Despliega con el script de 6.1 y comprueba `GET /cgm/status` y una pregunta desde el teléfono.
- [ ] Si hubo cambios en `core` o en `schema`, etiqueta: `git tag v0.1.<n> && git push --tags`. Es tu punto de retorno.

### 4.2 Lista para revisar una PR de módulo (cinco minutos)

- [ ] La plantilla está completa y enlaza un issue.
- [ ] CI verde (`gh pr checks <n>`).
- [ ] `gh pr diff <n> --name-only`: **solo archivos de su módulo** (más su carpeta en `schema` si añade migración). Si toca `core`, `core-testing`, `openapi.yaml`, `api` o carpetas ajenas, se rechaza: eso va en una PR de contrato aparte.
- [ ] Menos de 400 líneas sin contar archivos de datos. Si no, pide que la parta.
- [ ] Hay tests nuevos para lo nuevo; ninguno usa la red.
- [ ] Si implementa un adaptador, extiende el `Abstract…Contract`.
- [ ] Pasada de `/code-review` hecha (tú o un agente) y sin hallazgos graves sin responder.

Lo que no revisas: estilo (lo hace Spotless), nombres de variables, si tú lo habrías hecho distinto. Si funciona, está probado y respeta las fronteras, entra.

### 4.3 Lista para revisar una PR de contrato (diez minutos)

- [ ] Contiene solo interfaz o record en `core`, fake y `Abstract…Contract` en `core-testing`, y stub en `api`. Cero lógica de negocio.
- [ ] La firma es implementable por el dueño con los datos que tiene, y consumible por quien la pide. Si dudas, pide al agente del módulo dueño que la comente: abre su sesión y dile "revisa la PR #n y comenta si puedes implementar esa firma".
- [ ] Es aditiva. Si quita o renombra algo, exige las dos PRs de adaptación el mismo día.
- [ ] El test de contrato cubre los casos límite que definirán el comportamiento (sin datos, n pequeño, valores nulos).

### 4.4 Lista para escribir un issue

Un agente trabaja bien con un issue que tenga estas cinco cosas; sin alguna, se inventa el resto.

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
- tests con Testcontainers y con un servidor Nightscout simulado (WireMock); ninguno usa la red
- ./mvnw -q -pl glucurvia-cgm -am verify en verde
```

Créalo con `gh issue create --title "..." --body-file issue.md --label module:cgm --label machine:imac --label phase:1`.

---

## 5. Cómo respondes a lo que te piden los agentes

| El agente dice… | Tú haces |
|---|---|
| "Necesito X de `core` que no existe" | Le contestas: "abre la PR de contrato con la firma, el fake, el test y el stub, y apila tu rama encima". No la escribes tú. |
| "Necesito que `journal` devuelva también Y" | Es un contrato: PR de contrato aditiva por parte del que pide; issue para el dueño de `journal` con la implementación. |
| "El test de otro módulo falla en mi PR" | Si es por su cambio en `core`, adapta él en la misma tanda. Si no, `main` estaba roto: revierte el último merge y abre un `fix/…`. |
| "¿Puedo tocar `application.yml`?" | No. Que lea la configuración con `@ConfigurationProperties(prefix="glucurvia.<módulo>")` y valores por defecto en código, y añada su bloque a `.env.example`. |
| "Me hace falta una librería" | Que la declare en el `pom.xml` de su módulo. Si otro módulo ya la usa, la subes tú al padre. |
| "Necesito datos reales" | Que apunte `GLUCURVIA_CGM_NIGHTSCOUT_URL` al iMac por Tailscale con el token de lectura, o restaura tú el volcado de anoche en su base de worktree. |
| "Flyway se queja del checksum" | `docker compose -f docker-compose.dev.yml down -v` en su worktree y a empezar. Nunca `repair` en el iMac. |
| "¿Puedo arreglar de paso algo de `nutrition`?" | No. Que abra un issue en `nutrition` con lo que vio. |
| Lleva una hora dando vueltas | Párale. Léelo. Reescribe el issue más pequeño o con el dato que le faltaba. Reinicia la sesión con el mensaje de arranque. |

---

## 6. Producción en el iMac

### 6.1 Desplegar

Guarda esto como `scripts/deploy-imac.sh` en el repo (el humano lo crea en la fase 0) y ejecútalo desde el iMac:

```bash
#!/usr/bin/env bash
set -euo pipefail
cd ~/Desarrollo/glucurvia-prod
git pull --ff-only origin main
docker compose --profile prod up -d --build
sleep 15
curl -fsS http://localhost:8080/api/v1/cgm/status && echo && echo "OK $(git rev-parse --short HEAD)"
```

Si `api` no arranca por un stub activo en `prod`, es que se mergeó un contrato sin su implementación: no es un fallo del despliegue, es una PR pendiente en el otro lado. Vuelve a la versión anterior (6.2) y crea el issue.

### 6.2 Volver atrás

```bash
cd ~/Desarrollo/glucurvia-prod && git checkout v0.1.<n-1> && docker compose --profile prod up -d --build
```

Las migraciones ya aplicadas no se deshacen: por eso los cambios de esquema son aditivos y alterar columnas es contrato. Cuando `main` esté arreglado, `git checkout main` y despliega de nuevo.

### 6.3 Copias de seguridad

Volcado nocturno a una carpeta que Time Machine sí respalda (los volúmenes de Docker no):

```bash
docker compose --profile prod exec -T postgres pg_dump -U glucurvia glucurvia | gzip > ~/Backups/glucurvia/glucurvia-$(date +%F).sql.gz
```

Prográmalo con `launchd` o `cron` a las 03:00 y **prueba una restauración una vez al mes** en la base de un worktree: una copia que nunca se ha restaurado no es una copia. La MongoDB de Nightscout no necesita volcado: todo lo que ha visto está en Postgres al minuto.

### 6.4 Lo que nunca haces en el clon de producción

Abrir agentes, cambiar de rama, editar archivos, `flyway repair`, `docker compose down -v`.

---

## 7. Cómo sabes que vas bien

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

## 8. Costes y límites que conviene vigilar

- **Claude.** Los agentes de módulo trabajan bien con el esfuerzo por defecto; el esfuerzo máximo resérvalo para revisiones de diseño y contratos, que es donde una omisión cuesta días. Un agente que da vueltas quema más que uno que se para y pregunta: por eso el mensaje de arranque le dice que se detenga si necesita un contrato.
- **API de Anthropic** (la del producto, no la de Claude Code): límite de gasto mensual en la consola del proveedor desde el primer día. Ningún test la llama; si ves consumo un día sin haber usado la app, algo la está llamando y hay que mirarlo.
- **GitHub Actions**: 2 000 minutos al mes en el plan gratuito. `gh api /repos/windoctor7/glucurvia/actions/cache/usage` y la página de facturación te dicen cuánto llevas. Cuando ronde el 70 %, runner autohospedado en el iMac.
- **Disco del iMac**: los volúmenes de Docker crecen (Mongo, Postgres, imágenes viejas). `docker system prune -f` una vez al mes, nunca con `-a --volumes` en producción.

---

## 9. Seguridad mínima (la que sí aplica a uso personal)

- Contraseña única o Tailscale delante de la API y de Nightscout; ningún puerto abierto a internet. Comprueba desde datos móviles, sin Tailscale, que `http://<ip-pública>:8080` no responde.
- Secretos solo en los `.env` (ignorados por Git) y en la consola del proveedor. Nunca pegues una clave en el chat de un agente: si la necesita, está en el `.env` de su worktree.
- La cuenta seguidora de LibreLinkUp tiene su propia contraseña, distinta de la de tu cuenta principal de Abbott.
- Copia de seguridad probada (6.3). Es el único riesgo irreversible del proyecto.

---

## Apéndice A — Mensajes de arranque por módulo

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
Móvil primero (390 px), instalable, sin service worker. Termina con `npm run gen && npm run lint && npm run build && npm test` en verde, abre la PR con la plantilla y comenta en el issue.
```

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
gh issue create --title "..." --body-file issue.md --label module:cgm --label machine:imac --label phase:1
gh run list --branch main --limit 10         # ¿main verde?
git worktree list                            # qué agentes tienen carpeta
git worktree remove ../glucurvia-<módulo>    # limpiar al mergear
git tag v0.1.<n> && git push --tags          # punto de retorno tras cambios de core o schema
```

## Apéndice C — Lo que hago yo (esta sesión) y lo que haces tú

Yo puedo ejecutar la fase 0 entera contigo al lado, escribir los issues de la fase 1, pasar `/code-review` a las PRs, revisar contratos con esfuerzo máximo y redactar los tres documentos. Tú decides los contratos, mergeas, despliegas y usas la app: sin tus comidas y tus lecturas reales, la fase 3 no tiene con qué trabajar.
