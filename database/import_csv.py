#!/usr/bin/env python3
"""
import_csv.py — load the Dataset Team's CSVs into the campus SQLite database
=============================================================================

    python import_csv.py                          # build campus.db from ./data
    python import_csv.py --data-dir ../role2/out  # load a different CSV folder
    python import_csv.py --db /tmp/test.db --quiet
    python import_csv.py --dry-run                # validate CSVs, import nothing

WHAT IT DOES
    1. Rebuilds the database from schema.sql (dropping any previous copy).
    2. Loads the six CSVs in foreign-key-safe order.
    3. Resolves NATURAL keys to SURROGATE keys. The CSVs reference rows by
       their human-readable code ('balme-library', 'RES-004'), never by
       location_id/resource_id — the Dataset Team cannot know the integer IDs
       the database will assign, and code-based references stay stable if the
       data is ever reloaded. This script builds code -> id maps as it goes.
    4. Converts '' to NULL and casts numeric columns, because csv gives back
       nothing but strings and the STRICT tables will reject a TEXT value in
       a REAL column.
    5. Runs inside ONE transaction: any bad row rolls the whole import back,
       so the database is never left half-populated.

EXIT CODES
    0 success   1 validation/import error   2 bad invocation
"""

import argparse
import csv
import os
import sqlite3
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_DB = os.path.join(HERE, "campus.db")
DEFAULT_SCHEMA = os.path.join(HERE, "schema.sql")
DEFAULT_DATA = os.path.join(HERE, "data")

INT, REAL, TEXT = "int", "real", "text"


class ImportAbort(Exception):
    """Raised for any problem that should abort the import."""


