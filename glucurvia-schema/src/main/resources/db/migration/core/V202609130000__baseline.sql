-- Baseline de Glucurvia: todas las tablas del diseño (docs/diseno-mvp.md, sección 3).
-- Lo mantiene el humano. Los módulos añaden migraciones en su carpeta; nunca se edita este archivo una vez en main.

create extension if not exists pg_trgm;

-- ===== api =====
create table users (
  id            uuid primary key default gen_random_uuid(),
  display_name  text,
  timezone      text not null default 'America/Mexico_City',
  glucose_unit  text not null default 'MG_DL',
  locale        text not null default 'es-MX',
  preferences   jsonb not null default '{}',
  created_at    timestamptz not null default now()
);

-- Un solo usuario, con id fijo para poder referirlo desde configuración (glucurvia.user-id).
insert into users (id, display_name, timezone, glucose_unit, locale)
values ('00000000-0000-0000-0000-000000000001', 'Autor', 'America/Mexico_City', 'MG_DL', 'es-MX');

-- ===== cgm =====
create table import_batches (
  id                   uuid primary key default gen_random_uuid(),
  user_id              uuid not null references users(id) on delete cascade,
  source               text not null,            -- LIBREVIEW_CSV | MANUAL (los pulls de Nightscout no crean lote)
  source_name          text,
  content_hash         text,
  assumed_timezone     text not null,
  detected_cadence_sec int,
  device_serial        text,
  status               text not null,            -- RECEIVED | STORED | REVERTED | FAILED
  rows_total int, rows_inserted int, rows_duplicate int, rows_rejected int,
  range_start timestamptz, range_end timestamptz,
  warnings             jsonb not null default '[]',
  error                text,
  created_at           timestamptz not null default now()
);

create table cgm_pull_state (
  source               text primary key,         -- NIGHTSCOUT
  last_pull_at         timestamptz, last_reading_ts timestamptz,
  last_status          text, last_error text,
  consecutive_failures int not null default 0
);

create table glucose_readings (
  user_id          uuid not null references users(id) on delete cascade,
  ts               timestamptz not null,
  reading_type     smallint not null,            -- 0 HISTORIC | 1 SCAN | 2 REALTIME | 3 MANUAL | 4 STRIP
  value_mgdl       numeric(5,1) not null,
  is_clipped       boolean not null default false,
  import_batch_id  uuid references import_batches(id),
  primary key (user_id, ts, reading_type)
);
create index glucose_readings_batch_idx on glucose_readings (import_batch_id);

-- ===== assistant =====
create table conversations (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  started_at timestamptz not null default now(),
  last_message_at timestamptz
);

create table conversation_messages (
  id                 uuid primary key default gen_random_uuid(),
  conversation_id    uuid not null references conversations(id) on delete cascade,
  client_message_id  uuid unique,
  role               text not null,              -- USER | ASSISTANT | TOOL
  content            text,
  tool_calls         jsonb,
  model              text, prompt_version text,
  tokens_in int, tokens_out int,
  client_time        timestamptz,
  created_at         timestamptz not null default now()
);

-- ===== journal =====
create table events (
  id                   uuid primary key default gen_random_uuid(),
  user_id              uuid not null references users(id) on delete cascade,
  type                 text not null,            -- MEAL | ACTIVITY | SLEEP | STRESS | MEDICATION | ALCOHOL | SYMPTOM | NOTE
  started_at           timestamptz not null,
  ended_at             timestamptz,
  local_tz             text not null,
  time_confidence      text not null,            -- EXACT | APPROX | INFERRED
  time_uncertainty_min int not null default 0,
  time_basis           text,
  source               text not null,            -- CHAT | MANUAL | IMPORT
  raw_text             text,
  message_id           uuid references conversation_messages(id),
  attributes           jsonb not null default '{}',
  created_at           timestamptz not null default now(),
  updated_at           timestamptz not null default now(),
  deleted_at           timestamptz
);
create index events_user_started_idx on events (user_id, started_at desc) where deleted_at is null;
create index events_user_type_started_idx on events (user_id, type, started_at desc) where deleted_at is null;

