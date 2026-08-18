# database/

- `schema.sql`   — the SQLite schema (tables for locations, routes, service
  requests, resources, plus the `algorithm_runs` and `audit_events` log tables).
- `seed-data/`   — the four CSV files the application loads on first start
  (`locations.csv`, `routes.csv`, `service-requests.csv`, `resources.csv`).
- `hospital.db`  — the SQLite database file. It is created at runtime from the
  schema and seed CSVs, so it is intentionally excluded from git (`.gitignore`).

The seed CSVs are produced by `database.SeedDataGenerator` (a coherent, synthetic
hospital dataset) and can be regenerated at any time — see the root `README.md`.
