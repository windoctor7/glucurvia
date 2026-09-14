# glucurvia-schema

Todas las migraciones Flyway del proyecto, en `src/main/resources/db/migration/<módulo>/`.

- Versión con marca de tiempo: `V202609141530__anade_scan_fraction.sql`.
- Cada módulo edita solo su carpeta y solo sus tablas (plan, sección 5).
- `core/` contiene el baseline (todas las tablas del diseño, sección 3) y `users` sembrado; lo edita el humano.
- Nunca se edita una migración que ya está en `main`.
