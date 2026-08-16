#!/usr/bin/env python3
"""
make_sample_data.py — development utility (NOT a project deliverable)
=============================================================================
Generates the sample CSVs in ./data/ so the schema and import script can be
tested end-to-end before Role 2 delivers the surveyed dataset.

  * locations.csv is REAL data, extracted from ../ug-campus-map/locations.js
    (the campus map's single source of truth for places).
  * roads.csv is DERIVED: a k-nearest-neighbour graph over those real
    coordinates, great-circle distance inflated by a winding factor because
    campus footpaths are not straight lines. Segment geometry is plausible,
    not surveyed.
  * resources / service_requests / algorithm_runs / audit_events are
    SYNTHETIC but realistic, generated from a fixed random seed so the row
    counts in db-documentation.md are reproducible.

Role 2 replaces these files with surveyed data using the same column headers
(the CSV contract documented in db-documentation.md). Nothing here is imported
by the application at runtime.

Usage:  python make_sample_data.py
"""

import csv
import json
import math
import os
import random
import re
from datetime import datetime, timedelta

HERE = os.path.dirname(os.path.abspath(__file__))
LOCATIONS_JS = os.path.join(HERE, "..", "ug-campus-map", "locations.js")
DATA_DIR = os.path.join(HERE, "data")

SEED = 20260804
random.seed(SEED)

# Fixed clock so regenerating the data does not churn every timestamp.
NOW = datetime(2026, 8, 4, 9, 0, 0)
TS = "%Y-%m-%d %H:%M:%S"


# ---------------------------------------------------------------------------
# 1. Extract the real locations out of locations.js
# ---------------------------------------------------------------------------

def extract_campus_locations(js_text):
    """Pull the CAMPUS_LOCATIONS array out of locations.js.

    locations.js is a browser script, not JSON, and its object literals use
    unquoted keys, so it cannot simply be json.loads()-ed. We slice out the
    array by tracking brace depth (ignoring braces inside string literals),
    then read the fields we need out of each object with targeted regexes.
    """
    start = js_text.index("const CAMPUS_LOCATIONS = [")
    start = js_text.index("[", start)

    depth, i, in_str, quote, esc = 0, start, False, "", False
    while i < len(js_text):
        ch = js_text[i]
        if in_str:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == quote:
                in_str = False
        else:
            if ch in "\"'":
                in_str, quote = True, ch
            elif ch == "[":
                depth += 1
            elif ch == "]":
                depth -= 1
                if depth == 0:
                    break
        i += 1
    array_src = js_text[start:i + 1]

    # Split the array into its top-level { ... } object literals.
    objects, depth, obj_start, in_str, quote, esc = [], 0, None, False, "", False
    for j, ch in enumerate(array_src):
        if in_str:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == quote:
                in_str = False
            continue
        if ch in "\"'":
            in_str, quote = True, ch
        elif ch == "{":
            if depth == 0:
                obj_start = j
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                objects.append(array_src[obj_start:j + 1])

    def field(src, key):
        """Read a double-quoted string field, honouring \\" escapes."""
        m = re.search(r'\b%s\s*:\s*"((?:[^"\\]|\\.)*)"' % key, src)
        if not m:
            return None
        return (m.group(1)
                .replace('\\"', '"')
                .replace("\\'", "'")
                .replace("\\n", " ")
                .replace("\\\\", "\\"))

    out = []
    for src in objects:
        loc_id = field(src, "id")
        name = field(src, "name")
        category = field(src, "category")
        coords = re.search(r"\bcoords\s*:\s*\[\s*(-?[\d.]+)\s*,\s*(-?[\d.]+)\s*\]", src)
        if not (loc_id and name and category and coords):
            continue
        desc = field(src, "description") or ""
        # Strip the HTML that some descriptions carry, and collapse whitespace.
        desc = re.sub(r"<[^>]+>", "", desc)
        desc = re.sub(r"\s+", " ", desc).strip()
        out.append({
            "location_code": loc_id.strip(),
            "name": name.strip(),
            "category": category.strip(),
            "latitude": round(float(coords.group(1)), 6),
            "longitude": round(float(coords.group(2)), 6),
            "description": desc,
            "is_active": 1,
        })
    return out


# ---------------------------------------------------------------------------
# 2. Build a road network over the real coordinates
# ---------------------------------------------------------------------------

