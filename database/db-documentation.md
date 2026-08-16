# Campus Service & Routing Database — Schema Documentation

**Role 3 — Database Design** · University of Ghana, Legon
Engine: **SQLite 3.37+** (developed and tested against 3.46)
Files: `schema.sql` · `import_csv.py` · `db-documentation.md` (this file)

---

## 1. Overview

The database backs a campus service-dispatch and routing system. Its centre of
gravity is a **weighted graph**: `locations` are the vertices, `roads` are the
edges, and shortest-path algorithms run over them to send the right `resource`
to the place a `service_request` came from. Every execution of an algorithm is
recorded in `algorithm_runs` so the team can compare Dijkstra against A\* on
real data, and every consequential change is written to `audit_events`.

```
                      ┌───────────────┐
                      │   locations   │  (graph vertices)
                      └───────┬───────┘
        ┌─────────────────────┼──────────────────────┬────────────────┐
        │ from_location_id    │ base_location_id     │ location_id    │ source/target
        │ to_location_id      │                      │                │ _location_id
   ┌────┴─────┐        ┌──────┴──────┐      ┌────────┴─────────┐  ┌───┴────────────┐
   │  roads   │        │  resources  │      │ service_requests │  │ algorithm_runs │
   └──────────┘        └──────┬──────┘      └────────┬─────────┘  └───┬────────────┘
                              │ assigned_resource_id │                │ request_id
                              └──────────────────────┘────────────────┘

   ┌──────────────┐
   │ audit_events │  polymorphic (entity_table + entity_id) — see §2.6
   └──────────────┘
```

**Foreign keys at a glance**

| # | Child table | Column | Parent | ON DELETE |
|---|---|---|---|---|
| 1 | `roads` | `from_location_id` | `locations` | RESTRICT |
| 2 | `roads` | `to_location_id` | `locations` | RESTRICT |
| 3 | `resources` | `base_location_id` | `locations` | SET NULL |
| 4 | `service_requests` | `location_id` | `locations` | RESTRICT |
| 5 | `service_requests` | `assigned_resource_id` | `resources` | SET NULL |
| 6 | `algorithm_runs` | `request_id` | `service_requests` | SET NULL |
| 7 | `algorithm_runs` | `source_location_id` | `locations` | RESTRICT |
| 8 | `algorithm_runs` | `target_location_id` | `locations` | RESTRICT |

All eight use `ON UPDATE CASCADE`. `audit_events` deliberately has none (§2.6).

---

## 2. The tables

### 2.1 `locations`

**Purpose.** One row per addressable place on campus — halls, departments,
libraries, food joints, clinics — and the vertex set of the routing graph.
Every other table in the schema ultimately hangs off this one, because in a
campus system almost every fact is a fact *about a place*. **Keys.**
`location_id INTEGER PRIMARY KEY` is the surrogate key; `location_code` is the
natural key and carries a `UNIQUE` constraint, holding the same human-readable
slug the campus map already uses (`balme-library`, `jqb`), which lets the map
front-end and the database refer to the same place without a translation
table. **Relationships.** It is the parent in six of the eight foreign keys:
`roads` twice (both endpoints), `resources.base_location_id`,
`service_requests.location_id`, and both endpoints of `algorithm_runs`.
**Integrity.** `category` is restricted by CHECK to the seventeen categories
the campus map defines; latitude and longitude are constrained to a bounding
box around Legon (5.60–5.70 N, −0.25 to −0.15 E), which catches the classic
survey errors — swapped lat/lng, or a dropped minus sign — before a bad
coordinate silently corrupts every distance calculation downstream. Deletion
is RESTRICTed rather than cascading: a place that has road or request history
is retired by setting `is_active = 0`, never removed.

### 2.2 `roads`

