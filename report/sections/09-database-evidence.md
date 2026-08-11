# 9. Database Integration Evidence

## 9.1 Engine and connection

Storage is **SQLite 3** — a single file, no server, zero setup — accessed over
JDBC (the `org.xerial:sqlite-jdbc` driver, the only third-party jar used by the
running system). The database lives at `database/hospital.db` and is created
automatically on first run. All the JDBC code is in
[`database/DatabaseManager.java`](../../src/main/java/database/DatabaseManager.java);
the schema is [`database/schema.sql`](../../database/schema.sql).

## 9.2 How load / save works

1. **Start-up (load).** `ConsoleApp` opens the DB and runs `schema.sql`
   (idempotent — `CREATE TABLE IF NOT EXISTS`). If the DB already holds data it is
   loaded into memory; otherwise the seed CSVs are loaded and **mirrored into the
   DB** in one transaction.
2. **Editing.** Adding/updating a location, route, request or resource updates the
   in-memory `Repository` and then calls `saveAll`, which rewrites the four entity
   tables inside a transaction so the DB always mirrors memory. Each change is
   written to `audit_events`.
3. **Save on demand.** Menu option 12 offers "save to DB", "reload from DB", and
   "export back to CSV".
4. **Run logging.** Every algorithm run is timed and appended to `algorithm_runs`.

## 9.3 Live row counts (from an actual run)

```
locations        = 60 rows
roads            = 150 rows
service_requests = 320 rows
resources        = 36 rows
algorithm_runs   = 3 rows
audit_events     = 3 rows
```

## 9.4 Sample records (queried from `hospital.db`)

**locations**

| location_id | name | area | type |
|---|---|---|---|
| L001 | Emergency Department | Surgical Block | department |
| L002 | Outpatient Department | Support Services | department |
| L003 | Radiology | Emergency Block | department |

**service_requests** (from the seed data)

| request_id | source | destination | category | urgency | status |
|---|---|---|---|---|---|
| SR0001 | L001 | L022 | blood-delivery | 4 | PENDING |
| SR0002 | L022 | L012 | medication-run | 4 | PENDING |

**resources**

| resource_id | type | home_location | capacity | availability_status |
|---|---|---|---|---|
| R001 | trolley | L037 | 114 | AVAILABLE |
| R002 | trolley | L049 | 38 | AVAILABLE |

## 9.5 Algorithm-run log (real captured evidence)

The `algorithm_runs` table after running a few features — this is where the
report's timing evidence originates (it is not hand-typed):

```
Lookup       | key=Radiology                 | 0.057 ms  | hit
DPKnapsack   | requests=320 budget=120       | 6.121 ms  | chosen=15 value=765
GreedyAssign | requests=320 resources=36     | 11.563 ms | assigned=108 unassigned=212
```

*(The Documentation Team may insert a screenshot of the console or a DB-browser
view here; the tables above are the exact data those screenshots would show.)*

## 9.6 Integrity features

- Foreign keys (`PRAGMA foreign_keys = ON`) tie roads/requests/resources back to
  real locations, with `ON DELETE CASCADE`/`SET NULL`.
- `CHECK` constraints enforce `urgency BETWEEN 1 AND 5`, non-negative
  distance/time/capacity.
- `UNIQUE(from_id, to_id)` stores each corridor once.
- Saves run inside a transaction (`commit`/`rollback`) so a failure never leaves
  the DB half-written. Integration tests (`DatabaseIntegrationTest`) prove a full
  save→load round-trip preserves every table.
