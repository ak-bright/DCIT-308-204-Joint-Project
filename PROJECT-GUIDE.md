# Project Guide — Objectives & Walkthrough

This document explains **what the project sets out to do** and **what every file
does and how the pieces fit together**. It is meant to be readable by someone
seeing the code for the first time. For how to build and run, see [`README.md`](README.md).

---

## 1. Objectives

The project builds a **Hospital & Clinic Operations Optimizer**: a Java console
application that helps run the day-to-day logistics of a hospital / clinic network
in Ghana. Its objectives are:

1. **Model a real operational network.** Represent departments/wards as
   _locations_, the corridors between them as _routes_, operational jobs as
   _service requests_, and staff/equipment/beds as _resources_.

2. **Answer practical questions with classic algorithms**, all **implemented from
   scratch** (the course forbids using `java.util` collections for the assessed
   logic):
   - _Which job is most urgent?_ — a **binary heap** (priority queue).
   - _What is the fastest route from A to B?_ — **Dijkstra** on a travel-time graph.
   - _What can we reach from here?_ — **BFS / DFS**.
   - _What is the cheapest set of corridors that keeps everything connected?_ —
     **Prim / Kruskal** minimum spanning tree on a distance graph.
   - _Who should be assigned to which job?_ — a **greedy** assignment.
   - _What is the best bundle of jobs under a staff-time budget?_ — **dynamic
     programming** (0/1 knapsack).
   - _Look up a location/resource by id or name_ — a **hash table** and a
     **binary search tree**.

3. **Demonstrate a broad set of data structures** built by hand: dynamic array,
   doubly linked list, stack, queue, circular queue, deque, binary heap, binary
   search tree, AVL tree, B-tree, hash table (with map/set), disjoint-set, and graph.

4. **Persist and audit everything** through a real database: load/save the data in
   **SQLite** and log each algorithm run and each data change so results are
   reproducible, not invented.

5. **Prove it works and measure it**: a **JUnit 5** test suite (normal, boundary,
   and invalid cases) plus a **benchmark harness** that compares measured timings
   against the theoretical Big-O and draws its own SVG charts.

---

## 2. How the pieces connect (the big picture)

```
        database/seed-data/*.csv                database/schema.sql
                 │                                      │
                 ▼                                      ▼
          database/DataLoader ──► database/Repository ──► database/DatabaseManager
             (reads CSV)          (in-memory model +        (SQLite: load / save /
                                   our data structures)      log algorithm runs)
                                          │
                                          ▼
                                    ui/ConsoleApp  ◄── ui/Main (entry point)
                                    (the menu)
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    ▼                     ▼                     ▼
             datastructures/*       algorithms/*          model/* (data classes)
             (heap, graph,          (Dijkstra, BFS/DFS,   (Location, Route,
              trees, hash table)     Prim/Kruskal,         ServiceRequest,
                                     greedy, DP)           Resource, ...)
```

**Data flow in one sentence:** `Main` starts `ConsoleApp`, which uses `DataLoader`
to read the seed CSVs into a `Repository`; the `Repository` builds the from-scratch
data structures (hash tables, trees, graphs) over that data; the menu then runs the
hand-written `algorithms` against those structures, times each run, and asks
`DatabaseManager` to persist the data and log the run in SQLite.

---

## 3. File-by-file walkthrough

### Root

- **`README.md`** — how to install prerequisites, build, and run the project.
- **`PROJECT-GUIDE.md`** — this document (objectives + walkthrough).
- **`NOTES.md`** — assumptions and simplifications made while building the system.
- **`build.sh`** — Bash build script: `compile` / `test` / `run` / `bench`. Uses
  plain `javac`; no Maven/Gradle.
- **`build.ps1`** — the same build commands for Windows PowerShell.
- **`scripts/fetch-deps.sh`** — one-time download of the three helper JARs (JUnit
  runner + SQLite JDBC driver) into `lib/`. These are kept out of git.
- **`.gitignore`** — excludes compiled output, the generated `hospital.db`, and the
  downloaded JARs.

### `src/main/java/model/` — plain data classes (POJOs)

These are simple field holders that mirror the CSV columns, with no algorithm logic.

- **`Location.java`** — a place in the network (department/ward/service point). Graph _vertex_.
- **`Route.java`** — a corridor between two locations, carrying both a _distance_ (metres) and a _travelTime_ (seconds). Graph _edge_.
- **`ServiceRequest.java`** — a job to be done, with an _urgency_ (1 = most urgent) plus derived _serviceMinutes_ and _value_ used by greedy/DP.
- **`Resource.java`** — a worker/equipment/bed that can be _assigned_ to a request; has a capacity and an availability status.
- **`AlgorithmRun.java`** — an audit record of one algorithm execution (name, input summary, elapsed nanoseconds, result), saved to the `algorithm_runs` table.
- **`AuditEvent.java`** — a record of a data-changing action (create/update/load/save), saved to the `audit_events` table.

### `src/main/java/datastructures/` — the hand-written structures