def haversine_m(a, b):
    """Great-circle distance in metres between two {latitude, longitude}."""
    R = 6371000.0
    p1, p2 = math.radians(a["latitude"]), math.radians(b["latitude"])
    dp = p2 - p1
    dl = math.radians(b["longitude"] - a["longitude"])
    h = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * R * math.asin(math.sqrt(h))


def classify_road(dist_m, a, b):
    """Pick a plausible road class from the segment's length and endpoints."""
    residential = {"residences", "hostels"}
    if a["category"] in residential and b["category"] in residential and dist_m > 400:
        return "shuttle_route", "asphalt", 30.0
    if dist_m < 120:
        return "footpath", random.choice(["paved_stone", "concrete"]), 5.0
    if dist_m < 300:
        return "service", random.choice(["asphalt", "concrete"]), 20.0
    if dist_m < 700:
        return "minor", "asphalt", 30.0
    return "major", "asphalt", 50.0


def build_roads(locations, k=3):
    """k-nearest-neighbour graph, then force it into a single component.

    A disconnected graph would make most shortest-path test queries return
    'no path', which would be a poor exercise of the schema.
    """
    n = len(locations)
    dist = [[0.0] * n for _ in range(n)]
    for i in range(n):
        for j in range(i + 1, n):
            d = haversine_m(locations[i], locations[j])
            dist[i][j] = dist[j][i] = d

    edges = set()
    for i in range(n):
        nearest = sorted(range(n), key=lambda j: (dist[i][j] if j != i else 1e18))
        for j in nearest[:k]:
            edges.add((min(i, j), max(i, j)))

    # Union-find: stitch separate components together with their shortest link.
    parent = list(range(n))

    def find(x):
        while parent[x] != x:
            parent[x] = parent[parent[x]]
            x = parent[x]
        return x

    def union(x, y):
        rx, ry = find(x), find(y)
        if rx != ry:
            parent[ry] = rx
            return True
        return False

    for a, b in edges:
        union(a, b)

    while True:
        roots = {find(i) for i in range(n)}
        if len(roots) <= 1:
            break
        root_list = sorted(roots)
        base = root_list[0]
        members = [i for i in range(n) if find(i) == base]
        best = None
        for i in range(n):
            if find(i) == base:
                continue
            for m in members:
                if best is None or dist[i][m] < best[0]:
                    best = (dist[i][m], min(i, m), max(i, m))
        edges.add((best[1], best[2]))
        union(best[1], best[2])

    rows, seq = [], 0
    for a, b in sorted(edges):
        seq += 1
        straight = dist[a][b]
        # Campus paths bend around buildings: inflate the straight-line
        # distance by 15-35% so edge weights are not all trivially Euclidean
        # (which would make A* with a haversine heuristic look perfect).
        length = round(straight * random.uniform(1.15, 1.35), 1)
        road_type, surface, speed = classify_road(length, locations[a], locations[b])
        rows.append({
            "road_code": "RD-%04d" % seq,
            "name": "%s – %s Link" % (locations[a]["name"][:38], locations[b]["name"][:38]),
            "from_location_code": locations[a]["location_code"],
            "to_location_code": locations[b]["location_code"],
            "length_m": length,
            "road_type": road_type,
            "surface": surface,
            "is_bidirectional": 1,
            "speed_limit_kph": speed,
            "condition_rating": random.choices([1, 2, 3, 4, 5], [3, 8, 30, 40, 19])[0],
            "is_active": 1 if random.random() > 0.03 else 0,
        })
    return rows


# ---------------------------------------------------------------------------
# 3. Synthetic operational data
# ---------------------------------------------------------------------------

RESOURCE_SPECS = [
    ("maintenance_crew", "Maintenance Crew",  4, 45.00),
    ("security_patrol",  "Security Patrol",   2, 30.00),
    ("ambulance",        "Ambulance Unit",    3, 120.00),
    ("shuttle_bus",      "Campus Shuttle",   28, 85.00),
    ("sanitation_team",  "Sanitation Team",   5, 25.00),
    ("it_support",       "ICT Support Desk",  2, 40.00),
    ("equipment",        "Mobile Generator",  0, 15.00),
]


