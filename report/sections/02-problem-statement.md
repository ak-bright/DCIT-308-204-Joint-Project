# 2. Problem Statement, Assumptions, Input/Output, System Boundaries

## 2.1 Problem statement

A busy hospital or clinic is, at its core, a network of places connected by
corridors, through which people, samples, medicines and equipment must constantly
move — and a stream of jobs (requests) competing for a limited pool of staff and
equipment. Doing this well by hand is hard: which of 300 pending jobs is most
urgent right now? What is genuinely the fastest way to wheel a patient from the
Emergency Department to Radiology when there are many possible corridors? If a
budget only allows a certain number of corridors to be kept lit and maintained,
which set keeps every department reachable at least cost?

The **Hospital & Clinic Operations Optimizer** is a Java console application that
answers exactly these questions using classic data structures and algorithms —
all implemented from scratch — over the network's real (or, until the data lands,
realistic placeholder) data.

## 2.2 Assumptions

1. **Corridors are two-way.** A route between two locations can be travelled in
   both directions, so the network is modelled as an *undirected* graph.
2. **Two independent costs per corridor.** Each route has a *distance* (metres)
   and a *travel time* (seconds). Fastest-route questions use travel time;
   cheapest-network questions use distance.
3. **Non-negative costs.** Distances and times are ≥ 0, which is what lets us use
   Dijkstra's algorithm for shortest paths.
4. **Urgency is a small integer**, 1 (most urgent, triage-critical) to 5
   (routine). The "next job" is always the lowest-numbered urgency.
5. **A resource has a capacity** measured in minutes of work available in the
   current planning window, and a status (AVAILABLE / BUSY / OFFLINE); only
   AVAILABLE resources can be assigned.
6. **Two derived fields** are computed for each request because they are not in
   the raw data: an estimated *service time* (from the job category) and a
   *value/benefit score* (higher for more urgent jobs). These feed the greedy and
   dynamic-programming features. The rule used is documented in the code
   (`database/Repository.deriveOne`).
7. **Single user, single site, in-memory + one SQLite file.** No concurrency, no
   network server — appropriate for a console tool.

## 2.3 Inputs

- **Seed data** in `database/seed-data/` as four CSV files: `locations.csv`,
  `routes.csv`, `service-requests.csv`, `resources.csv` (headers documented in
  section 3). These are loaded at start-up.
- **Interactive menu choices** from the operator: which algorithm to run and its
  parameters (e.g. a start and end location for a route, a staff-minute budget for
  the DP selection).

## 2.4 Outputs

- **On screen:** the most urgent request; a fastest route as an ordered list of
  locations with its total time; the set of reachable locations; a minimum-cost
  network (list of corridors + total distance); a greedy assignment of resources
  to requests; the best set of requests under a budget; and lookup results.
- **Persisted to SQLite** (`database/hospital.db`): the four entity tables, an
  append-only `algorithm_runs` log (every run's inputs, timing and result), and an
  `audit_events` change trail.
- **Benchmark artefacts** (from the performance harness): CSVs and charts under
  `performance/`.

## 2.5 System boundaries

**In scope:** modelling the network; the 13 data structures and the
search/sort/graph/greedy/DP algorithms; the console workflow; SQLite persistence;
timing/logging; benchmarking; and this report.

**Out of scope:** a graphical or web interface; real-time tracking of staff
location; live integration with a hospital's actual HIS/EMR systems; multi-user
access; authentication/authorisation; and any clinical/triage decision-making —
the tool *supports* operations logistics, it does **not** make patient-safety
decisions (see section 10, Responsible Algorithm Selection).
