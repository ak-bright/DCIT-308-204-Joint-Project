#!/usr/bin/env python3
"""
test_database.py — acceptance tests for the campus database
=============================================================================
Proves three things the deliverable is judged on:

  A. schema.sql loads into a fresh database with no errors.
  B. Basic SELECT queries return the CORRECT rows — every expected value is
     computed independently from the CSV files, not copied from a previous
     run of the query, so a wrong join or a mangled import would fail here.
  C. The constraints are real: foreign keys, CHECKs, UNIQUE and STRICT types
     all reject bad data instead of silently accepting it.

Usage:  python test_database.py            (builds a scratch DB, tests, exits)
        python test_database.py --keep     (leaves test_campus.db for inspection)
Exit code 0 = all passed, 1 = at least one failure.
"""

import argparse
import csv
import json
import os
import sqlite3
import sys
from collections import Counter, defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = os.path.join(HERE, "data")
SCHEMA = os.path.join(HERE, "schema.sql")
TEST_DB = os.path.join(HERE, "test_campus.db")

PASS, FAIL = [], []


def check(name, got, want):
    """Assert equality and record the outcome."""
    if got == want:
        PASS.append(name)
        print("  PASS  %-58s %s" % (name, _fmt(got)))
    else:
        FAIL.append((name, got, want))
        print("  FAIL  %-58s got %s, want %s" % (name, _fmt(got), _fmt(want)))


def check_true(name, condition, detail=""):
    if condition:
        PASS.append(name)
        print("  PASS  %-58s %s" % (name, detail))
    else:
        FAIL.append((name, detail, "condition to hold"))
        print("  FAIL  %-58s %s" % (name, detail))


def check_rejects(conn, name, sql, params=(), expect=None):
    """The database MUST refuse this statement, for the RIGHT reason.

    `expect` is a substring of the expected error (e.g. a constraint name).
    Asserting on it matters: an earlier version of this suite "passed" a
    foreign-key test that was in fact being rejected by an unrelated CHECK,
    which hid a real conflict between two constraints in schema.sql.
    """
    try:
        with conn:
            conn.execute(sql, params)
    except (sqlite3.IntegrityError, sqlite3.DatabaseError) as exc:
        msg = str(exc)
        if expect and expect.lower() not in msg.lower():
            FAIL.append((name, msg, "error containing %r" % expect))
            print("  FAIL  %-58s rejected by the WRONG rule: %s" % (name, msg[:40]))
            return
        PASS.append(name)
        print("  PASS  %-58s rejected: %s" % (name, msg[:44]))
        return
    FAIL.append((name, "accepted", "rejected"))
    print("  FAIL  %-58s row was ACCEPTED but should have been rejected" % name)


def _fmt(v):
    s = str(v)
    return s if len(s) <= 40 else s[:37] + "..."


def read_csv(filename):
    with open(os.path.join(DATA, filename), newline="", encoding="utf-8-sig") as fh:
        return [r for r in csv.DictReader(fh)
                if any((v or "").strip() for v in r.values())]