def build_resources(locations):
    # Base each resource at a category-appropriate location where possible.
    def pick(categories, fallback_idx):
        pool = [l for l in locations if l["category"] in categories]
        return random.choice(pool)["location_code"] if pool else locations[fallback_idx]["location_code"]

    prefs = {
        "ambulance": {"health"},
        "it_support": {"ict"},
        "shuttle_bus": {"transport", "landmarks"},
        "sanitation_team": {"food", "residences"},
        "security_patrol": {"offices", "landmarks"},
        "maintenance_crew": {"offices"},
        "equipment": {"offices", "ict"},
    }

    rows, seq = [], 0
    for rtype, label, cap, cost in RESOURCE_SPECS:
        for unit in range(1, random.randint(2, 4)):
            seq += 1
            status = random.choices(
                ["available", "deployed", "maintenance", "retired"],
                [58, 27, 12, 3])[0]
            rows.append({
                "resource_code": "RES-%03d" % seq,
                "name": "%s %d" % (label, unit),
                "resource_type": rtype,
                "base_location_code": pick(prefs[rtype], 0),
                "capacity": cap,
                "status": status,
                "cost_per_hour": cost,
                "contact_phone": "+233 30 250 %04d" % random.randint(1000, 9999),
                # ck_res_retired: a retired resource must not be active.
                "is_active": 0 if status == "retired" else 1,
            })
    return rows


REQUEST_TEMPLATES = [
    ("maintenance", "Broken ceiling fan reported in the main hall"),
    ("maintenance", "Cracked window pane on the first floor corridor"),
    ("sanitation",  "Overflowing waste bins at the entrance"),
    ("sanitation",  "Blocked drain after heavy rainfall"),
    ("security",    "Faulty security light at the rear car park"),
    ("security",    "Unattended bag reported by a student"),
    ("medical",     "Student reported feeling faint during lectures"),
    ("it_support",  "Wi-Fi access point offline since morning"),
    ("it_support",  "Projector not connecting in the lecture theatre"),
    ("transport",   "Shuttle stop signage damaged"),
    ("utilities",   "Intermittent power supply to the building"),
    ("utilities",   "No running water in the washrooms"),
]

# Which resource type is competent to handle each request category.
CATEGORY_TO_RESOURCE = {
    "maintenance": "maintenance_crew",
    "sanitation": "sanitation_team",
    "security": "security_patrol",
    "medical": "ambulance",
    "it_support": "it_support",
    "transport": "shuttle_bus",
    "utilities": "maintenance_crew",
}


def build_requests(locations, resources, count=60):
    usable = [r for r in resources if r["status"] != "retired"]
    by_type = {}
    for r in usable:
        by_type.setdefault(r["resource_type"], []).append(r["resource_code"])

    rows = []
    for seq in range(1, count + 1):
        category, desc = random.choice(REQUEST_TEMPLATES)
        loc = random.choice(locations)
        reported = NOW - timedelta(
            days=random.randint(0, 45),
            hours=random.randint(0, 23),
            minutes=random.randint(0, 59))
        status = random.choices(
            ["open", "assigned", "in_progress", "resolved", "cancelled"],
            [22, 15, 13, 43, 7])[0]

        # ck_req_assigned: assigned/in_progress rows MUST carry a resource.
        needs_resource = status in ("assigned", "in_progress")
        pool = by_type.get(CATEGORY_TO_RESOURCE[category], [])
        if needs_resource and not pool:
            status = "open"
            needs_resource = False
        resource_code = ""
        if needs_resource or (status == "resolved" and pool and random.random() < 0.9):
            resource_code = random.choice(pool) if pool else ""

        acknowledged, resolved = "", ""
        if status != "open":
            ack_dt = reported + timedelta(minutes=random.randint(5, 240))
            acknowledged = ack_dt.strftime(TS)
            # ck_req_resolved: a resolved row MUST carry resolved_at.
            if status in ("resolved", "cancelled"):
                res_dt = ack_dt + timedelta(hours=random.randint(1, 96))
                if status == "resolved":
                    resolved = res_dt.strftime(TS)

        rows.append({
            "request_code": "SR-2026-%04d" % seq,
            "location_code": loc["location_code"],
            "assigned_resource_code": resource_code,
            "category": category,
            "priority": random.choices(
                ["low", "medium", "high", "critical"], [28, 42, 22, 8])[0],
            "status": status,
            "description": desc,
            "reported_by": random.choice([
                "student.portal", "hall.porter", "security.desk",
                "faculty.officer", "facilities.hotline", "shuttle.driver"]),
            "reported_at": reported.strftime(TS),
            "acknowledged_at": acknowledged,
            "resolved_at": resolved,
        })
    return rows


