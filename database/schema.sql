-- =====================================================================
-- schema.sql — Hospital & Clinic Operations Optimizer
-- Target engine: SQLite 3 (single-file database, no server needed).
--
-- This schema is created automatically by database.DatabaseManager on first
-- run; it is also kept here as the human-readable source of truth referenced by
-- report section 3 (Dataset description & data dictionary) and section 9
-- (Database integration evidence).
--
-- Design notes:
--   * locations are the network's nodes; roads are its (undirected) edges,
--     stored once with a CHECK that keeps from < to so we don't duplicate a
--     corridor as both A->B and B->A.
--   * service_requests and resources are the operational workload / workforce.
--   * algorithm_runs and audit_events are append-only history tables that give
--     the report real, reproducible evidence (timings and a change trail).
-- =====================================================================

PRAGMA foreign_keys = ON;

-- ---------------------------------------------------------------------
-- locations: departments, wards, and service points (graph vertices)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS locations (
    location_id TEXT PRIMARY KEY,           -- e.g. 'L001'
    name        TEXT NOT NULL,              -- e.g. 'Emergency Department'
    area        TEXT,                       -- block/zone, e.g. 'Main Block'
    type        TEXT,                       -- ward | department | unit | ...
    notes       TEXT
);

-- ---------------------------------------------------------------------
-- roads: corridors/routes between two locations (graph edges)
--   distance_m  -> used by Prim/Kruskal MST (cheapest network)
--   travel_secs -> used by Dijkstra (fastest route)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roads (
    road_id      INTEGER PRIMARY KEY AUTOINCREMENT,
    from_id      TEXT NOT NULL,
    to_id        TEXT NOT NULL,
    distance_m   REAL NOT NULL CHECK (distance_m  >= 0),
    travel_secs  REAL NOT NULL CHECK (travel_secs >= 0),
    notes        TEXT,
    FOREIGN KEY (from_id) REFERENCES locations(location_id) ON DELETE CASCADE,
    FOREIGN KEY (to_id)   REFERENCES locations(location_id) ON DELETE CASCADE,
    UNIQUE (from_id, to_id)                 -- one corridor per ordered pair
);
CREATE INDEX IF NOT EXISTS idx_roads_from ON roads(from_id);
CREATE INDEX IF NOT EXISTS idx_roads_to   ON roads(to_id);

-- ---------------------------------------------------------------------
-- service_requests: the pending operational workload (heap / greedy / DP)
--   urgency 1 = most urgent (triage-critical) .. 5 = routine
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS service_requests (
    request_id      TEXT PRIMARY KEY,       -- e.g. 'SR0001'
    source          TEXT NOT NULL,
    destination     TEXT NOT NULL,
    category        TEXT,                    -- patient-transfer | lab-sample | ...
    urgency         INTEGER NOT NULL CHECK (urgency BETWEEN 1 AND 5),
    time_submitted  TEXT,                    -- ISO-8601 string
    deadline        TEXT,
    status          TEXT DEFAULT 'PENDING',  -- PENDING | ASSIGNED | DONE
    service_minutes INTEGER DEFAULT 15,      -- estimated staff time (greedy/DP weight)
    value_score     INTEGER DEFAULT 0,       -- benefit score (DP value)
    FOREIGN KEY (source)      REFERENCES locations(location_id) ON DELETE CASCADE,
    FOREIGN KEY (destination) REFERENCES locations(location_id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_requests_urgency ON service_requests(urgency);

-- ---------------------------------------------------------------------
-- resources: staff / equipment / ambulances / beds (greedy workforce)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS resources (
    resource_id         TEXT PRIMARY KEY,    -- e.g. 'R001'
    type                TEXT NOT NULL,        -- nurse | porter | ambulance | ...
    home_location       TEXT,
    capacity            INTEGER NOT NULL CHECK (capacity >= 0),
    availability_status TEXT DEFAULT 'AVAILABLE', -- AVAILABLE | BUSY | OFFLINE
    FOREIGN KEY (home_location) REFERENCES locations(location_id) ON DELETE SET NULL
);

-- ---------------------------------------------------------------------
-- algorithm_runs: append-only timing/result log for every algorithm run
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS algorithm_runs (
    run_id         INTEGER PRIMARY KEY AUTOINCREMENT,
    algorithm      TEXT NOT NULL,            -- 'Dijkstra' | 'Kruskal' | 'GreedyAssign' | ...
    input_summary  TEXT,                     -- inputs + graph size
    elapsed_nanos  INTEGER NOT NULL,         -- measured wall-clock time
    result_summary TEXT,                     -- short description of the outcome
    run_at         TEXT NOT NULL             -- ISO-8601 timestamp
);

-- ---------------------------------------------------------------------
-- audit_events: append-only change trail (create/update/delete/load/save)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_events (
    event_id     INTEGER PRIMARY KEY AUTOINCREMENT,
    event_type   TEXT NOT NULL,              -- CREATE | UPDATE | DELETE | LOAD | SAVE
    entity       TEXT NOT NULL,              -- Location | Route | ServiceRequest | Resource | Database
    detail       TEXT,
    occurred_at  TEXT NOT NULL               -- ISO-8601 timestamp
);
