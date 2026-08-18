# NOTES — assumptions & simplifications

Things I had to assume or simplify because the brief or the data left them open.
Recorded here so the team (and examiner) can see the reasoning.

## Data
- **The seed data is a generated synthetic dataset.** `/data/` holds only the
  empty column templates, so the dataset the system runs on is produced by
  `database.SeedDataGenerator`: a coherent, realistic hospital network of 60
  locations, 150 routes, 320 requests, and 36 resources. It is built from a fixed
  random seed, so regenerating always gives the identical files. To drive the
  system from real figures instead, replace the CSVs in `/database/seed-data/`
  with real data using the same headers. The loader skips `#` comment and blank
  lines, so notes can be added at the top of any CSV.
- **Two derived request fields.** The raw request CSV has no "service time" or
  "value/benefit", but the greedy and DP features need them. I derive
  `serviceMinutes` from the request category and `value` from urgency
  (`Repository.deriveOne`). If the real data includes these, prefer the real values.

## Modelling choices
- **Corridors are undirected**; each route becomes two directed graph edges.
- **Two separate graphs** are built from the same routes — one weighted by travel
  time (Dijkstra/BFS/DFS), one by distance (Prim/Kruskal) — rather than one graph
  with dual weights, because it keeps each algorithm's cost function unambiguous.
- **Urgency is 1–5**, 1 = most urgent; the heap is a min-heap on urgency.

## Data-structure / algorithm scope
- **"Queue and circular queue"** is delivered as two classes: `CircularQueue`
  (array ring, used by BFS) and `LinkedQueue` (linked-list FIFO), to show both
  realisations.
- **Set and Map** are `HashSet`/`HashMap` built on our own `HashTable` (the brief
  allowed hash-table-or-BST backing).
- **Self-balancing tree** = AVL (brief allowed AVL or red-black; AVL is simpler to
  explain and to prove balanced).
- **Hash table uses separate chaining**, so it never becomes "full" (it resizes);
  the brief's "full hash table" invalid case applies to open addressing. I instead
  test heavy-collision retrieval and absent-key lookups as the boundary/invalid
  cases, and note this in the tests.
- **Disjoint-set is integer-indexed** (0..n-1), which is all Kruskal needs; it is
  not generic.
- **`Graph.addVertex` rebuilds the adjacency matrix** on each new vertex (O(V²)
  each). Fine for this project's sizes (≤ 500 vertices in benchmarks); a production
  version would grow the matrix in blocks.

## Tooling
- **No Maven/Gradle** was available, so the project compiles with plain `javac`
  via `build.sh` / `build.ps1`. The three third-party jars (JUnit console, SQLite
  JDBC, SLF4J) are downloaded by `scripts/fetch-deps.sh` and are git-ignored — none
  is a DSA library.
- **Charts are generated as SVG from Java** (`performance/SvgChart`) because Python/
  matplotlib is not guaranteed in the marking environment. An optional
  `performance/plot.py` is included for anyone who prefers matplotlib PNGs.
- **Sources use some Unicode in comments** (×, ≈, α, …); compile with
  `-encoding UTF-8` (the build scripts already do). Console output is ASCII-only to
  avoid Windows code-page mojibake.

## Deliberately out of scope
GUI/web UI, authentication, concurrency/multi-user, live hospital-system
integration, and any clinical/triage decision-making (the tool supports logistics
only — see report section 10).