- **`DynamicArray.java`** — a resizable array (like `ArrayList`); the project's default list, used almost everywhere.
- **`DoublyLinkedList.java`** — a doubly linked list with next/previous pointers.
- **`ArrayStack.java`** — a LIFO stack over an array; used by iterative DFS.
- **`LinkedQueue.java`** — a FIFO queue backed by a linked list.
- **`CircularQueue.java`** — a FIFO queue over a fixed ring of slots; used by BFS.
- **`Deque.java`** — a double-ended queue (add/remove at both ends).
- **`BinaryHeap.java`** — a binary min-heap (priority queue); powers "next most urgent request".
- **`BinarySearchTree.java`** — an ordered key→value tree for name lookups (can become unbalanced).
- **`AVLTree.java`** — a self-balancing search tree; stays shallow even on sorted input.
- **`BTree.java`** — a shallow, wide balanced tree of the kind databases use for indexes.
- **`HashTable.java`** — a hash table with separate chaining; the core of O(1) id lookups.
- **`HashMap.java`** — a key→value map built on `HashTable`.
- **`HashSet.java`** — a unique-membership set built on `HashTable`.
- **`DisjointSet.java`** — union-find over integer indices; powers Kruskal's cycle check.
- **`Graph.java`** — the network itself: vertices + weighted edges, stored as both an adjacency list and a matrix, shared by all the graph algorithms.

### `src/main/java/algorithms/` — the algorithms (all from scratch)

- **`SearchAlgorithms.java`** — linear search and binary search (generic over a `Comparator`).
- **`SortAlgorithms.java`** — selection, insertion, merge, and quick sort (the subject of the O(n²) vs O(n log n) study).
- **`GraphAlgorithms.java`** — BFS, DFS, Dijkstra (shortest path), and Prim/Kruskal (minimum spanning tree), built on our own graph, queue, stack, heap, and disjoint-set.
- **`GreedyAssignment.java`** — assigns available resources to requests most-urgent-first (fast, but not always the best _combination_).
- **`DynamicSelection.java`** — 0/1 knapsack by bottom-up dynamic programming: the best-value subset of requests within a staff-minute budget.

### `src/main/java/database/` — data loading, model, and persistence

- **`DataLoader.java`** — reads and writes the seed CSVs, skipping comment (`#`) and header lines and understanding quoted fields.
- **`Repository.java`** — the in-memory heart of the system. Holds the four entity tables and builds every derived structure: hash-table indexes by id, a BST **and** an AVL tree keyed by name, and two graphs (one weighted by travel time, one by distance). It also derives each request's `serviceMinutes` and `value`.
- **`DatabaseManager.java`** — the SQLite (JDBC) layer: creates the schema, loads/saves all tables, and logs algorithm runs and audit events.
- **`SeedDataGenerator.java`** — generates the coherent synthetic dataset (60 locations, 150 routes, 320 requests, 36 resources) from a fixed random seed and writes the four CSVs.

### `src/main/java/ui/` — the console application

- **`Main.java`** — the entry point; just constructs and runs `ConsoleApp`.
- **`ConsoleApp.java`** — owns the `Repository` and `DatabaseManager` and runs the numbered menu. Each option maps to one required behaviour, is **timed** with `System.nanoTime()`, and logs its run to the database.

### `src/main/java/performance/` — benchmarking

- **`BenchmarkRunner.java`** — runs each structure/algorithm at increasing input sizes, averages repeated trials (with a JIT warm-up), writes the timing CSVs, and calls `SvgChart` to draw the graphs.
- **`SvgChart.java`** — a tiny dependency-free SVG line-chart writer, so charts are produced straight from Java (no Python needed).

### `src/test/java/tests/` — the JUnit 5 test suite (60 tests)

- **`DataStructuresTest.java`** — each structure with a normal, a boundary (empty/single/duplicate), and an invalid case.
- **`AlgorithmsTest.java`** — search/sort/graph/greedy/DP correctness, including the greedy counterexample and a DP worked example.
- **`DatabaseIntegrationTest.java`** — the CSV loader, the `Repository` wiring, and a SQLite save→load round-trip.

### Supporting folders (non-code)

- **`data/`** — blank raw-data column templates (headers only) plus how index-number parameters are derived. See `data/README.md`.
- **`database/`** — `schema.sql` (the SQLite schema), `seed-data/*.csv` (the dataset the app loads), and `hospital.db` (generated at runtime, git-ignored).
- **`performance/`** — the benchmark output: `experiment-results/*.csv`, `graphs/*.svg`, and `interpretation.md` (theory vs measured).
- **`report/`** — the 12-section project report: `sections/` (one file per section) and `final-report-draft.md` (the combined draft).
- **`presentation/`** — slides notes and the oral-defense assignment.
- **`testing/`** — the manual QA checklist and results.
- **`logs/`** — attendance and weekly development logs.

---

## 4. A typical run, step by step

1. You run `bash build.sh run`. `build.sh` compiles `src/main/java` with `javac`
   and launches `ui.Main`.
2. `Main` creates `ConsoleApp` and calls `run()`.
3. `ConsoleApp` opens SQLite via `DatabaseManager` and initialises the schema. If
   the database is empty, it uses `DataLoader` to read `database/seed-data/*.csv`
   into a `Repository`, then mirrors that into the database.
4. Building the `Repository` fills the hash-table id indexes, the BST/AVL name
   trees, and the two graphs — all from the hand-written `datastructures`.
5. You pick a menu option. `ConsoleApp` calls the matching method in
   `algorithms` (e.g. `GraphAlgorithms.dijkstra`), times it, prints the result,
   and logs an `AlgorithmRun` to SQLite.
6. Data changes (add/update) go through the `Repository`, are saved to the
   database, and recorded as an `AuditEvent`.
7. You exit with `0`; `ConsoleApp` closes the database cleanly.