# ---------------------------------------------------------------------------
# The CSV contract. Changing anything here is an interface change that must be
# mirrored in db-documentation.md and agreed with the Dataset Team (Role 2).
#
#   columns  : csv_header -> (db_column, type, required)
#   refs     : csv_header -> (lookup_table, db_column) for natural-key columns
# ---------------------------------------------------------------------------
TABLES = [
    {
        "name": "locations",
        "file": "locations.csv",
        "key": "location_code",
        "columns": {
            "location_code": ("location_code", TEXT, True),
            "name":          ("name", TEXT, True),
            "category":      ("category", TEXT, True),
            "latitude":      ("latitude", REAL, True),
            "longitude":     ("longitude", REAL, True),
            "description":   ("description", TEXT, False),
            "is_active":     ("is_active", INT, False),
        },
        "refs": {},
    },
    {
        "name": "roads",
        "file": "roads.csv",
        "key": "road_code",
        "columns": {
            "road_code":          ("road_code", TEXT, True),
            "name":               ("name", TEXT, False),
            "from_location_code": ("from_location_id", INT, True),
            "to_location_code":   ("to_location_id", INT, True),
            "length_m":           ("length_m", REAL, True),
            "road_type":          ("road_type", TEXT, True),
            "surface":            ("surface", TEXT, False),
            "is_bidirectional":   ("is_bidirectional", INT, False),
            "speed_limit_kph":    ("speed_limit_kph", REAL, False),
            "condition_rating":   ("condition_rating", INT, False),
            "is_active":          ("is_active", INT, False),
        },
        "refs": {
            "from_location_code": ("locations", "from_location_id"),
            "to_location_code":   ("locations", "to_location_id"),
        },
    },
    {
        "name": "resources",
        "file": "resources.csv",
        "key": "resource_code",
        "columns": {
            "resource_code":      ("resource_code", TEXT, True),
            "name":               ("name", TEXT, True),
            "resource_type":      ("resource_type", TEXT, True),
            "base_location_code": ("base_location_id", INT, False),
            "capacity":           ("capacity", INT, False),
            "status":             ("status", TEXT, False),
            "cost_per_hour":      ("cost_per_hour", REAL, False),
            "contact_phone":      ("contact_phone", TEXT, False),
            "is_active":          ("is_active", INT, False),
        },
        "refs": {"base_location_code": ("locations", "base_location_id")},
    },
    {
        "name": "service_requests",
        "file": "service_requests.csv",
        "key": "request_code",
        "columns": {
            "request_code":           ("request_code", TEXT, True),
            "location_code":          ("location_id", INT, True),
            "assigned_resource_code": ("assigned_resource_id", INT, False),
            "category":               ("category", TEXT, True),
            "priority":               ("priority", TEXT, False),
            "status":                 ("status", TEXT, False),
            "description":            ("description", TEXT, False),
            "reported_by":            ("reported_by", TEXT, False),
            "reported_at":            ("reported_at", TEXT, False),
            "acknowledged_at":        ("acknowledged_at", TEXT, False),
            "resolved_at":            ("resolved_at", TEXT, False),
        },
        "refs": {
            "location_code":          ("locations", "location_id"),
            "assigned_resource_code": ("resources", "assigned_resource_id"),
        },
    },
    {
        "name": "algorithm_runs",
        "file": "algorithm_runs.csv",
        "key": "run_code",
        "columns": {
            "run_code":             ("run_code", TEXT, True),
            "algorithm":            ("algorithm", TEXT, True),
            "purpose":              ("purpose", TEXT, True),
            "request_code":         ("request_id", INT, False),
            "source_location_code": ("source_location_id", INT, False),
            "target_location_code": ("target_location_id", INT, False),
            "parameters_json":      ("parameters_json", TEXT, False),
            "status":               ("status", TEXT, False),
            "started_at":           ("started_at", TEXT, True),
            "finished_at":          ("finished_at", TEXT, False),
            "runtime_ms":           ("runtime_ms", REAL, False),
            "nodes_expanded":       ("nodes_expanded", INT, False),
            "edges_relaxed":        ("edges_relaxed", INT, False),
            "total_cost_m":         ("total_cost_m", REAL, False),
            "path_json":            ("path_json", TEXT, False),
            "error_message":        ("error_message", TEXT, False),
        },
        "refs": {
            "request_code":         ("service_requests", "request_id"),
            "source_location_code": ("locations", "source_location_id"),
            "target_location_code": ("locations", "target_location_id"),
        },
    },
    {
        "name": "audit_events",
        "file": "audit_events.csv",
        "key": None,          # append-only log; no natural key of its own
        "columns": {
            "entity_table": ("entity_table", TEXT, True),
            "entity_code":  ("entity_id", INT, True),
            "action":       ("action", TEXT, True),
            "actor":        ("actor", TEXT, False),
            "actor_role":   ("actor_role", TEXT, False),
            "occurred_at":  ("occurred_at", TEXT, False),
            "old_values":   ("old_values", TEXT, False),
            "new_values":   ("new_values", TEXT, False),
            "notes":        ("notes", TEXT, False),
        },
        # entity_code is polymorphic: resolved against whichever table
        # entity_table names. Handled specially in resolve_row().
        "refs": {},
    },
]


def log(msg, quiet=False):
    if not quiet:
        print(msg)


def coerce(value, kind, column, table, line_no):
    """Turn a CSV string into the Python type the STRICT column expects."""
    if value is None:
        return None
    value = value.strip()
    if value == "":
        return None
    if kind == TEXT:
        return value
    try:
        if kind == INT:
            return int(float(value)) if "." in value else int(value)
        return float(value)
    except ValueError:
        raise ImportAbort(
            "%s line %d: column '%s' expected %s but got %r"
            % (table, line_no, column, kind, value))


def validate_header(spec, header, path):
    """Every required column must be present; unknown columns are refused."""
    if header is None:
        raise ImportAbort("%s is empty (no header row)" % path)
    have = {h.strip() for h in header}
    want = set(spec["columns"])
    missing = sorted(c for c in want if c not in have
                     and spec["columns"][c][2])
    if missing:
        raise ImportAbort(
            "%s is missing required column(s): %s\n  expected header: %s"
            % (path, ", ".join(missing), ", ".join(spec["columns"])))
    unknown = sorted(have - want)
    if unknown:
        raise ImportAbort(
            "%s has unrecognised column(s): %s\n  expected header: %s"
            % (path, ", ".join(unknown), ", ".join(spec["columns"])))