**Purpose.** One row per traversable segment joining two locations — the edge
set of the graph, and the table the shortest-path algorithms actually iterate
over. `length_m` is the edge weight; `road_type`, `surface` and
`condition_rating` let a route be costed by more than raw distance (a
wheelchair route may exclude `earth` surfaces; a shuttle route may only use
`shuttle_route` and `major` segments). **Keys.** `road_id` is the surrogate
key, `road_code` the unique natural key. The important constraint is the
composite `UNIQUE (from_location_id, to_location_id)`, which guarantees the
adjacency list cannot contain duplicate parallel edges — a real hazard when
survey data is merged from several sources, and one that makes shortest-path
output non-deterministic. **Relationships.** Two foreign keys, both to
`locations`; this self-referencing pair is what makes the table a graph rather
than a list. **Integrity.** `CHECK (from_location_id <> to_location_id)`
forbids self-loops, which make path output ambiguous and can trap naive
traversals in an infinite cycle. `length_m` must be strictly positive —
Dijkstra is only correct on non-negative weights, so this CHECK is what
protects the algorithm's core precondition at the storage layer.
`is_bidirectional` marks whether the reverse traversal is legal; the loader
that builds the in-memory graph reads one row as two directed edges when it
is set.

### 2.3 `resources`

**Purpose.** The dispatchable inventory — maintenance crews, security patrols,
ambulances, shuttle buses, sanitation teams, ICT support and mobile equipment.
Each row carries what the allocation algorithm needs to choose between
candidates: what kind of unit it is, where it is stationed, how much it can
carry, what it costs per hour, and whether it is free right now. **Keys.**
`resource_id` surrogate, `resource_code` unique natural key (`RES-004`).
**Relationships.** Child of `locations` through `base_location_id`, and parent
of `service_requests.assigned_resource_id`. It therefore sits in the middle of
the dispatch chain: *place → request → resource → base place*, which is the
join the allocation query walks. **Integrity.** `base_location_id` is nullable
with `ON DELETE SET NULL`, because retiring a depot must not delete the crew
that was stationed there — it just leaves them temporarily unbased.
`ck_res_retired` enforces the cross-column rule that a resource with
`status = 'retired'` must also have `is_active = 0`, closing the gap where a
retired unit could still be picked up by a query filtering only on
`is_active`.

### 2.4 `service_requests`

**Purpose.** The work queue: one row per reported issue, from a broken lecture
hall projector to a blocked drain. It is the demand side of the system, and
the reason a routing computation happens at all. **Keys.** `request_id`
surrogate, `request_code` unique natural key (`SR-2026-0042`).
**Relationships.** Child of `locations` (where the problem is, RESTRICT) and
of `resources` (who is handling it, SET NULL); parent of
`algorithm_runs.request_id`. **Integrity.** This table carries the schema's
richest business rules, all enforced by the database rather than trusted to
application code: a request whose `status` is `resolved` must have a
`resolved_at` timestamp (`ck_req_resolved`); one that is `assigned` or
`in_progress` must actually name a resource (`ck_req_assigned`); and neither
`acknowledged_at` nor `resolved_at` may precede `reported_at`
(`ck_req_ack_order`, `ck_req_res_order`). Together these make the impossible
states — a resolved ticket with no resolution time, an assigned ticket with
nobody assigned, a ticket closed before it was opened — unrepresentable.
`assigned_resource_id` is deliberately SET NULL rather than RESTRICT so a
decommissioned unit can be removed while its historical tickets survive,
merely showing as unassigned.

### 2.5 `algorithm_runs`