-- ===== nutrition =====
create table food_references (
  id              uuid primary key default gen_random_uuid(),
  name            text not null,
  aliases         text[] not null default '{}',
  search_text     text not null,                 -- name + aliases normalizados por la aplicación
  category        text,
  source          text not null,                 -- USDA_FDC | CURATED | LLM_FALLBACK
  source_notes    text,
  per_100g        jsonb not null,                -- {"carbs_total","fiber","carbs_available","carb_convention","protein","fat","kcal"}
  portions        jsonb not null default '{}',
  density_g_ml    numeric(4,2),
  locale_notes    jsonb not null default '{}',
  constraint food_references_convention_chk check (per_100g ? 'carb_convention' and per_100g ? 'carbs_available')
);
create index food_references_search_idx on food_references using gin (search_text gin_trgm_ops);

create table user_food_aliases (
  user_id      uuid not null references users(id) on delete cascade,
  alias        text not null,
  food_ref_id  uuid references food_references(id),
  grams        numeric(7,1),
  per_100g     jsonb,
  learned_from uuid,
  primary key (user_id, alias)
);

-- ===== journal (comidas) =====
create table meals (
  event_id            uuid primary key references events(id) on delete cascade,
  meal_type           text,                      -- BREAKFAST | LUNCH | DINNER | SNACK
  carbs_g             numeric(6,1), carbs_low numeric(6,1), carbs_high numeric(6,1),
  carbs_worst_low     numeric(6,1), carbs_worst_high numeric(6,1),
  fiber_g             numeric(6,1), protein_g numeric(6,1), fat_g numeric(6,1), kcal numeric(7,1),
  confidence          numeric(3,2),
  estimator_version   text not null,
  clarification       text,
  estimated_at        timestamptz not null
);

create table meal_items (
  id                uuid primary key default gen_random_uuid(),
  meal_id           uuid not null references meals(event_id) on delete cascade,
  position          int not null,
  raw_description   text not null,
  food_name         text not null,
  preparation       text,
  food_ref_id       uuid references food_references(id),
  quantity          numeric(7,2), unit text,
  grams             numeric(7,1), grams_low numeric(7,1), grams_high numeric(7,1),
  volume_ml         numeric(7,1),
  portion_eaten     numeric(3,2) not null default 1.0,
  carbs_g           numeric(6,1), carbs_low numeric(6,1), carbs_high numeric(6,1),
  fiber_g           numeric(6,1), protein_g numeric(6,1), fat_g numeric(6,1), kcal numeric(7,1),
  confidence        numeric(3,2),
  llm_confidence    numeric(3,2),
  macro_source      text not null,               -- CATALOG | USER_ALIAS | LLM_FALLBACK | LABEL
  assumptions       text[] not null default '{}',
  needs_clarification boolean not null default false
);
create index meal_items_meal_idx on meal_items (meal_id, position);

-- ===== glycemic =====
create table glycemic_responses (
  meal_event_id        uuid primary key references meals(event_id) on delete cascade,
  algorithm_version    text not null,
  computed_at          timestamptz not null,
  series_source        text not null,            -- REALTIME | HISTORIC
  cadence_sec          int not null,
  scan_fraction        numeric(4,3),
  quality              text not null,            -- GOOD | PARTIAL | CONFOUNDED | INSUFFICIENT
  coverage_pct         numeric(5,2),
  max_gap_min          int,
  baseline_mgdl        numeric(5,1), baseline_n int, baseline_unstable boolean,
  peak_mgdl            numeric(5,1), peak_at timestamptz, time_to_peak_min int, delta_peak_mgdl numeric(5,1),
  g30 numeric(5,1), g60 numeric(5,1), g90 numeric(5,1), g120 numeric(5,1), g180 numeric(5,1),
  return_to_baseline_min int,
  iauc_window_start    timestamptz,
  iauc_0_120           numeric(8,1),
  iauc_0_180           numeric(8,1),
  max_rise_rate        numeric(5,2),
  max_fall_rate        numeric(5,2),
  minutes_above_personal_p90 int,
  personal_p90_mgdl    numeric(5,1),
  personal_range_period_end date,
  window_end           timestamptz,
  flags                text[] not null default '{}',
  context_tags         text[] not null default '{}',
  confounder_event_ids uuid[] not null default '{}'
);
create index glycemic_responses_tags_idx on glycemic_responses using gin (context_tags);