def build_runs(locations, requests, roads, count=40):
    """Simulate routing/allocation runs, including realistic failures."""
    adjacency = {}
    for r in roads:
        if not r["is_active"]:
            continue
        adjacency.setdefault(r["from_location_code"], []).append(
            (r["to_location_code"], r["length_m"]))
        adjacency.setdefault(r["to_location_code"], []).append(
            (r["from_location_code"], r["length_m"]))

    def dijkstra(src, dst):
        """Real shortest path, so total_cost_m and path_json are consistent."""
        import heapq
        dist = {src: 0.0}
        prev, seen, pq, expanded, relaxed = {}, set(), [(0.0, src)], 0, 0
        while pq:
            d, u = heapq.heappop(pq)
            if u in seen:
                continue
            seen.add(u)
            expanded += 1
            if u == dst:
                break
            for v, w in adjacency.get(u, []):
                relaxed += 1
                nd = d + w
                if nd < dist.get(v, float("inf")):
                    dist[v] = nd
                    prev[v] = u
                    heapq.heappush(pq, (nd, v))
        if dst not in dist:
            return None, None, expanded, relaxed
        path, cur = [], dst
        while cur != src:
            path.append(cur)
            cur = prev[cur]
        path.append(src)
        path.reverse()
        return path, dist[dst], expanded, relaxed

    path_algos = ["dijkstra", "a_star", "bfs", "bellman_ford"]
    rows = []
    for seq in range(1, count + 1):
        started = NOW - timedelta(days=random.randint(0, 30),
                                  hours=random.randint(0, 23),
                                  minutes=random.randint(0, 59))
        # ~75% shortest-path runs, ~25% allocation runs.
        if random.random() < 0.75:
            src, dst = random.sample(locations, 2)
            algo = random.choice(path_algos)
            path, cost, expanded, relaxed = dijkstra(
                src["location_code"], dst["location_code"])
            runtime = round(random.uniform(0.4, 38.0), 3)
            if path is None:
                status, path_json, cost_val, err = "no_path", "", "", ""
            else:
                status, path_json = "success", json.dumps(path)
                cost_val, err = round(cost, 1), ""
            row = {
                "run_code": "RUN-%04d" % seq,
                "algorithm": algo,
                "purpose": "shortest_path",
                "request_code": "",
                "source_location_code": src["location_code"],
                "target_location_code": dst["location_code"],
                "parameters_json": json.dumps(
                    {"weight": "length_m", "avoid_inactive": True}),
                "status": status,
                "started_at": started.strftime(TS),
                "finished_at": (started + timedelta(
                    milliseconds=int(runtime))).strftime(TS),
                "runtime_ms": runtime,
                "nodes_expanded": expanded,
                "edges_relaxed": relaxed,
                "total_cost_m": cost_val,
                "path_json": path_json,
                "error_message": err,
            }
            # Occasionally record a genuine failure — ck_run_error demands a
            # message, which is exactly the constraint worth exercising.
            if random.random() < 0.08:
                row.update(status="failed", path_json="", total_cost_m="",
                           error_message="Edge weight overflow: negative length_m encountered",
                           finished_at="")
        else:
            req = random.choice(requests)
            row = {
                "run_code": "RUN-%04d" % seq,
                "algorithm": random.choice(["greedy_assignment", "hungarian"]),
                "purpose": "resource_allocation",
                "request_code": req["request_code"],
                "source_location_code": req["location_code"],
                "target_location_code": "",
                "parameters_json": json.dumps(
                    {"objective": "min_response_time", "max_candidates": 8}),
                "status": "success",
                "started_at": started.strftime(TS),
                "finished_at": (started + timedelta(seconds=1)).strftime(TS),
                "runtime_ms": round(random.uniform(0.2, 12.0), 3),
                "nodes_expanded": random.randint(4, 40),
                "edges_relaxed": random.randint(8, 160),
                "total_cost_m": round(random.uniform(120, 2400), 1),
                "path_json": "",
                "error_message": "",
            }
        rows.append(row)
    return rows