**Purpose.** An immutable measurement log: one row per execution of a routing
or allocation algorithm, capturing both the *answer* (`path_json`,
`total_cost_m`) and the *cost of computing it* (`runtime_ms`,
`nodes_expanded`, `edges_relaxed`). This is the table the complexity-analysis
section of the report is written from — it is what lets the team say Dijkstra
expanded N nodes where A\* expanded M on the same source/target pair.
**Keys.** `run_id` surrogate, `run_code` unique natural key (`RUN-0007`).
**Relationships.** Child of `service_requests` (the ticket that triggered the
run, nullable — benchmark runs have no ticket) and of `locations` twice, for
the source and target vertices. **Integrity.** `parameters_json` and
`path_json` are validated with `json_valid()`, so a malformed blob cannot be
stored. `ck_run_error` requires a failed run to record *why* it failed, and
`ck_run_success` requires a successful shortest-path run to carry both a path
and a cost — a "successful" run with no answer is a contradiction.
`ck_run_endpoints` requires shortest-path runs to name both endpoints. That
last constraint is why the two location foreign keys use **RESTRICT rather
than SET NULL**: nulling an endpoint on delete would violate
`ck_run_endpoints` and abort the DELETE with a confusing CHECK error instead
of a clear foreign-key one. The two rules would have silently contradicted
each other; the test suite caught it, and §4 records the fix.

### 2.6 `audit_events`

**Purpose.** An append-only trail of who changed what, when, and from what to
what — the accountability layer over the other five tables. `old_values` and
`new_values` hold JSON snapshots, so a change can be reconstructed or reversed
without a second history table per entity. **Keys.** `event_id` is the
surrogate primary key. The table has no natural key by design: two identical
events a second apart are both legitimate, so no uniqueness constraint
applies. **Relationships — and the deliberate absence of a foreign key.**
`audit_events` is *polymorphic*: `entity_id` points into whichever table
`entity_table` names. No single `REFERENCES` clause can express "this integer
lives in one of five different tables", so a declarative foreign key is not
available. This is the correct trade-off for two independent reasons. First,
an audit row must be able to describe a **deleted** entity — an FK with
`ON DELETE CASCADE` would erase exactly the history a deletion most needs to
leave behind, and `RESTRICT` would make deletion impossible forever. Second,
the log is append-only, so the usual argument for referential enforcement
(preventing drift as rows are edited) does not apply. Integrity is instead
maintained at two other levels: `ck_audit_table` restricts `entity_table` to
the five auditable table names, and `import_csv.py` resolves every
`entity_code` against the target table at load time, refusing the import if
the referenced row is absent. `test_database.py` additionally proves with an
explicit `NOT EXISTS` sweep that no audit row dangles. `ck_audit_update`
requires an `UPDATE` event to record both sides of the change, since a
one-sided update record is not auditable.

---

## 3. The CSV contract (interface with Role 2 — Dataset Team)

`import_csv.py` expects six files. **Headers must match exactly**; the loader
rejects missing required columns *and* unrecognised extra columns rather than
importing them silently.

The single most important rule: **CSVs reference other rows by natural code,
never by integer ID.** The Dataset Team cannot know which `location_id` the
database will assign, and code-based references survive a full reload
unchanged. The importer builds `code → id` maps as it loads and resolves them
in a foreign-key-safe order. Columns therefore end in `_code` in the CSV where
the database column ends in `_id`.

| File | Columns (`*` = required) |
|---|---|
| `locations.csv` | `location_code*`, `name*`, `category*`, `latitude*`, `longitude*`, `description`, `is_active` |
| `roads.csv` | `road_code*`, `name`, `from_location_code*`, `to_location_code*`, `length_m*`, `road_type*`, `surface`, `is_bidirectional`, `speed_limit_kph`, `condition_rating`, `is_active` |
| `resources.csv` | `resource_code*`, `name*`, `resource_type*`, `base_location_code`, `capacity`, `status`, `cost_per_hour`, `contact_phone`, `is_active` |
| `service_requests.csv` | `request_code*`, `location_code*`, `assigned_resource_code`, `category*`, `priority`, `status`, `description`, `reported_by`, `reported_at`, `acknowledged_at`, `resolved_at` |
| `algorithm_runs.csv` | `run_code*`, `algorithm*`, `purpose*`, `request_code`, `source_location_code`, `target_location_code`, `parameters_json`, `status`, `started_at*`, `finished_at`, `runtime_ms`, `nodes_expanded`, `edges_relaxed`, `total_cost_m`, `path_json`, `error_message` |
| `audit_events.csv` | `entity_table*`, `entity_code*`, `action*`, `actor`, `actor_role`, `occurred_at`, `old_values`, `new_values`, `notes` |

