# Hospital & Clinic Operations Optimizer

**DCIT 204/308 — Joint Data Structures & Algorithms Semester Project**

A Java console application for a Ghanaian hospital / clinic network. It models the
network's departments and wards as **locations**, the corridors between them as
**routes**, day-to-day jobs as **service requests**, and staff/equipment/beds as
**resources** — then answers everyday operational questions using data structures
and algorithms that are all **implemented from scratch** (no `java.util`
collections for the assessed logic):

- What is the most urgent job right now? _(priority queue / binary heap)_
- What is the fastest way to move a patient from A to B? _(Dijkstra)_
- Which locations can a porter reach from here? _(BFS / DFS)_
- What is the cheapest set of corridors that keeps everything connected? _(Prim / Kruskal)_
- Who should be assigned to which job? _(greedy assignment)_
- What is the best bundle of jobs under a staff-time budget? _(dynamic programming / knapsack)_
- Look up a location or resource by id or name _(hash table / binary search tree)_

All data is loaded from CSV, stored in an in-memory model built on the
hand-written data structures, and mirrored into a SQLite database.

---

## 1. Prerequisites

- **Java JDK 17 or newer** (the project is tested on OpenJDK 17). Check with:
  ```
  java -version
  javac -version
  ```
- **Bash** (Git Bash on Windows, or any Unix shell) to run `build.sh` and
  `fetch-deps.sh`. Windows users can alternatively use PowerShell with `build.ps1`.
- Internet access **the first time only**, to download three helper JARs
  (JUnit test runner + SQLite JDBC driver). These are not part of the assessed
  data-structure/algorithm code.

## 2. One-time setup — download the helper JARs

The three third-party JARs are kept out of git. Download them into `lib/` once:

```
bash scripts/fetch-deps.sh
```

(If you already have the JARs in `lib/`, you can skip this step.)

## 3. Build and run

The project compiles with plain `javac` — **no Maven or Gradle needed.** Use the
build script for your shell.

**macOS / Linux / Git Bash:**

```
bash build.sh run       # compile, then launch the interactive console app
bash build.sh test      # compile, then run the JUnit 5 test suite (60 tests)
bash build.sh bench     # compile, then run the performance benchmarks
bash build.sh compile   # compile only
```

**Windows PowerShell:**

```
.\build.ps1 run
.\build.ps1 test
.\build.ps1 bench
.\build.ps1 compile
```

Running `build.sh`/`build.ps1` with no argument compiles and runs the tests.

## 4. Using the app

`build.sh run` launches a numbered menu. On first start the app loads the dataset
from `database/seed-data/*.csv`, creates `database/hospital.db`, and shows how many
records were loaded. Pick a menu number to try each feature; every algorithm run is
timed and logged into the database (menu option 12 shows the run log).

To exit, choose `0` (or press Ctrl-D / end-of-input).

## 5. Regenerating the dataset (optional)

The dataset in `database/seed-data/` is a coherent **synthetic** dataset generated
from a fixed random seed, so it is stable and reproducible. To rebuild it:

```
bash build.sh compile
java -cp out/main database.SeedDataGenerator
```

To run the system on **real** figures instead, replace the four CSVs in
`database/seed-data/` (keeping the same column headers) and delete
`database/hospital.db` so it rebuilds from the new data.

---

## Folder map

- `src/` — the Java source (data structures, algorithms, database layer, console UI, tests)
- `data/` — blank raw-data column templates (see `data/README.md`)
- `database/` — SQLite schema, the seed CSVs, and the generated `hospital.db`
- `performance/` — benchmark results, SVG charts, and the theory-vs-observed write-up
- `report/` — the 12-section project report (`sections/` + combined draft)
- `presentation/`— slides, demo notes, and the oral-defense assignment
- `testing/` — the manual QA checklist and results
- `logs/` — attendance and weekly development logs
- `PROJECT-GUIDE.md` — objectives and a file-by-file walkthrough of the whole project
- `NOTES.md` — assumptions and simplifications made during development

## Team

The project is organised into role-based teams:

| Role / Team                        | Responsibility                                                  |
| ---------------------------------- | --------------------------------------------------------------- |
| Group Leader & Technical Developer | System design and the full Java implementation                  |
| Data Collection Team               | Gathering the raw location, route, request and resource figures |
| Database Team                      | Data entry and CSV formatting to the agreed column headers      |
| Documentation & Report Team        | Compiling, formatting and proofreading the report               |
| QA / Manual Testing Team           | Working through the console features and logging results        |
| Presentation & Oral-Defense Team   | Slides, demo, and defending the work                            |
| Logistics & Attendance Coordinator | Scheduling, communication and attendance records                |