def resolve_row(spec, row, line_no, id_maps):
    """Map one CSV row to {db_column: value}, resolving natural keys."""
    out = {}
    table = spec["name"]

    for csv_col, (db_col, kind, required) in spec["columns"].items():
        raw = row.get(csv_col)

        # Polymorphic audit reference: entity_code -> id in entity_table.
        if table == "audit_events" and csv_col == "entity_code":
            target = (row.get("entity_table") or "").strip()
            code = (raw or "").strip()
            if target not in id_maps:
                raise ImportAbort(
                    "audit_events line %d: entity_table %r is not an "
                    "auditable table" % (line_no, target))
            if code not in id_maps[target]:
                raise ImportAbort(
                    "audit_events line %d: %s %r not found — the audit log "
                    "references a row that is not in the import"
                    % (line_no, target, code))
            out[db_col] = id_maps[target][code]
            continue

        if csv_col in spec["refs"]:
            lookup_table, _ = spec["refs"][csv_col]
            code = (raw or "").strip()
            if code == "":
                if required:
                    raise ImportAbort(
                        "%s line %d: '%s' is required but empty"
                        % (table, line_no, csv_col))
                out[db_col] = None
                continue
            if code not in id_maps[lookup_table]:
                raise ImportAbort(
                    "%s line %d: '%s' references %s %r, which does not exist "
                    "in %s.csv" % (table, line_no, csv_col, lookup_table,
                                   code, lookup_table))
            out[db_col] = id_maps[lookup_table][code]
            continue

        value = coerce(raw, kind, csv_col, table, line_no)
        if value is None and required:
            raise ImportAbort(
                "%s line %d: '%s' is required but empty" % (table, line_no, csv_col))
        out[db_col] = value

    return out


def load_table(conn, spec, data_dir, id_maps, quiet, dry_run):
    path = os.path.join(data_dir, spec["file"])
    if not os.path.exists(path):
        raise ImportAbort("missing CSV: %s" % path)

    with open(path, "r", newline="", encoding="utf-8-sig") as fh:
        reader = csv.DictReader(fh)
        validate_header(spec, reader.fieldnames, path)

        rows, codes = [], []
        for line_no, row in enumerate(reader, start=2):
            if not any((v or "").strip() for v in row.values()):
                continue                                    # skip blank lines
            rows.append(resolve_row(spec, row, line_no, id_maps))
            if spec["key"]:
                codes.append((row[spec["key"]] or "").strip())

    if not rows:
        log("  %-18s 0 rows (empty)" % spec["name"], quiet)
        if spec["key"]:
            id_maps[spec["name"]] = {}
        return 0

    if dry_run:
        # Still populate the id map so downstream tables can be validated.
        if spec["key"]:
            id_maps[spec["name"]] = {c: i for i, c in enumerate(codes, start=1)}
        log("  %-18s %4d rows validated" % (spec["name"], len(rows)), quiet)
        return len(rows)

    db_cols = list(rows[0].keys())
    sql = "INSERT INTO %s (%s) VALUES (%s)" % (
        spec["name"], ", ".join(db_cols),
        ", ".join(":" + c for c in db_cols))

    cur = conn.cursor()
    for row, code in zip(rows, codes or [None] * len(rows)):
        try:
            cur.execute(sql, row)
        except sqlite3.IntegrityError as exc:
            raise ImportAbort(
                "%s: row %r rejected by the database — %s"
                % (spec["name"], code or row, exc))
        except sqlite3.InterfaceError as exc:
            raise ImportAbort(
                "%s: row %r has a value of the wrong type — %s"
                % (spec["name"], code or row, exc))

    # Build the code -> id map for tables that later tables reference.
    if spec["key"]:
        db_key = spec["columns"][spec["key"]][0]
        id_maps[spec["name"]] = dict(cur.execute(
            "SELECT %s, %s FROM %s" % (db_key, _pk_of(spec["name"]), spec["name"])
        ).fetchall())

    log("  %-18s %4d rows" % (spec["name"], len(rows)), quiet)
    return len(rows)