def section(title):
    print("\n" + title)
    print("-" * 74)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--keep", action="store_true",
                    help="keep the test database file after the run")
    args = ap.parse_args()

    # ---------------------------------------------------------------- A. load
    section("A. Schema loads")
    if os.path.exists(TEST_DB):
        os.remove(TEST_DB)

    import import_csv
    rc = import_csv.main(["--db", TEST_DB, "--data-dir", DATA,
                          "--schema", SCHEMA, "--quiet"])
    check("import_csv.py exit code", rc, 0)

    conn = sqlite3.connect(TEST_DB)
    conn.execute("PRAGMA foreign_keys = ON")

    tables = [r[0] for r in conn.execute(
        "SELECT name FROM sqlite_master WHERE type='table' "
        "AND name NOT LIKE 'sqlite_%' ORDER BY name")]
    check("six tables created", tables, [
        "algorithm_runs", "audit_events", "locations",
        "resources", "roads", "service_requests"])

    # Every table must be STRICT, or the declared types enforce nothing.
    non_strict = [t for t in tables
                  if "STRICT" not in (conn.execute(
                      "SELECT sql FROM sqlite_master WHERE name=?", (t,)
                  ).fetchone()[0] or "").upper()]
    check("all tables declared STRICT", non_strict, [])

    check("integrity_check", conn.execute("PRAGMA integrity_check").fetchone()[0], "ok")
    check("foreign_key_check finds no orphans",
          conn.execute("PRAGMA foreign_key_check").fetchall(), [])

    # ------------------------------------------------- B. correct SELECT rows
    section("B. SELECT queries return correct rows")

    csv_locations = read_csv("locations.csv")
    csv_roads = read_csv("roads.csv")
    csv_resources = read_csv("resources.csv")
    csv_requests = read_csv("service_requests.csv")
    csv_runs = read_csv("algorithm_runs.csv")
    csv_audit = read_csv("audit_events.csv")

    # -- row counts match the source files exactly
    for table, rows in [("locations", csv_locations), ("roads", csv_roads),
                        ("resources", csv_resources),
                        ("service_requests", csv_requests),
                        ("algorithm_runs", csv_runs),
                        ("audit_events", csv_audit)]:
        check("row count: %s" % table,
              conn.execute("SELECT count(*) FROM %s" % table).fetchone()[0],
              len(rows))

    # -- a known real place round-trips with the right values
    row = conn.execute(
        "SELECT name, category, latitude, longitude FROM locations "
        "WHERE location_code = 'balme-library'").fetchone()
    check("lookup balme-library", row, ("Balme Library", "libraries", 5.6518, -0.1871))

    # -- GROUP BY against a Counter computed straight from the CSV
    want_cats = Counter(r["category"] for r in csv_locations)
    got_cats = dict(conn.execute(
        "SELECT category, count(*) FROM locations GROUP BY category"))
    check("locations per category", got_cats, dict(want_cats))

    # -- the biggest category, computed both ways
    top_csv = want_cats.most_common(1)[0]
    top_sql = conn.execute(
        "SELECT category, count(*) c FROM locations "
        "GROUP BY category ORDER BY c DESC, category LIMIT 1").fetchone()
    check("largest category", top_sql, top_csv)

    # -- JOIN correctness: resolve a road back to its endpoint NAMES, which
    #    only works if the code -> id resolution during import was right.
    code_to_name = {r["location_code"]: r["name"] for r in csv_locations}
    sample = csv_roads[0]
    got = conn.execute("""
        SELECT a.name, b.name, r.length_m
          FROM roads r
          JOIN locations a ON a.location_id = r.from_location_id
          JOIN locations b ON b.location_id = r.to_location_id
         WHERE r.road_code = ?""", (sample["road_code"],)).fetchone()
    check("JOIN roads -> location names (%s)" % sample["road_code"], got,
          (code_to_name[sample["from_location_code"]],
           code_to_name[sample["to_location_code"]],
           float(sample["length_m"])))

    # -- aggregate over a join: open/assigned requests per category
    want_open = Counter(r["category"] for r in csv_requests
                        if r["status"] in ("open", "assigned", "in_progress"))
    got_open = dict(conn.execute("""
        SELECT category, count(*) FROM service_requests
         WHERE status IN ('open','assigned','in_progress')
         GROUP BY category"""))
    check("outstanding requests per category", got_open, dict(want_open))

    # -- three-table join: request -> location -> assigned resource
    assigned = [r for r in csv_requests if r["assigned_resource_code"]]
    want_join = len(assigned)
    got_join = conn.execute("""
        SELECT count(*)
          FROM service_requests sr
          JOIN locations  l ON l.location_id  = sr.location_id
          JOIN resources  r ON r.resource_id  = sr.assigned_resource_id""").fetchone()[0]
    check("3-table join request/location/resource", got_join, want_join)

    # -- LEFT JOIN must preserve the unassigned rows
    got_left = conn.execute("""
        SELECT count(*) FROM service_requests sr
          LEFT JOIN resources r ON r.resource_id = sr.assigned_resource_id""").fetchone()[0]
    check("LEFT JOIN keeps unassigned requests", got_left, len(csv_requests))

    # -- the adjacency query that a shortest-path algorithm actually issues
    degree = Counter()
    for r in csv_roads:
        if r["is_active"] == "1":
            degree[r["from_location_code"]] += 1
            degree[r["to_location_code"]] += 1
    busiest_code, busiest_deg = degree.most_common(1)[0]
    got_deg = conn.execute("""
        SELECT count(*) FROM roads r
          JOIN locations l ON l.location_id IN (r.from_location_id, r.to_location_id)
         WHERE l.location_code = ? AND r.is_active = 1""", (busiest_code,)).fetchone()[0]
    check("adjacency degree of %s" % busiest_code, got_deg, busiest_deg)

    # -- DATA CONSISTENCY: for every successful shortest-path run, walk the
    #    stored path through the roads table and confirm the edge lengths sum
    #    to the recorded total_cost_m. This exercises the roads <-> runs
    #    relationship far harder than a row count does.
    edge_len = {}
    for r in csv_roads:
        a, b, ln = r["from_location_code"], r["to_location_code"], float(r["length_m"])
        edge_len[(a, b)] = ln
        if r["is_bidirectional"] == "1":
            edge_len[(b, a)] = ln

    checked, mismatched = 0, []
    for run in conn.execute("""
            SELECT run_code, path_json, total_cost_m FROM algorithm_runs
             WHERE purpose = 'shortest_path' AND status = 'success'"""):
        run_code, path_json, total = run
        path = json.loads(path_json)
        walked = sum(edge_len[(path[i], path[i + 1])] for i in range(len(path) - 1))
        checked += 1
        if abs(walked - total) > 0.5:
            mismatched.append((run_code, round(walked, 1), total))
    check_true("stored paths sum to total_cost_m (%d runs)" % checked,
               not mismatched and checked > 0,
               "all consistent" if not mismatched else str(mismatched[:3]))

    # -- every stored path starts at source and ends at target
    bad_ends = conn.execute("""
        SELECT count(*) FROM algorithm_runs ar
          JOIN locations s ON s.location_id = ar.source_location_id
          JOIN locations t ON t.location_id = ar.target_location_id
         WHERE ar.purpose = 'shortest_path' AND ar.status = 'success'
           AND (json_extract(ar.path_json, '$[0]') <> s.location_code
             OR json_extract(ar.path_json, '$[#-1]') <> t.location_code)""").fetchone()[0]
    check("paths start at source and end at target", bad_ends, 0)

    # -- polymorphic audit resolution: audit rows point at real entities
    want_audit = Counter(r["entity_table"] for r in csv_audit)
    got_audit = dict(conn.execute(
        "SELECT entity_table, count(*) FROM audit_events GROUP BY entity_table"))
    check("audit events per entity_table", got_audit, dict(want_audit))

    orphan_audit = conn.execute("""
        SELECT count(*) FROM audit_events a
         WHERE (a.entity_table = 'service_requests'
                AND NOT EXISTS (SELECT 1 FROM service_requests x WHERE x.request_id = a.entity_id))
            OR (a.entity_table = 'locations'
                AND NOT EXISTS (SELECT 1 FROM locations x WHERE x.location_id = a.entity_id))
            OR (a.entity_table = 'roads'
                AND NOT EXISTS (SELECT 1 FROM roads x WHERE x.road_id = a.entity_id))
            OR (a.entity_table = 'resources'
                AND NOT EXISTS (SELECT 1 FROM resources x WHERE x.resource_id = a.entity_id))
            OR (a.entity_table = 'algorithm_runs'
                AND NOT EXISTS (SELECT 1 FROM algorithm_runs x WHERE x.run_id = a.entity_id))
        """).fetchone()[0]
    check("no dangling audit references", orphan_audit, 0)

    # -- ORDER BY / LIMIT returns the genuinely longest road
    longest_csv = max(csv_roads, key=lambda r: float(r["length_m"]))
    longest_sql = conn.execute(
        "SELECT road_code, length_m FROM roads ORDER BY length_m DESC LIMIT 1").fetchone()
    check("longest road", longest_sql,
          (longest_csv["road_code"], float(longest_csv["length_m"])))

    # -- AVG with a WHERE clause
    dij = [float(r["runtime_ms"]) for r in csv_runs
           if r["algorithm"] == "dijkstra" and r["runtime_ms"]]
    got_avg = conn.execute(
        "SELECT round(avg(runtime_ms), 6) FROM algorithm_runs "
        "WHERE algorithm = 'dijkstra'").fetchone()[0]
    check("avg dijkstra runtime_ms",
          got_avg, round(sum(dij) / len(dij), 6) if dij else None)

    # -- NULL handling survived the import ('' in CSV must become NULL)
    csv_nulls = sum(1 for r in csv_requests if not r["resolved_at"].strip())
    db_nulls = conn.execute(
        "SELECT count(*) FROM service_requests WHERE resolved_at IS NULL").fetchone()[0]
    check("empty CSV cells imported as NULL", db_nulls, csv_nulls)

    # -- unicode / punctuation survived the round trip
    n_commas = conn.execute(
        "SELECT count(*) FROM locations WHERE description LIKE '%,%'").fetchone()[0]
    check_true("descriptions with commas parsed correctly",
               n_commas > 20, "%d descriptions contain commas" % n_commas)

    # ------------------------------------------------ C. constraints are real
    section("C. Constraints reject invalid data")

    loc_id = conn.execute("SELECT location_id FROM locations LIMIT 1").fetchone()[0]

    other_id = conn.execute(
        "SELECT location_id FROM locations WHERE location_id <> ? LIMIT 1",
        (loc_id,)).fetchone()[0]

    check_rejects(conn, "FK: road to a non-existent location",
                  "INSERT INTO roads (road_code, from_location_id, to_location_id,"
                  " length_m, road_type) VALUES ('BAD-1', ?, 999999, 100, 'footpath')",
                  (loc_id,), expect="FOREIGN KEY")

    check_rejects(conn, "CHECK: self-loop road",
                  "INSERT INTO roads (road_code, from_location_id, to_location_id,"
                  " length_m, road_type) VALUES ('BAD-2', ?, ?, 100, 'footpath')",
                  (loc_id, loc_id), expect="ck_road_no_self_loop")

    check_rejects(conn, "CHECK: negative road length",
                  "INSERT INTO roads (road_code, from_location_id, to_location_id,"
                  " length_m, road_type) VALUES ('BAD-3', ?, ?, -5, 'footpath')",
                  (loc_id, other_id), expect="ck_road_length")

    check_rejects(conn, "CHECK: unknown road_type",
                  "INSERT INTO roads (road_code, from_location_id, to_location_id,"
                  " length_m, road_type) VALUES ('BAD-4', ?, ?, 50, 'teleporter')",
                  (loc_id, other_id), expect="ck_road_type")

    check_rejects(conn, "UNIQUE: duplicate location_code",
                  "INSERT INTO locations (location_code, name, category, latitude,"
                  " longitude) VALUES ('balme-library', 'Copy', 'libraries', 5.65, -0.187)",
                  expect="UNIQUE")

    # SQLite reports a UNIQUE violation by column list, not by constraint
    # name, so this asserts on the columns rather than on 'uq_road_pair'.
    check_rejects(conn, "UNIQUE: duplicate road between the same two places",
                  "INSERT INTO roads (road_code, from_location_id, to_location_id,"
                  " length_m, road_type) SELECT 'BAD-5', from_location_id,"
                  " to_location_id, 100, 'footpath' FROM roads LIMIT 1",
                  expect="roads.from_location_id, roads.to_location_id")

    check_rejects(conn, "CHECK: latitude outside campus bounds",
                  "INSERT INTO locations (location_code, name, category, latitude,"
                  " longitude) VALUES ('off-map', 'Nowhere', 'offices', 51.5, -0.187)",
                  expect="ck_loc_lat")

    check_rejects(conn, "CHECK: unknown category",
                  "INSERT INTO locations (location_code, name, category, latitude,"
                  " longitude) VALUES ('bad-cat', 'X', 'nightclub', 5.65, -0.187)",
                  expect="ck_loc_category")

    check_rejects(conn, "STRICT: text in a REAL column",
                  "INSERT INTO locations (location_code, name, category, latitude,"
                  " longitude) VALUES ('bad-type', 'X', 'offices', 'north', -0.187)",
                  expect="cannot store TEXT")

    check_rejects(conn, "CHECK: resolved request without resolved_at",
                  "INSERT INTO service_requests (request_code, location_id, category,"
                  " status) VALUES ('BAD-SR1', ?, 'maintenance', 'resolved')",
                  (loc_id,), expect="ck_req_resolved")

    check_rejects(conn, "CHECK: assigned request without a resource",
                  "INSERT INTO service_requests (request_code, location_id, category,"
                  " status) VALUES ('BAD-SR2', ?, 'maintenance', 'assigned')",
                  (loc_id,), expect="ck_req_assigned")

    check_rejects(conn, "CHECK: resolved_at before reported_at",
                  "INSERT INTO service_requests (request_code, location_id, category,"
                  " status, reported_at, resolved_at) VALUES ('BAD-SR3', ?, 'medical',"
                  " 'resolved', '2026-08-01 10:00:00', '2026-07-01 10:00:00')",
                  (loc_id,), expect="ck_req_res_order")

    check_rejects(conn, "CHECK: failed run without an error message",
                  "INSERT INTO algorithm_runs (run_code, algorithm, purpose, status,"
                  " started_at, source_location_id, target_location_id)"
                  " VALUES ('BAD-RUN1', 'dijkstra', 'shortest_path', 'failed',"
                  " '2026-08-01 10:00:00', ?, ?)",
                  (loc_id, other_id), expect="ck_run_error")

    check_rejects(conn, "CHECK: shortest_path run missing an endpoint",
                  "INSERT INTO algorithm_runs (run_code, algorithm, purpose, status,"
                  " started_at, source_location_id) VALUES ('BAD-RUN3', 'a_star',"
                  " 'shortest_path', 'no_path', '2026-08-01 10:00:00', ?)",
                  (loc_id,), expect="ck_run_endpoints")

    check_rejects(conn, "CHECK: malformed JSON in parameters_json",
                  "INSERT INTO algorithm_runs (run_code, algorithm, purpose, status,"
                  " started_at, parameters_json) VALUES ('BAD-RUN2', 'hungarian',"
                  " 'resource_allocation', 'success', '2026-08-01 10:00:00', '{not json')",
                  expect="ck_run_params_json")

    check_rejects(conn, "CHECK: audit UPDATE missing before/after values",
                  "INSERT INTO audit_events (entity_table, entity_id, action)"
                  " VALUES ('locations', ?, 'UPDATE')", (loc_id,),
                  expect="ck_audit_update")

    check_rejects(conn, "CHECK: audit against a non-auditable table",
                  "INSERT INTO audit_events (entity_table, entity_id, action)"
                  " VALUES ('audit_events', 1, 'INSERT')", expect="ck_audit_table")

    # Must fail with a FOREIGN KEY error, not a CHECK error. See the docstring
    # on check_rejects() for why this distinction is worth asserting.
    check_rejects(conn, "RESTRICT: deleting a referenced location",
                  "DELETE FROM locations WHERE location_id = ?", (loc_id,),
                  expect="FOREIGN KEY")

    section("D. Referential actions behave as declared")

    # Every imported location is road-connected, so RESTRICT would always fire
    # first. Build an isolated location purely to exercise ON DELETE SET NULL,
    # then roll the whole thing back.
    conn.execute("BEGIN")
    conn.execute("INSERT INTO locations (location_code, name, category, latitude,"
                 " longitude) VALUES ('tmp-depot', 'Temp Depot', 'transport',"
                 " 5.6501, -0.1866)")
    tmp_id = conn.execute(
        "SELECT location_id FROM locations WHERE location_code = 'tmp-depot'").fetchone()[0]
    conn.execute("INSERT INTO resources (resource_code, name, resource_type,"
                 " base_location_id) VALUES ('TMP-1', 'Temp Van', 'shuttle_bus', ?)",
                 (tmp_id,))
    conn.execute("DELETE FROM locations WHERE location_id = ?", (tmp_id,))
    base_after = conn.execute(
        "SELECT base_location_id FROM resources WHERE resource_code = 'TMP-1'").fetchone()[0]
    check("ON DELETE SET NULL clears resources.base_location_id", base_after, None)
    still_there = conn.execute(
        "SELECT count(*) FROM resources WHERE resource_code = 'TMP-1'").fetchone()[0]
    check("resource row survives its base location's deletion", still_there, 1)
    conn.rollback()

    # ON UPDATE CASCADE: renumbering a location must follow through to roads.
    conn.execute("BEGIN")
    victim, new_id = conn.execute(
        "SELECT location_id, (SELECT max(location_id) + 1 FROM locations)"
        " FROM roads JOIN locations ON location_id = from_location_id LIMIT 1").fetchone()
    before = conn.execute(
        "SELECT count(*) FROM roads WHERE from_location_id = ?", (victim,)).fetchone()[0]
    conn.execute("UPDATE locations SET location_id = ? WHERE location_id = ?",
                 (new_id, victim))
    after = conn.execute(
        "SELECT count(*) FROM roads WHERE from_location_id = ?", (new_id,)).fetchone()[0]
    check("ON UPDATE CASCADE follows location_id into roads", after, before)
    conn.rollback()

    # The whole database must still be consistent after all that.
    check("foreign_key_check still clean after tests",
          conn.execute("PRAGMA foreign_key_check").fetchall(), [])

    conn.close()
    if not args.keep and os.path.exists(TEST_DB):
        os.remove(TEST_DB)

    # ------------------------------------------------------------- summary
    print("\n" + "=" * 74)
    print("%d passed, %d failed" % (len(PASS), len(FAIL)))
    if FAIL:
        print("\nFailures:")
        for name, got, want in FAIL:
            print("  - %s: got %r, want %r" % (name, got, want))
        return 1
    print("All checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