**Conventions**

- Encoding UTF-8 (a BOM is tolerated). Timestamps are `YYYY-MM-DD HH:MM:SS`.
- An **empty cell means NULL**. Required columns may not be empty.
- Booleans are `1` / `0`.
- `path_json` stores **location codes**, not IDs, so a stored route stays
  readable and survives a reload: `["balme-library","jqb","great-hall"]`.
- Enumerated columns (`category`, `road_type`, `status`, `priority`,
  `algorithm`, …) must use one of the values listed in the CHECK constraints
  in `schema.sql`; anything else is rejected at import.

**Running it**

```bash
python import_csv.py                       # build campus.db from ./data
python import_csv.py --dry-run             # validate CSVs, write nothing
python import_csv.py --data-dir ../role2/out --db campus.db
```

The whole load runs in **one transaction**: any bad row aborts the import and
leaves no partial database behind, so a failed run can be fixed and retried
without cleanup. On success the script runs `PRAGMA foreign_key_check` as a
final gate.

---

## 4. Testing

`python test_database.py` — **49 checks, all passing.**

- **A. Schema loads** — `schema.sql` applies to an empty database with no
  errors; all six tables exist, all are `STRICT`, `PRAGMA integrity_check`
  returns `ok`, and `PRAGMA foreign_key_check` finds no orphans.
- **B. SELECTs return correct rows** — every expected value is recomputed
  independently from the CSV files rather than copied from a previous run, so
  a mangled import or a wrong join fails the test. This covers row counts,
  single-row lookups, `GROUP BY` aggregates, two- and three-table joins,
  `LEFT JOIN` null-preservation, `ORDER BY … LIMIT`, `AVG` with a filter, and
  NULL handling. The strongest check walks each stored route through the
  `roads` table edge by edge and confirms the lengths sum to the recorded
  `total_cost_m` — 29 routes, all consistent.
- **C. Constraints reject bad data** — 18 statements that *must* fail, each
  asserting on the **specific** constraint that rejected it.
- **D. Referential actions** — `ON DELETE SET NULL`, `ON UPDATE CASCADE` and
  `RESTRICT` are each shown to behave as declared.

**One schema bug was found and fixed this way.** The `RESTRICT` test initially
passed while being rejected by an unrelated CHECK. Asserting on the *reason*
for rejection exposed the real problem: `algorithm_runs` declared
`ON DELETE SET NULL` on its two location endpoints while `ck_run_endpoints`
required shortest-path runs to keep both — so the SET NULL could never
succeed, and deleting any location used in a benchmark would fail with a
misleading error. Both foreign keys were changed to `RESTRICT` (§2.5).

---

## 5. Design decisions worth defending in the report

**Surrogate primary keys alongside unique natural keys.** Every table has an
`INTEGER PRIMARY KEY` *and* a unique business code. Joins and indexes use the
compact integer; humans, CSVs and the campus map front-end use the code. This
also means a place can be renamed or recoded without rewriting every child row.

**STRICT tables.** Without `STRICT`, SQLite's type declarations are advisory
affinities — it will happily store the text `'north'` in a `REAL` latitude
column. `STRICT` makes them enforced, which is why `test_database.py` can
demonstrate a genuine type rejection. The cost is that only
`INT/INTEGER/REAL/TEXT/BLOB/ANY` are permitted, so length limits that would
normally be `VARCHAR(n)` are written as `CHECK (length(x) BETWEEN …)` instead
— arguably clearer, since the limit is then explicit rather than implied.

**CHECK constraints instead of lookup tables.** The brief specifies exactly six
tables, so enumerations (`category`, `status`, `priority`, `algorithm`, …) are
CHECK lists rather than reference tables. The trade-off is honest: a CHECK is
faster and needs no join, but changing the allowed set requires a schema
migration. Were a seventh table permitted, `location_categories` would be the
first candidate.