def _pk_of(table):
    return {
        "locations": "location_id",
        "roads": "road_id",
        "resources": "resource_id",
        "service_requests": "request_id",
        "algorithm_runs": "run_id",
        "audit_events": "event_id",
    }[table]


def create_database(conn, schema_path, quiet):
    if not os.path.exists(schema_path):
        raise ImportAbort("schema not found: %s" % schema_path)
    with open(schema_path, "r", encoding="utf-8") as fh:
        conn.executescript(fh.read())
    log("Schema applied from %s" % os.path.basename(schema_path), quiet)


def main(argv=None):
    ap = argparse.ArgumentParser(
        description="Load the Dataset Team's CSVs into the campus database.")
    ap.add_argument("--db", default=DEFAULT_DB, help="SQLite file to create")
    ap.add_argument("--schema", default=DEFAULT_SCHEMA, help="schema.sql path")
    ap.add_argument("--data-dir", default=DEFAULT_DATA, help="folder of CSVs")
    ap.add_argument("--dry-run", action="store_true",
                    help="validate the CSVs without writing a database")
    ap.add_argument("--quiet", action="store_true")
    args = ap.parse_args(argv)

    if not os.path.isdir(args.data_dir):
        print("error: --data-dir %s is not a directory" % args.data_dir,
              file=sys.stderr)
        return 2

    log("Campus database import", args.quiet)
    log("  database : %s" % (("(dry run — none)" if args.dry_run else args.db)),
        args.quiet)
    log("  csv dir  : %s\n" % args.data_dir, args.quiet)

    if args.dry_run:
        conn = sqlite3.connect(":memory:")
    else:
        if os.path.exists(args.db):
            os.remove(args.db)                     # schema.sql is authoritative
        conn = sqlite3.connect(args.db)

    total = 0
    try:
        # SQLite disables FK enforcement by default — without this the
        # REFERENCES clauses in schema.sql would be silently decorative.
        conn.execute("PRAGMA foreign_keys = ON")
        if not args.dry_run:
            create_database(conn, args.schema, args.quiet)

        log("\nLoading CSVs (foreign-key-safe order):", args.quiet)
        id_maps = {}
        with conn:                                  # commit, or roll back
            for spec in TABLES:
                total += load_table(conn, spec, args.data_dir, id_maps,
                                    args.quiet, args.dry_run)

            if not args.dry_run:
                bad = conn.execute("PRAGMA foreign_key_check").fetchall()
                if bad:
                    raise ImportAbort(
                        "foreign key check failed on %d row(s): %r"
                        % (len(bad), bad[:5]))

    except ImportAbort as exc:
        print("\nIMPORT FAILED — no changes committed\n  %s" % exc,
              file=sys.stderr)
        conn.close()
        if not args.dry_run and os.path.exists(args.db):
            os.remove(args.db)
        return 1
    except sqlite3.Error as exc:
        print("\nIMPORT FAILED — database error\n  %s" % exc, file=sys.stderr)
        conn.close()
        if not args.dry_run and os.path.exists(args.db):
            os.remove(args.db)
        return 1

    if not args.dry_run:
        conn.execute("ANALYZE")
        conn.commit()
    conn.close()

    log("\n%s: %d rows across %d tables."
        % ("Validated" if args.dry_run else "Imported", total, len(TABLES)),
        args.quiet)
    if not args.dry_run:
        log("Database written to %s" % args.db, args.quiet)
    return 0


if __name__ == "__main__":
    sys.exit(main())
