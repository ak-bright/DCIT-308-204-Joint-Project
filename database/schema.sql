-- ============================================================================
-- UG CAMPUS SERVICE & ROUTING DATABASE — SCHEMA
-- ============================================================================
-- Target engine : SQLite 3.37+  (developed and tested on 3.46)
-- Encoding      : UTF-8
-- Author        : Role 3 — Database Design
--
-- Six tables, in dependency order:
--
--     locations ──┬──< roads (from_location_id, to_location_id)
--                 ├──< resources.base_location_id
--                 ├──< service_requests.location_id
--                 └──< algorithm_runs.(source_location_id, target_location_id)
--
--     resources ─────< service_requests.assigned_resource_id
--     service_requests ──< algorithm_runs.request_id
--     audit_events  (polymorphic log — see db-documentation.md)
--
-- NOTES ON DIALECT
--   * STRICT tables are used so column types are actually ENFORCED. Without
--     STRICT, SQLite would happily store the text 'abc' in a REAL column.
--     STRICT restricts us to INT/INTEGER/REAL/TEXT/BLOB/ANY, so length and
--     domain limits are expressed as CHECK constraints instead of VARCHAR(n).
--   * Dates/times are ISO-8601 TEXT ('YYYY-MM-DD HH:MM:SS'). SQLite has no
--     native DATE/TIMESTAMP type; ISO-8601 sorts and compares correctly.
--   * Booleans are INTEGER 0/1, constrained by CHECK.
--   * PRAGMA foreign_keys must be ON per connection — SQLite defaults it OFF.
--     The import script sets it explicitly.
--   * See db-documentation.md for the PostgreSQL / MySQL type substitutions.
-- ============================================================================

PRAGMA foreign_keys = ON;

-- Dropped in reverse dependency order so re-running this file is safe.
DROP TABLE IF EXISTS audit_events;
DROP TABLE IF EXISTS algorithm_runs;
DROP TABLE IF EXISTS service_requests;
DROP TABLE IF EXISTS resources;
DROP TABLE IF EXISTS roads;
DROP TABLE IF EXISTS locations;


-- ----------------------------------------------------------------------------
-- 1. locations — every addressable place on campus (graph VERTICES)
-- ----------------------------------------------------------------------------
CREATE TABLE locations (
    location_id     INTEGER PRIMARY KEY,
    location_code   TEXT    NOT NULL UNIQUE,
    name            TEXT    NOT NULL,
    category        TEXT    NOT NULL,
    latitude        REAL    NOT NULL,
    longitude       REAL    NOT NULL,
    description     TEXT,
    is_active       INTEGER NOT NULL DEFAULT 1,
    created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at      TEXT    NOT NULL DEFAULT (datetime('now')),

    CONSTRAINT ck_loc_code_len  CHECK (length(location_code) BETWEEN 2 AND 64),
    CONSTRAINT ck_loc_name_len  CHECK (length(name) BETWEEN 1 AND 160),
    -- University of Ghana, Legon sits near 5.65 N, -0.19 W. The bounding box
    -- keeps obviously bad survey data (swapped lat/lng, missing minus sign)
    -- out of the graph rather than letting it corrupt distance calculations.
    CONSTRAINT ck_loc_lat       CHECK (latitude  BETWEEN   5.60 AND   5.70),
    CONSTRAINT ck_loc_lng       CHECK (longitude BETWEEN  -0.25 AND  -0.15),
    CONSTRAINT ck_loc_active    CHECK (is_active IN (0, 1)),
    CONSTRAINT ck_loc_category  CHECK (category IN (
        'departments', 'lecture-halls', 'libraries', 'research', 'landmarks',
        'residences', 'hostels', 'food', 'health', 'banking', 'sports',
        'offices', 'ict', 'childcare', 'hotspots', 'transport', 'worship'
    ))
) STRICT;

CREATE INDEX idx_locations_category ON locations (category);
CREATE INDEX idx_locations_active   ON locations (is_active);