**Timestamps as ISO-8601 TEXT.** SQLite has no native date type. ISO-8601
sorts and compares correctly as text, and works with `julianday()` for the
elapsed-time queries in `queries.sql`.

**Indexes chosen from the query plan, not by reflex.** `roads` is indexed on
both endpoint columns because adjacency lookup is the inner loop of every
shortest-path run; `service_requests` on `(status, priority)` because that is
the dispatcher's main screen; `audit_events` on `(entity_table, entity_id)`
because "show me this ticket's history" is the only way the log is ever read.

---

## 6. Portability

The schema keeps to a portable subset. To move to another engine:

| SQLite | PostgreSQL | MySQL 8 |
|---|---|---|
| `INTEGER PRIMARY KEY` | `GENERATED ALWAYS AS IDENTITY` (or `SERIAL`) | `INT AUTO_INCREMENT` |
| `TEXT` | `TEXT` / `VARCHAR(n)` | `VARCHAR(n)` / `TEXT` |
| `REAL` | `DOUBLE PRECISION` | `DOUBLE` |
| `REAL` (money) | `NUMERIC(10,2)` | `DECIMAL(10,2)` |
| `INTEGER` 0/1 + CHECK | `BOOLEAN` | `TINYINT(1)` |
| `TEXT` ISO-8601 | `TIMESTAMPTZ` | `DATETIME` |
| `TEXT` + `json_valid()` | `JSONB` | `JSON` |
| `datetime('now')` | `now()` | `CURRENT_TIMESTAMP` |
| `… ) STRICT;` | (types always enforced) | (types always enforced) |
| `PRAGMA foreign_keys = ON` | (always on) | (InnoDB: always on) |

Everything else — `PRIMARY KEY`, `UNIQUE`, `REFERENCES`, `ON DELETE`/
`ON UPDATE`, named `CONSTRAINT … CHECK`, `CREATE INDEX` — is standard SQL and
transfers unchanged. On PostgreSQL, `CHECK` enumerations could become `ENUM`
types and the coordinate columns could become PostGIS `GEOGRAPHY(POINT)`,
which would make radius and nearest-neighbour queries native.

---

## 7. Files in `/database/`

| File | Role |
|---|---|
| `schema.sql` | **Deliverable.** The six `CREATE TABLE` statements plus indexes. |
| `import_csv.py` | **Deliverable.** Loads the Dataset Team's CSVs into the database. |
| `db-documentation.md` | **Deliverable.** This document. |
| `test_database.py` | Acceptance tests (49 checks) proving the schema loads and queries are correct. |
| `queries.sql` | Ten worked example queries for the report. |
| `make_sample_data.py` | Dev utility that generated `data/*.csv`. Not part of the running system. |
| `data/*.csv` | Sample dataset — see the caveat below. |
| `campus.db` | Built artefact; regenerate at any time with `python import_csv.py`. |

### Caveat on the sample data

Role 2's surveyed CSVs were not yet available, so `data/` holds a stand-in
dataset that exists to prove the schema and importer work end to end:

- **`locations.csv` is real** — all 100 places, with their true coordinates
  and categories, extracted from `ug-campus-map/locations.js`.
- **`roads.csv` is derived, not surveyed.** It is a k-nearest-neighbour graph
  over those real coordinates, forced into a single connected component, with
  great-circle distances inflated 15–35% to approximate paths that bend around
  buildings. The topology is plausible; it is **not** a survey and should not
  be quoted as one in the report.
- **`resources.csv`, `service_requests.csv`, `algorithm_runs.csv` and
  `audit_events.csv` are synthetic**, generated from a fixed seed
  (`SEED = 20260804`) so results are reproducible. The routes in
  `algorithm_runs` are genuine Dijkstra outputs over the generated graph,
  which is why the path-consistency test in §4 is meaningful.

When the surveyed CSVs arrive, drop them into `data/` (or point `--data-dir`
at them) and re-run `python import_csv.py`. No schema change is needed
provided the headers in §3 are used.