def build_audit(locations, roads, resources, requests, runs, count=80):
    actors = [
        ("dispatch.officer", "dispatcher"), ("facilities.admin", "admin"),
        ("gis.surveyor", "staff"), ("import.job", "system"),
        ("hall.porter", "staff"), ("student.portal", "student"),
    ]
    rows = []
    for _ in range(count):
        table = random.choices(
            ["service_requests", "resources", "roads", "locations", "algorithm_runs"],
            [45, 18, 14, 13, 10])[0]
        code_key = {
            "service_requests": ("request_code", requests),
            "resources": ("resource_code", resources),
            "roads": ("road_code", roads),
            "locations": ("location_code", locations),
            "algorithm_runs": ("run_code", runs),
        }[table]
        entity = random.choice(code_key[1])
        actor, role = random.choice(actors)
        occurred = NOW - timedelta(days=random.randint(0, 40),
                                   hours=random.randint(0, 23),
                                   minutes=random.randint(0, 59))

        if table == "service_requests":
            action = random.choices(
                ["STATUS_CHANGE", "ASSIGN", "UPDATE", "INSERT"], [40, 25, 20, 15])[0]
        else:
            action = random.choices(
                ["UPDATE", "INSERT", "STATUS_CHANGE", "DELETE"], [45, 30, 20, 5])[0]

        old_values, new_values, notes = "", "", ""
        if action == "UPDATE":
            # ck_audit_update: both sides are mandatory for an UPDATE.
            old_values = json.dumps({"condition_rating": 3})
            new_values = json.dumps({"condition_rating": 4})
            notes = "Routine condition re-survey"
        elif action == "STATUS_CHANGE":
            old_values = json.dumps({"status": "open"})
            new_values = json.dumps({"status": "assigned"})
            notes = "Dispatcher triage"
        elif action == "ASSIGN":
            new_values = json.dumps({"assigned_resource": random.choice(resources)["resource_code"]})
            notes = "Nearest available unit allocated"
        elif action == "INSERT":
            new_values = json.dumps({"created_via": "bulk_import"})
        else:
            old_values = json.dumps({"is_active": 1})
            notes = "Superseded record removed"

        rows.append({
            "entity_table": table,
            "entity_code": entity[code_key[0]],
            "action": action,
            "actor": actor,
            "actor_role": role,
            "occurred_at": occurred.strftime(TS),
            "old_values": old_values,
            "new_values": new_values,
            "notes": notes,
        })
    rows.sort(key=lambda r: r["occurred_at"])
    return rows


# ---------------------------------------------------------------------------
# 4. Write the CSVs
# ---------------------------------------------------------------------------

def write_csv(filename, rows, fieldnames):
    path = os.path.join(DATA_DIR, filename)
    with open(path, "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=fieldnames,
                                lineterminator="\n", quoting=csv.QUOTE_MINIMAL)
        writer.writeheader()
        writer.writerows(rows)
    print("  %-24s %4d rows" % (filename, len(rows)))


def main():
    os.makedirs(DATA_DIR, exist_ok=True)

    with open(LOCATIONS_JS, "r", encoding="utf-8") as fh:
        js = fh.read()
    locations = extract_campus_locations(js)
    if not locations:
        raise SystemExit("No locations extracted — check locations.js format.")

    lats = [l["latitude"] for l in locations]
    lngs = [l["longitude"] for l in locations]
    print("Extracted %d real locations from locations.js" % len(locations))
    print("  latitude  range: %.4f .. %.4f" % (min(lats), max(lats)))
    print("  longitude range: %.4f .. %.4f" % (min(lngs), max(lngs)))

    roads = build_roads(locations)
    resources = build_resources(locations)
    requests = build_requests(locations, resources)
    runs = build_runs(locations, requests, roads)
    audit = build_audit(locations, roads, resources, requests, runs)

    print("\nWriting CSVs to %s" % DATA_DIR)
    write_csv("locations.csv", locations, [
        "location_code", "name", "category", "latitude", "longitude",
        "description", "is_active"])
    write_csv("roads.csv", roads, [
        "road_code", "name", "from_location_code", "to_location_code",
        "length_m", "road_type", "surface", "is_bidirectional",
        "speed_limit_kph", "condition_rating", "is_active"])
    write_csv("resources.csv", resources, [
        "resource_code", "name", "resource_type", "base_location_code",
        "capacity", "status", "cost_per_hour", "contact_phone", "is_active"])
    write_csv("service_requests.csv", requests, [
        "request_code", "location_code", "assigned_resource_code", "category",
        "priority", "status", "description", "reported_by", "reported_at",
        "acknowledged_at", "resolved_at"])
    write_csv("algorithm_runs.csv", runs, [
        "run_code", "algorithm", "purpose", "request_code",
        "source_location_code", "target_location_code", "parameters_json",
        "status", "started_at", "finished_at", "runtime_ms", "nodes_expanded",
        "edges_relaxed", "total_cost_m", "path_json", "error_message"])
    write_csv("audit_events.csv", audit, [
        "entity_table", "entity_code", "action", "actor", "actor_role",
        "occurred_at", "old_values", "new_values", "notes"])
    print("\nDone (seed=%d)." % SEED)


if __name__ == "__main__":
    main()
