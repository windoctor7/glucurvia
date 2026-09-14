title: human-1 · Producción en el iMac: clon de prod, uploader, pg_dump nocturno, contraseña o Tailscale, api sirviendo web/dist
labels: module:api,machine:human,phase:1
**Dueño:** humano · **Fase:** 1

**Contexto:** guía 1.2 y 6; plan 6.2; diseño 13.

**Terminado cuando:**
- [ ] Clon `~/Desarrollo/glucurvia-prod` en el iMac con `.env` real; `docker compose --profile prod up -d` con Nightscout recibiendo glucosa.
- [ ] `scripts/backup-imac.sh` programado a las 03:00 y una restauración probada en la base de un worktree.
- [ ] Contraseña única en Spring Security o Tailscale delante de la API y de Nightscout; comprobado desde datos móviles que nada responde fuera de la red privada.
- [ ] Dockerfile multietapa: construye `web/` y `api` sirve `web/dist` como estáticos; servicio `api` activo en `docker-compose.yml`.
- [ ] `scripts/deploy-imac.sh` ejecutado con éxito una vez.