-- ----------------------------------------------------------------------------
-- 2. roads — traversable segments between two locations (graph EDGES)
-- ----------------------------------------------------------------------------
CREATE TABLE roads (
    road_id             INTEGER PRIMARY KEY,
    road_code           TEXT    NOT NULL UNIQUE,
    name                TEXT,
    from_location_id    INTEGER NOT NULL,
    to_location_id      INTEGER NOT NULL,
    length_m            REAL    NOT NULL,
    road_type           TEXT    NOT NULL,
    surface             TEXT    NOT NULL DEFAULT 'asphalt',
    is_bidirectional    INTEGER NOT NULL DEFAULT 1,
    speed_limit_kph     REAL,
    condition_rating    INTEGER,
    is_active           INTEGER NOT NULL DEFAULT 1,
    created_at          TEXT    NOT NULL DEFAULT (datetime('now')),

    CONSTRAINT fk_road_from FOREIGN KEY (from_location_id)
        REFERENCES locations (location_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_road_to   FOREIGN KEY (to_location_id)
        REFERENCES locations (location_id) ON UPDATE CASCADE ON DELETE RESTRICT,

    -- A road may not start and end at the same vertex (no self-loops: they
    -- make shortest-path output ambiguous and can trap naive traversals).
    CONSTRAINT ck_road_no_self_loop CHECK (from_location_id <> to_location_id),
    CONSTRAINT ck_road_length       CHECK (length_m > 0 AND length_m <= 20000),
    CONSTRAINT ck_road_speed        CHECK (speed_limit_kph IS NULL
                                           OR (speed_limit_kph > 0 AND speed_limit_kph <= 120)),
    CONSTRAINT ck_road_condition    CHECK (condition_rating IS NULL
                                           OR condition_rating BETWEEN 1 AND 5),
    CONSTRAINT ck_road_bidir        CHECK (is_bidirectional IN (0, 1)),
    CONSTRAINT ck_road_active       CHECK (is_active IN (0, 1)),
    CONSTRAINT ck_road_type         CHECK (road_type IN (
        'footpath', 'service', 'minor', 'major', 'shuttle_route'
    )),
    CONSTRAINT ck_road_surface      CHECK (surface IN (
        'asphalt', 'concrete', 'paved_stone', 'gravel', 'earth'
    )),
    -- At most one segment per ordered pair, so the adjacency list cannot
    -- silently contain duplicate parallel edges.
    CONSTRAINT uq_road_pair UNIQUE (from_location_id, to_location_id)
) STRICT;

-- Adjacency lookups are the inner loop of Dijkstra / A*; both directions are
-- indexed because a bidirectional edge is traversed either way.
CREATE INDEX idx_roads_from   ON roads (from_location_id);
CREATE INDEX idx_roads_to     ON roads (to_location_id);
CREATE INDEX idx_roads_active ON roads (is_active);


-- ----------------------------------------------------------------------------
-- 3. resources — crews, vehicles and equipment that can be dispatched
-- ----------------------------------------------------------------------------
CREATE TABLE resources (
    resource_id         INTEGER PRIMARY KEY,
    resource_code       TEXT    NOT NULL UNIQUE,
    name                TEXT    NOT NULL,
    resource_type       TEXT    NOT NULL,
    base_location_id    INTEGER,
    capacity            INTEGER NOT NULL DEFAULT 1,
    status              TEXT    NOT NULL DEFAULT 'available',
    cost_per_hour       REAL,
    contact_phone       TEXT,
    is_active           INTEGER NOT NULL DEFAULT 1,
    created_at          TEXT    NOT NULL DEFAULT (datetime('now')),

    -- ON DELETE SET NULL: retiring a depot must not delete the crew record,
    -- it just leaves them unassigned to a base.
    CONSTRAINT fk_resource_base FOREIGN KEY (base_location_id)
        REFERENCES locations (location_id) ON UPDATE CASCADE ON DELETE SET NULL,

    CONSTRAINT ck_res_capacity CHECK (capacity >= 0),
    CONSTRAINT ck_res_cost     CHECK (cost_per_hour IS NULL OR cost_per_hour >= 0),
    CONSTRAINT ck_res_active   CHECK (is_active IN (0, 1)),
    CONSTRAINT ck_res_type     CHECK (resource_type IN (
        'maintenance_crew', 'security_patrol', 'ambulance', 'shuttle_bus',
        'sanitation_team', 'it_support', 'equipment'
    )),
    CONSTRAINT ck_res_status   CHECK (status IN (
        'available', 'deployed', 'maintenance', 'retired'
    )),
    -- A retired resource must not still be flagged active.
    CONSTRAINT ck_res_retired  CHECK (status <> 'retired' OR is_active = 0)
) STRICT;

CREATE INDEX idx_resources_status ON resources (status, resource_type);
CREATE INDEX idx_resources_base   ON resources (base_location_id);


-- ----------------------------------------------------------------------------
-- 4. service_requests — reported issues awaiting dispatch
-- ----------------------------------------------------------------------------
CREATE TABLE service_requests (
    request_id           INTEGER PRIMARY KEY,
    request_code         TEXT    NOT NULL UNIQUE,
    location_id          INTEGER NOT NULL,
    assigned_resource_id INTEGER,
    category             TEXT    NOT NULL,
    priority             TEXT    NOT NULL DEFAULT 'medium',
    status               TEXT    NOT NULL DEFAULT 'open',
    description          TEXT,
    reported_by          TEXT,
    reported_at          TEXT    NOT NULL DEFAULT (datetime('now')),
    acknowledged_at      TEXT,
    resolved_at          TEXT,

    -- RESTRICT: a location with request history cannot be deleted outright;
    -- it must be de-activated instead, preserving the record.
    CONSTRAINT fk_req_location FOREIGN KEY (location_id)
        REFERENCES locations (location_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_req_resource FOREIGN KEY (assigned_resource_id)
        REFERENCES resources (resource_id) ON UPDATE CASCADE ON DELETE SET NULL,

    CONSTRAINT ck_req_category CHECK (category IN (
        'maintenance', 'sanitation', 'security', 'medical',
        'it_support', 'transport', 'utilities'
    )),
    CONSTRAINT ck_req_priority CHECK (priority IN ('low', 'medium', 'high', 'critical')),
    CONSTRAINT ck_req_status   CHECK (status IN (
        'open', 'assigned', 'in_progress', 'resolved', 'cancelled'
    )),
    -- Lifecycle rules enforced by the database, not just by application code:
    CONSTRAINT ck_req_ack_order   CHECK (acknowledged_at IS NULL
                                         OR acknowledged_at >= reported_at),
    CONSTRAINT ck_req_res_order   CHECK (resolved_at IS NULL
                                         OR resolved_at >= reported_at),
    CONSTRAINT ck_req_resolved    CHECK (status <> 'resolved' OR resolved_at IS NOT NULL),
    CONSTRAINT ck_req_assigned    CHECK (status NOT IN ('assigned', 'in_progress')
                                         OR assigned_resource_id IS NOT NULL)
) STRICT;

-- The dispatcher's main query: open work, most urgent first.
CREATE INDEX idx_requests_status_priority ON service_requests (status, priority);
CREATE INDEX idx_requests_location        ON service_requests (location_id);
CREATE INDEX idx_requests_resource        ON service_requests (assigned_resource_id);
CREATE INDEX idx_requests_reported_at     ON service_requests (reported_at);


-- ----------------------------------------------------------------------------
-- 5. algorithm_runs — one row per execution of a routing/allocation algorithm
-- ----------------------------------------------------------------------------
CREATE TABLE algorithm_runs (
    run_id              INTEGER PRIMARY KEY,
    run_code            TEXT    NOT NULL UNIQUE,
    algorithm           TEXT    NOT NULL,
    purpose             TEXT    NOT NULL,
    request_id          INTEGER,
    source_location_id  INTEGER,
    target_location_id  INTEGER,
    parameters_json     TEXT,
    status              TEXT    NOT NULL DEFAULT 'success',
    started_at          TEXT    NOT NULL,
    finished_at         TEXT,
    runtime_ms          REAL,
    nodes_expanded      INTEGER,
    edges_relaxed       INTEGER,
    total_cost_m        REAL,
    path_json           TEXT,
    error_message       TEXT,

    -- request_id is SET NULL: a run is an immutable performance measurement
    -- and must outlive the request that prompted it.
    CONSTRAINT fk_run_request FOREIGN KEY (request_id)
        REFERENCES service_requests (request_id) ON UPDATE CASCADE ON DELETE SET NULL,
    -- The two endpoints are RESTRICT, NOT SET NULL. SET NULL here would be
    -- unsatisfiable: ck_run_endpoints below requires a shortest_path run to
    -- keep both endpoints, so nulling them on delete would always abort the
    -- DELETE with a confusing CHECK error instead of a clear FK error.
    -- Locations are retired by setting is_active = 0, never hard-deleted.
    CONSTRAINT fk_run_source  FOREIGN KEY (source_location_id)
        REFERENCES locations (location_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_run_target  FOREIGN KEY (target_location_id)
        REFERENCES locations (location_id) ON UPDATE CASCADE ON DELETE RESTRICT,

    CONSTRAINT ck_run_algorithm CHECK (algorithm IN (
        'dijkstra', 'a_star', 'bfs', 'dfs', 'bellman_ford', 'floyd_warshall',
        'kruskal', 'prim', 'greedy_assignment', 'hungarian'
    )),
    CONSTRAINT ck_run_purpose   CHECK (purpose IN (
        'shortest_path', 'resource_allocation', 'connectivity', 'coverage'
    )),
    CONSTRAINT ck_run_status    CHECK (status IN ('success', 'failed', 'timeout', 'no_path')),
    CONSTRAINT ck_run_time      CHECK (finished_at IS NULL OR finished_at >= started_at),
    CONSTRAINT ck_run_runtime   CHECK (runtime_ms     IS NULL OR runtime_ms     >= 0),
    CONSTRAINT ck_run_nodes     CHECK (nodes_expanded IS NULL OR nodes_expanded >= 0),
    CONSTRAINT ck_run_edges     CHECK (edges_relaxed  IS NULL OR edges_relaxed  >= 0),
    CONSTRAINT ck_run_cost      CHECK (total_cost_m   IS NULL OR total_cost_m   >= 0),
    -- JSON columns must hold parseable JSON (SQLite json1 extension).
    CONSTRAINT ck_run_params_json CHECK (parameters_json IS NULL OR json_valid(parameters_json)),
    CONSTRAINT ck_run_path_json   CHECK (path_json       IS NULL OR json_valid(path_json)),
    -- A failed run has to say why; a successful path search has to have a cost.
    CONSTRAINT ck_run_error     CHECK (status <> 'failed' OR error_message IS NOT NULL),
    CONSTRAINT ck_run_success   CHECK (NOT (status = 'success' AND purpose = 'shortest_path')
                                       OR (path_json IS NOT NULL AND total_cost_m IS NOT NULL)),
    -- A shortest-path run needs both endpoints.
    CONSTRAINT ck_run_endpoints CHECK (purpose <> 'shortest_path'
                                       OR (source_location_id IS NOT NULL
                                           AND target_location_id IS NOT NULL))
) STRICT;

CREATE INDEX idx_runs_algorithm ON algorithm_runs (algorithm, started_at);
CREATE INDEX idx_runs_request   ON algorithm_runs (request_id);
CREATE INDEX idx_runs_status    ON algorithm_runs (status);


-- ----------------------------------------------------------------------------
-- 6. audit_events — append-only trail of who changed what, and when
-- ----------------------------------------------------------------------------
-- Deliberately has NO foreign key: entity_id points into whichever table
-- entity_table names, so no single REFERENCES clause can express it, and an
-- FK would also delete history when the audited row is deleted — the exact
-- opposite of what an audit trail is for. Referential integrity is instead
-- enforced by ck_audit_table plus the application. See db-documentation.md.
-- ----------------------------------------------------------------------------
CREATE TABLE audit_events (
    event_id        INTEGER PRIMARY KEY,
    entity_table    TEXT    NOT NULL,
    entity_id       INTEGER NOT NULL,
    action          TEXT    NOT NULL,
    actor           TEXT    NOT NULL DEFAULT 'system',
    actor_role      TEXT    NOT NULL DEFAULT 'system',
    occurred_at     TEXT    NOT NULL DEFAULT (datetime('now')),
    old_values      TEXT,
    new_values      TEXT,
    notes           TEXT,

    CONSTRAINT ck_audit_table  CHECK (entity_table IN (
        'locations', 'roads', 'resources',
        'service_requests', 'algorithm_runs'
    )),
    CONSTRAINT ck_audit_action CHECK (action IN (
        'INSERT', 'UPDATE', 'DELETE', 'STATUS_CHANGE', 'ASSIGN', 'IMPORT'
    )),
    CONSTRAINT ck_audit_role   CHECK (actor_role IN (
        'student', 'staff', 'dispatcher', 'admin', 'system'
    )),
    CONSTRAINT ck_audit_entity CHECK (entity_id > 0),
    CONSTRAINT ck_audit_old_json CHECK (old_values IS NULL OR json_valid(old_values)),
    CONSTRAINT ck_audit_new_json CHECK (new_values IS NULL OR json_valid(new_values)),
    -- An UPDATE must record both sides of the change to be auditable.
    CONSTRAINT ck_audit_update CHECK (action <> 'UPDATE'
                                      OR (old_values IS NOT NULL AND new_values IS NOT NULL))
) STRICT;

CREATE INDEX idx_audit_entity   ON audit_events (entity_table, entity_id);
CREATE INDEX idx_audit_time     ON audit_events (occurred_at);
CREATE INDEX idx_audit_actor    ON audit_events (actor);
