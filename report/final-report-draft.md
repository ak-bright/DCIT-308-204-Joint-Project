# Hospital & Clinic Operations Optimizer — Full Project Report (Draft)

> DCIT 204/308 Joint DSA Semester Project. This is the auto-assembled combined
> draft; the authoritative per-section sources are in `report/sections/`. See
> `report/README-for-documentation-team.md` for what is final vs. placeholder.

## Table of contents
1. Cover Page
2. Problem Statement, Assumptions, I/O, System Boundaries
3. Dataset Description, Data Dictionary, Database Schema
4. System Architecture and Module Design
5. Data-Structure Implementation
6. Algorithm Implementation
7. Correctness Evidence
8. Performance Analysis
9. Database Integration Evidence
10. Responsible Algorithm Selection
11. Individual Contribution Statements & Oral-Defense Prep
12. References and Appendices

---

# 1. Cover Page

<div align="center">

## Hospital & Clinic Operations Optimizer
### A Data-Structures & Algorithms System for a Ghanaian Hospital / Clinic Network

**Course:** DCIT 204 / 308 — Joint Data Structures & Algorithms Semester Project

**Local context:** A hospital/clinic network in Ghana. The system models the
network's departments and wards as *locations*, the corridors and connecting
routes between them as *routes*, the day-to-day patient/operational jobs as
*service requests*, and the staff, equipment, ambulances and beds as *resources*.
It helps operations staff answer everyday questions quickly: *what is the most
urgent job right now? what is the fastest way to move a patient from A to B?
which departments can a porter reach from here? what is the cheapest set of
corridors that keeps the whole site connected? who should be assigned to what?*

</div>

---

**Group project — 15 members.** (Names to be filled in by the Documentation Team.)

| Role | Member name |
|---|---|
| Group Leader & Sole Technical Developer | Bright |
| Data Collection Team (member 2) | ________________________ |
| Data Collection Team (member 3) | ________________________ |
| Data Collection Team (member 4) | ________________________ |
| Data Collection Team (member 5) | ________________________ |
| Database Team — data entry & formatting (member 6) | ________________________ |
| Database Team — data entry & formatting (member 7) | ________________________ |
| Documentation & Report Team (member 8) | ________________________ |
| Documentation & Report Team (member 9) | ________________________ |
| QA / Manual Testing Team (member 10) | ________________________ |
| QA / Manual Testing Team (member 11) | ________________________ |
| Presentation & Oral-Defense Team (member 12) | ________________________ |
| Presentation & Oral-Defense Team (member 13) | ________________________ |
| Logistics & Attendance Coordinator (member 14) | ________________________ |
| Logistics & Attendance Coordinator (member 15) | ________________________ |

**Submission date:** ____________________  **Academic year:** 2025/2026

> **Note for the Documentation Team:** every blank above is a placeholder. Fill in
> the 15 real names (Bright is already confirmed as Group Leader) and the two
> dates. See `report/README-for-documentation-team.md` for the full list of
> placeholders across the whole report.

---

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

---

# 3. Dataset Description, Data Dictionary, and Database Schema

## 3.1 Dataset description

The system works over four datasets describing the hospital network. Until the
Data Collection and Database Teams deliver the final data, the project ships a
**clearly-marked placeholder dataset** generated by
`database/SeedDataGenerator.java` (each CSV starts with a `# PLACEHOLDER` banner
line). The placeholder set exceeds the brief's minimums:

| Dataset | File | Rows (placeholder) | Brief minimum |
|---|---|---|---|
| Locations (departments/wards/service points) | `locations.csv` | 60 | 50 |
| Routes (corridors between locations) | `routes.csv` | 150 | 100 |
| Service requests (operational jobs) | `service-requests.csv` | 320 | 300 |
| Resources (staff/equipment/beds) | `resources.csv` | 36 | 30 |

The placeholder network is guaranteed **connected** (the generator lays down a
spanning tree first, then adds extra corridors), so every routing and reachability
feature has meaningful data to work on.

## 3.2 Data dictionary

**locations.csv**

| Column | Type | Meaning | Example |
|---|---|---|---|
| locationId | text (PK) | Stable identifier | `L001` |
| name | text | Human name | `Emergency Department` |
| area | text | Block / zone | `Surgical Block` |
| type | text | ward / department / unit | `department` |
| notes | text | Free text | `placeholder` |

**routes.csv**

| Column | Type | Meaning | Example |
|---|---|---|---|
| fromLocationId | text (FK→locations) | One end of the corridor | `L002` |
| toLocationId | text (FK→locations) | Other end | `L001` |
| distance | number (m) | Physical length — used by MST | `25` |
| travelTime | number (s) | Walking/porter time — used by Dijkstra | `47` |
| notes | text | Free text | `placeholder` |

**service-requests.csv**

| Column | Type | Meaning | Example |
|---|---|---|---|
| requestId | text (PK) | Identifier | `SR0001` |
| source | text (FK→locations) | Where the job starts | `L001` |
| destination | text (FK→locations) | Where it ends | `L022` |
| category | text | Kind of job | `blood-delivery` |
| urgency | int 1–5 | 1 = most urgent | `4` |
| timeSubmitted | text (ISO) | When raised | `2026-08-11T17:41` |
| deadline | text (ISO) | Soft target | `2026-08-11T18:49` |
| status | text | PENDING/ASSIGNED/DONE | `PENDING` |

**resources.csv**

| Column | Type | Meaning | Example |
|---|---|---|---|
| resourceId | text (PK) | Identifier | `R001` |
| type | text | nurse/porter/ambulance/… | `trolley` |
| homeLocation | text (FK→locations) | Base location | `L037` |
| capacity | int (min) | Work minutes available | `114` |
| availabilityStatus | text | AVAILABLE/BUSY/OFFLINE | `AVAILABLE` |

## 3.3 Database schema

The SQLite schema is the authoritative version in
[`database/schema.sql`](../../database/schema.sql). It has six tables: the four
entity tables above plus two append-only history tables. Summary:

- **`locations`** — PK `location_id`.
- **`roads`** — the corridors; auto-increment `road_id`, foreign keys `from_id` /
  `to_id` → `locations`, `UNIQUE(from_id, to_id)` so a corridor is stored once,
  `CHECK` constraints keeping distance/time ≥ 0, and indexes on both ends.
- **`service_requests`** — PK `request_id`, FK `source` / `destination`, a
  `CHECK (urgency BETWEEN 1 AND 5)`, plus stored `service_minutes` and
  `value_score` (the derived fields), and an index on `urgency`.
- **`resources`** — PK `resource_id`, FK `home_location`, `CHECK (capacity >= 0)`.
- **`algorithm_runs`** — auto-increment log of every algorithm execution
  (`algorithm`, `input_summary`, `elapsed_nanos`, `result_summary`, `run_at`).
- **`audit_events`** — auto-increment change trail (`event_type`, `entity`,
  `detail`, `occurred_at`).

An entity-relationship sketch (the Documentation Team can redraw this properly):

```
 locations 1───∞ roads            (from_id, to_id both reference locations)
 locations 1───∞ service_requests (source, destination reference locations)
 locations 1───∞ resources        (home_location references locations)
 algorithm_runs   (standalone history table)
 audit_events     (standalone history table)
```

Sample records appear in section 9 (Database Integration Evidence).

---

# 4. System Architecture and Module Design

## 4.1 Layered overview

The system is a single Java console application organised into clear layers. Data
flows up from storage into memory, is indexed by our own data structures, operated
on by our own algorithms, and shown through a text menu.

```
                 ┌───────────────────────────────────────────┐
                 │                ui  (console)               │
                 │   Main → ConsoleApp: menu, timing, I/O     │
                 └───────────────┬───────────────────────────┘
                                 │ calls
        ┌────────────────────────┼─────────────────────────────┐
        │                        │                              │
        ▼                        ▼                              ▼
 ┌──────────────┐        ┌───────────────┐             ┌────────────────┐
 │  algorithms  │        │   database    │             │     model      │
 │ search/sort/ │        │ Repository    │◄────────────│ Location,Route,│
 │ graph/greedy/│  uses  │ DataLoader    │  holds/     │ ServiceRequest,│
 │ DP           │───────►│ DatabaseMgr   │  returns    │ Resource, ...  │
 └──────┬───────┘        └──────┬────────┘             └────────────────┘
        │ built on              │ indexes with
        ▼                       ▼
 ┌───────────────────────────────────────────┐        ┌────────────────┐
 │             datastructures                 │        │   SQLite file  │
 │ DynamicArray, LinkedList, Stack, Queue,    │◄──JDBC─│ database/      │
 │ Deque, Heap, BST, AVL, BTree, HashTable,   │        │ hospital.db    │
 │ HashMap, HashSet, DisjointSet, Graph       │        └────────────────┘
 └───────────────────────────────────────────┘
```

## 4.2 Modules (Java packages)

- **`model`** — plain data holders: `Location`, `Route`, `ServiceRequest`,
  `Resource`, plus `AlgorithmRun` and `AuditEvent` for the history tables. No
  logic, so they read straight against the CSV columns.
- **`datastructures`** — the 13 from-scratch structures (section 5). Everything
  else is built on these; no `java.util` collection is used for assessed logic.
- **`algorithms`** — the from-scratch algorithms (section 6): `SearchAlgorithms`,
  `SortAlgorithms`, `GraphAlgorithms`, `GreedyAssignment`, `DynamicSelection`.
- **`database`** — persistence and the in-memory domain store: `DataLoader`
  (CSV ↔ memory), `DatabaseManager` (SQLite JDBC), `Repository` (holds the lists,
  builds the hash-table/tree indexes and the two graphs), and `SeedDataGenerator`.
- **`ui`** — `Main` (entry point) and `ConsoleApp` (the menu loop that wires it
  all together, times each algorithm, and logs runs).
- **`performance`** — `BenchmarkRunner` and `SvgChart` (the study in section 8).

## 4.3 Key design decisions

1. **Two graphs from one set of routes.** The `Repository` builds a *time-weighted*
   graph (for Dijkstra, BFS, DFS) and a *distance-weighted* graph (for Prim,
   Kruskal) from the same corridor list, so each question uses the right cost.
2. **Indexes are our own structures.** Look-up by id uses our `HashTable` (O(1)
   average); look-up/ordering by name uses our `BinarySearchTree` **and**
   `AVLTree` (kept side by side so the performance study can contrast them on real
   names). The "next request" uses our `BinaryHeap`.
3. **Memory is the source of truth; SQLite mirrors it.** The app loads seed CSVs
   (or an already-populated DB) into the `Repository`; edits update memory and are
   saved to SQLite. This keeps the assessed data structures central while still
   giving durable storage and a run/audit history.
4. **Timing and logging are cross-cutting.** Every algorithm menu option is wrapped
   in `System.nanoTime()` and written to `algorithm_runs`, which is where the
   report's performance evidence ultimately comes from.

---

# 5. Data-Structure Implementation

All 13 structures are hand-written in the `datastructures` package. None uses
`java.util.HashMap`, `TreeMap`, `PriorityQueue`, `Stack`, `ArrayDeque` or any
built-in equivalent — only plain arrays and (inside a couple of them) `java.util.List`/`Iterator`
as backing storage, as the brief permits. Each subsection says what it is, how it
works, its costs, and where the system uses it.

## 5.1 DynamicArray — `DynamicArray.java`
A resizable array. It keeps a plain `Object[]` block and a size; when full it
doubles, when a quarter full it halves. Operations: `add` (amortised O(1)),
`insert`/`remove` at an index (O(n) for the shift), `get`/`set` (O(1)).
**Used as** the workhorse backing store everywhere: the in-memory entity tables,
the graph's adjacency lists, and every result list the algorithms return.

## 5.2 DoublyLinkedList — `DoublyLinkedList.java`
Nodes with `prev`/`next` pointers and head/tail. `addFirst`/`addLast`/`insertAfter`
splice in O(1) once the node is found; `remove` unlinks in O(1). Iterable for
for-each. **Used as** the bucket chains inside the hash table (separate chaining),
and anywhere cheap end-insertion is wanted.

## 5.3 ArrayStack — `ArrayStack.java`
A LIFO stack on top of `DynamicArray`. `push`/`pop`/`peek` are O(1) at the array's
end; `isEmpty`. **Used by** the iterative Depth-First Search, so deep hospital
graphs cannot overflow the JVM call stack, and to reverse a Dijkstra path.

## 5.4 CircularQueue — `CircularQueue.java`
A ring buffer with `head`/`tail` indices that wrap using modular arithmetic;
it grows and re-linearises when full. `enqueue`/`dequeue` are O(1) with no element
shifting. **Used by** Breadth-First Search as its frontier queue.

## 5.5 LinkedQueue — `LinkedQueue.java`
The linked-list realisation of a FIFO queue (companion to the ring buffer), built
on `DoublyLinkedList`. `enqueue`/`dequeue`/`peek` O(1). **Used for** general
arrival-order buffering of requests before they are prioritised into the heap.

## 5.6 Deque — `Deque.java`
A double-ended queue as a wrap-around array: `addFront`/`addRear`/`removeFront`/
`removeRear` all O(1). **Used where** work may be pushed back to the front as well
as the back; it also demonstrates that one ring generalises both a stack and a
queue.

## 5.7 BinaryHeap (priority queue) — `BinaryHeap.java`
A complete binary tree embedded in an array (parent `i`, children `2i+1`/`2i+2`).
`insert` sifts up, `extractTop` sifts down (both O(log n)); a bottom-up
constructor **heapifies** an array in O(n). Ordered by a supplied comparator, so a
natural comparator gives a min-heap (extractMin) and a reversed one a max-heap
(extractMax). **Used as** the request dispatcher (min-heap on urgency) and inside
Dijkstra and Prim.

## 5.8 BinarySearchTree — `BinarySearchTree.java`
An ordered map of comparable keys → values. `insert`/`search` follow the
left-smaller/right-larger rule (average O(log n)); `inorderKeys` returns keys in
ascending order. It is deliberately **unbalanced** so the study can show it
degenerating to height n−1 on sorted input. **Used for** ordered/alphabetical
look-up of locations by name.

## 5.9 AVLTree — `AVLTree.java`
A self-balancing BST. After each insert it updates node heights and applies the
four rotations (LL, RR, LR, RL) wherever a subtree's balance factor leaves
{−1,0,+1}, guaranteeing height ≈ 1.44·log₂n and O(log n) search. **Used as** the
balanced counterpart to the plain BST — the benchmark inserts the same sorted keys
into both and measures the height/search-time gap.

## 5.10 BTree — `BTree.java`
A CLRS-style B-tree of minimum degree *t*: every node holds *t−1…2t−1* keys and the
tree stays perfectly height-balanced. `insert` splits a full child *before*
descending (proactive splitting), so only the root ever raises the height;
`search` is O(logₜ n). **Used as** a disk-style ordered index of records by id, and
it ties the project to the SQLite layer, whose own indexes are B-trees.

## 5.11 HashTable — `HashTable.java`
A hash table with **separate chaining**: an array of buckets, each a
`DoublyLinkedList` of `(key,value)` entries. A spread function scrambles poor
hash codes; the table resizes and rehashes when the load factor passes 0.75.
`put`/`get`/`remove` are average O(1). **Used as** the primary id→entity indexes
(locations, requests, resources) and the id→vertex-index map inside the graph.

## 5.12 HashMap & HashSet — `HashMap.java`, `HashSet.java`
Thin, clearly-named wrappers **built on our own `HashTable`** (not `java.util`).
`HashMap` gives map semantics (`put`/`get`/`remove`/`keys`); `HashSet` stores
membership as keys against a sentinel (`add`/`contains`/`remove`). **Used by** the
graph's id↔index map (HashMap) and the "visited"/"seen" sets in BFS/DFS and while
de-duplicating ids (HashSet).

## 5.13 DisjointSet (union-find) — `DisjointSet.java`
Integer union-find with **path compression** in `find` and **union by rank**,
giving near-constant O(α(n)) amortised operations. **Used by** Kruskal's MST: it
answers "are these two locations already connected?" so an edge is added only when
it joins two different components (no cycle).

## 5.14 Graph — `Graph.java`
A weighted graph keeping **both** representations in sync: an *adjacency list*
(`DynamicArray` of edges per vertex, space O(V+E), ideal for the sparse corridor
network and traversals) and an *adjacency matrix* (V×V weights, O(1) "is there a
direct corridor A→B?"). External string ids map to dense integer indices via our
`HashMap`. Undirected corridors are stored as two directed edges. **Used as** the
whole road network — the `Repository` builds one time-weighted and one
distance-weighted instance from the same routes.

---

*Correctness evidence (trace tables, invariants, rotation/split walk-throughs) for
the trees, heap and union-find appears in section 7; measured costs in section 8.*

---

# 6. Algorithm Implementation

All algorithms are hand-written in the `algorithms` package. For each one below:
a short plain-language description, pseudocode, a key Java snippet, and the Big-O.

## 6.1 Searching — `SearchAlgorithms.java`

**Linear search** — scan left to right. **Binary search** — repeatedly halve a
sorted interval.

```
binarySearch(a, target):
    lo ← 0; hi ← n-1
    while lo ≤ hi:
        mid ← lo + (hi-lo)/2
        if a[mid] = target: return mid
        else if a[mid] < target: lo ← mid+1
        else: hi ← mid-1
    return -1
```
```java
int mid = lo + (hi - lo) / 2;      // avoids integer overflow
int c = cmp.compare(a[mid], target);
if (c == 0) return mid;
else if (c < 0) lo = mid + 1; else hi = mid - 1;
```
**Big-O:** linear O(n); binary best O(1), average/worst O(log n). Binary requires
sorted input (see the invalid case in section 7).

## 6.2 Sorting — `SortAlgorithms.java`

| Algorithm | Idea | Best | Average | Worst | Stable |
|---|---|---|---|---|---|
| Selection | pick min of the suffix, swap into place | O(n²) | O(n²) | O(n²) | no |
| Insertion | grow a sorted prefix | O(n) | O(n²) | O(n²) | yes |
| Merge | divide, sort halves, merge | O(n log n) | O(n log n) | O(n log n) | yes |
| Quick | median-of-3 pivot, partition | O(n log n) | O(n log n) | O(n²) | no |

```
mergeSort(a, lo, hi):
    if lo ≥ hi: return
    mid ← (lo+hi)/2
    mergeSort(a, lo, mid); mergeSort(a, mid+1, hi)
    merge(a, lo, mid, hi)          # emit the smaller front element each step
```
```java
// merge step: pick the smaller of the two run fronts (ties take left => stable)
if (i > mid) a[k] = aux[j++];
else if (j > hi) a[k] = aux[i++];
else if (cmp.compare(aux[j], aux[i]) < 0) a[k] = aux[j++];
else a[k] = aux[i++];
```
Quicksort uses **median-of-three** pivoting and an **insertion-sort cutoff** for
small ranges, and recurses into the smaller side (looping on the larger) to bound
recursion depth to O(log n).

## 6.3 Graph traversal — BFS & DFS — `GraphAlgorithms.java`

BFS explores in rings using our `CircularQueue`; DFS dives deep using our
`ArrayStack` (iterative, not recursive). Both mark visited once.
```
BFS(g, s):
    visited[s] ← true; queue.enqueue(s)
    while queue not empty:
        u ← queue.dequeue(); output u
        for each edge (u→w): if not visited[w]: visited[w] ← true; queue.enqueue(w)
```
**Big-O:** O(V + E) time, O(V) space. The set BFS/DFS outputs *is* the set of
locations reachable from the dispatch point.

## 6.4 Dijkstra's shortest path — `GraphAlgorithms.dijkstra`

Greedily settle the closest unsettled vertex, relaxing its edges, using our
`BinaryHeap` as the priority queue (lazy deletion of stale entries).
```
dijkstra(g, s):
    dist[*] ← ∞; dist[s] ← 0; pq.insert((s,0))
    while pq not empty:
        (u,d) ← pq.extractMin(); if settled[u]: continue; settled[u] ← true
        for each edge (u→w, weight): if dist[u]+weight < dist[w]:
            dist[w] ← dist[u]+weight; prev[w] ← u; pq.insert((w, dist[w]))
```
**Big-O:** O((V + E) log V). Requires non-negative weights (travel times). Path is
rebuilt by following `prev[]` back from the target (empty if unreachable).

## 6.5 Minimum spanning tree — Prim & Kruskal

**Prim** grows a tree from vertex 0, always adding the cheapest edge that leaves
the current tree (min-heap of crossing edges) — O(E log V).
**Kruskal** sorts all edges and adds each one whose endpoints are in different
components, tested with our `DisjointSet` — O(E log E).
```java
// Kruskal core: union returns false if the two ends are already connected (cycle)
for (Graph.Edge e : sortedEdges)
    if (dsu.union(e.from, e.to)) { mst.add(e); if (mst.size()==V-1) break; }
```
Both compute the same minimum total weight (verified by a unit test); on a
disconnected graph they yield a spanning *forest*.

## 6.6 Greedy staff assignment — `GreedyAssignment.java`

Handle requests **most-urgent-first** (min-heap on urgency); give each the
available resource with the most remaining capacity that still fits it (best-fit),
subtracting the service time.
```
for each request in order of increasing urgency:
    pick AVAILABLE resource with max remaining capacity ≥ request.serviceMinutes
    if found: assign, capacity -= serviceMinutes   else: leave unassigned
```
**Big-O:** O(R log R + R·K) for R requests, K resources. Section 7 gives a worked
counterexample proving this greedy is **not** optimal for throughput.

## 6.7 Dynamic-programming request selection — `DynamicSelection.java`

Choose the subset of requests of maximum total value within a staff-minute budget
— the **0/1 knapsack**, solved by bottom-up tabulation.
```
dp[0][c] = 0
dp[i][c] = dp[i-1][c]                               if w_i > c
         = max(dp[i-1][c], v_i + dp[i-1][c - w_i])  otherwise
```
```java
dp[i][c] = dp[i - 1][c];                 // skip request i
if (w <= c) dp[i][c] = Math.max(dp[i][c], v + dp[i - 1][c - w]); // take it
```
**Big-O:** O(n·C) time and space (the full table is kept so the chosen set can be
reconstructed). A worked table appears in section 7; because the DP considers
*combinations* it beats the greedy dispatcher on the counterexample.

---

# 7. Correctness Evidence

This section gives the trace tables, loop invariants, proof sketches and
counterexamples required by the brief. Every claim here is also backed by an
automated test in `src/test/java/tests/` (60 tests, all passing).

## 7.1 Binary search — loop invariant + trace

**Invariant:** at the top of each loop, *if* `target` is in the array then its
index lies within `[lo, hi]`. Each step discards a half that cannot contain the
target, so the invariant is preserved and the interval strictly shrinks →
termination with a hit or `lo > hi`.

**Trace** — array `[1,3,5,7,9,11]`, target `9`:

| step | lo | hi | mid | a[mid] | action |
|---|---|---|---|---|---|
| 1 | 0 | 5 | 2 | 5 | 5 < 9 → lo = 3 |
| 2 | 3 | 5 | 4 | 9 | hit → return 4 |

## 7.2 Insertion sort — loop invariant + trace

**Invariant:** before processing index `i`, the prefix `a[0..i-1]` is a sorted
permutation of the original prefix. When the loop ends (`i = n`) the whole array
is sorted.

**Trace** — `[5, 2, 4, 1]`:

| i | key | array after inserting key |
|---|---|---|
| 1 | 2 | `[2, 5, 4, 1]` |
| 2 | 4 | `[2, 4, 5, 1]` |
| 3 | 1 | `[1, 2, 4, 5]` |

## 7.3 Merge sort — induction

**Base:** a range of length ≤ 1 is sorted. **Step:** assume both halves are sorted;
the linear merge always emits the smaller of the two front elements, so the output
is sorted and contains exactly the two halves' elements. By induction the whole
array is sorted. Merge is **stable** because ties take the left element first.

## 7.4 Quicksort — partition invariant

**Invariant (after partition around pivot p at final index k):** every element in
`a[lo..k-1] ≤ p ≤ a[k+1..hi]`, and `p` is at its final sorted position `k`.
Recursively sorting the two sides therefore sorts the whole. Median-of-three
pivoting makes the worst-case O(n²) input very unlikely, and recursing into the
smaller side keeps stack depth O(log n). *(Test `sortsAgree` confirms all four
sorts produce identical output; `sortsAdversarial` checks sorted/reverse input.)*

## 7.5 Dijkstra — greedy exchange argument + trace

**Why it is correct:** when a vertex `u` is first extracted from the min-heap, its
recorded distance is final. Suppose not — then some shorter path to `u` exists via
an unsettled vertex `x`; but `x` would have distance ≤ `dist[u]` and would have
been extracted first (all weights ≥ 0), a contradiction. Non-negative weights are
essential.

**Trace** — edges A–B 4, A–C 1, C–B 2, B–D 1; source A:

| settle | dist updates | frontier |
|---|---|---|
| A(0) | B←4, C←1 | {C:1, B:4} |
| C(1) | B←min(4, 1+2)=3 | {B:3, B:4(stale)} |
| B(3) | D←4 | {D:4, B:4(stale)} |
| D(4) | — | done |

Result: dist(D)=4, path A→C→B→D. *(Test `dijkstraNormal`; `dijkstraUnreachable`
checks an isolated vertex gets ∞ and an empty path.)*

## 7.6 Kruskal / Prim — cut & cycle properties

**Prim (cut property):** the cheapest edge crossing the cut between the growing
tree and the rest is always safe (belongs to some MST); adding such edges V−1
times yields an MST. **Kruskal (cycle property):** taking edges cheapest-first and
skipping any that would close a cycle (detected when union-find finds both ends in
one component) keeps only safe edges → an MST (a forest if disconnected).

**Trace (Kruskal)** — edges sorted: A-B 1, B-C 2, A-C 3, C-D 4:

| edge | ends already connected? | action | tree weight |
|---|---|---|---|
| A-B (1) | no | add | 1 |
| B-C (2) | no | add | 3 |
| A-C (3) | **yes** (A,C in {A,B,C}) | skip (cycle) | 3 |
| C-D (4) | no | add | 7 |

MST weight = 7. *(Test `mstAgrees` confirms Prim and Kruskal both give 7;
`kruskalForest` checks a disconnected graph gives a forest of V−component edges.)*

## 7.7 Dynamic programming — the knapsack table

Capacity 10; requests A(w10,v10), B(w5,v8), C(w5,v8):

| after \\ capacity c → | 0 | 5 | 10 |
|---|---|---|---|
| i=0 (none) | 0 | 0 | 0 |
| i=1 (A) | 0 | 0 | 10 |
| i=2 (B) | 0 | 8 | 10 |
| i=3 (C) | 0 | 8 | **16** |

`dp[3][10] = max(dp[2][10]=10, v_C + dp[2][5] = 8+8 = 16) = 16`. Back-tracking the
table selects **{B, C}**. *(Test `dpWorkedExample` asserts value 16, weight 10, two
items; `dpZeroCapacity` and `dpNegativeCapacity` cover the boundary/invalid cases.)*

## 7.8 Counterexample 1 — greedy is NOT throughput-optimal

Same numbers, objective = *complete as many requests as possible* with a single
team of capacity 10:

- **Greedy (urgency-first):** takes A (urgency 1, needs all 10 minutes) → B and C
  can't fit → **1 request completed**.
- **Optimal / DP:** takes B and C (5 + 5 = 10) → **2 requests completed**, value 16
  vs greedy's 10.

There is no exchange that lets urgency-first recover the second completion once A
is chosen, so the greedy choice is genuinely sub-optimal here. *(Test
`greedyCounterexample` asserts greedy completes 1 while the DP selects 2.)* Section
10 explains why we still, deliberately, use urgency-first for live triage.

## 7.9 Counterexample 2 — binary search on unsorted input (broken precondition)

Binary search's correctness depends on the array being sorted. On the **unsorted**
array `[5,1,3,2,4]`, searching for `1` (which is present at index 1):

| step | lo | hi | mid | a[mid] | action |
|---|---|---|---|---|---|
| 1 | 0 | 4 | 2 | 3 | 3 > 1 → hi = 1 |
| 2 | 0 | 1 | 0 | 5 | 5 > 1 → hi = −1 |

Loop ends → returns **−1 (not found)** even though `1` is present — linear search
finds it at index 1. This shows the precondition is not optional. *(Test
`binarySearchUnsorted` asserts linear finds it but binary misses it.)*

---

# 8. Performance Analysis

All numbers below are **real measurements** from running the project's own
benchmark harness (`bash build.sh bench`), averaged over 3 trials with a JIT
warm-up pass. The full method, every CSV, and the theory-vs-observed discussion
live in [`performance/interpretation.md`](../../performance/interpretation.md);
raw data is in `performance/experiment-results/` and charts (SVG) in
`performance/graphs/`. **These are not invented figures.** Absolute times depend on
the machine (here: Windows 11 laptop, OpenJDK 17); the curve *shapes* are the
point.

## 8.1 Sorting (`sorting.svg`)

| n | selection (ms) | insertion (ms) | merge (ms) | quick (ms) |
|---|---|---|---|---|
| 1,000 | 3.83 | 1.64 | 2.44 | 0.52 |
| 5,000 | 92.95 | 38.40 | 6.27 | 1.82 |
| 10,000 | 341.08 | 162.20 | 4.13 | 1.83 |

From ×10 the input (1k→10k) selection sort grew ~89× and insertion ~99× — the ×100
an O(n²) algorithm predicts — while merge/quick stayed in single-digit ms
(O(n log n)). By n=10,000 quicksort is ~185× faster than selection sort.

## 8.2 Searching (`searching.svg`)

| n | linear (ns/query) | binary (ns/query) |
|---|---|---|
| 100 | 1,171 | 243 |
| 10,000 | 10,727 | 210 |

Linear cost tracks n; binary stays flat near ~210 ns across a 100× range — the
signature of O(log n).

## 8.3 Hash table vs. load factor (`hashtable.svg`)

`get` stayed ~70–150 ns from load factor 0.25 up to 4.0 — near-constant, exactly
the O(1 + α) expectation for separate chaining. The live app keeps α ≤ 0.75 by
resizing.

## 8.4 BST vs. AVL — the headline experiment (`tree-height.svg`, `tree-search.svg`)

| n | BST height | AVL height | BST search (ns) | AVL search (ns) |
|---|---|---|---|---|
| 100 | 99 | 6 | 4,409 | 766 |
| 1,000 | 999 | 9 | 3,021 | 353 |
| 10,000 | 9,999 | 13 | 6,337 | 230 |

On sorted input the plain BST degenerates to height **exactly n−1** (a linked
list) while the AVL tree holds height ≈ log₂n (6→13 as n goes 100→10,000). AVL
search is 20–30× faster. This is the clearest demonstration of *why* self-balancing
matters.

## 8.5 Binary heap (`heap.svg`)

| n | build (ms) | dispatch (ms) |
|---|---|---|
| 1,000 | 0.18 | 0.39 |
| 10,000 | 2.06 | 5.99 |

Both grow ~n log n; dispatch is ~2–3× build because extract-min sifts a leaf all
the way down.

## 8.6 Graph algorithms (`graph.svg`)

| V | E | BFS | DFS | Dijkstra | Prim | Kruskal (ms) |
|---|---|---|---|---|---|---|
| 50 | 396 | 0.13 | 0.18 | 0.17 | 0.18 | 0.34 |
| 500 | 3,992 | 1.59 | 2.06 | 2.38 | 3.21 | 4.50 |

BFS/DFS grow ~linearly (O(V+E)); the heap-based Dijkstra/Prim a little faster than
linear (extra log V); Kruskal is consistently slowest because of its up-front
O(E log E) edge sort. The ordering BFS ≲ DFS ≲ Dijkstra ≲ Prim ≲ Kruskal held at
every size.

## 8.7 Conclusion

Across all six experiments the observed timings match the theoretical Big-O
classes; the only deviations were small constant-factor/warm-up effects at tiny
inputs and a few ms of measurement noise, both discussed in
`performance/interpretation.md`. Nothing observed contradicted the theory.

---

# 9. Database Integration Evidence

## 9.1 Engine and connection

Storage is **SQLite 3** — a single file, no server, zero setup — accessed over
JDBC (the `org.xerial:sqlite-jdbc` driver, the only third-party jar used by the
running system). The database lives at `database/hospital.db` and is created
automatically on first run. All the JDBC code is in
[`database/DatabaseManager.java`](../../src/main/java/database/DatabaseManager.java);
the schema is [`database/schema.sql`](../../database/schema.sql).

## 9.2 How load / save works

1. **Start-up (load).** `ConsoleApp` opens the DB and runs `schema.sql`
   (idempotent — `CREATE TABLE IF NOT EXISTS`). If the DB already holds data it is
   loaded into memory; otherwise the seed CSVs are loaded and **mirrored into the
   DB** in one transaction.
2. **Editing.** Adding/updating a location, route, request or resource updates the
   in-memory `Repository` and then calls `saveAll`, which rewrites the four entity
   tables inside a transaction so the DB always mirrors memory. Each change is
   written to `audit_events`.
3. **Save on demand.** Menu option 12 offers "save to DB", "reload from DB", and
   "export back to CSV".
4. **Run logging.** Every algorithm run is timed and appended to `algorithm_runs`.

## 9.3 Live row counts (from an actual run)

```
locations        = 60 rows
roads            = 150 rows
service_requests = 320 rows
resources        = 36 rows
algorithm_runs   = 3 rows
audit_events     = 3 rows
```

## 9.4 Sample records (queried from `hospital.db`)

**locations**

| location_id | name | area | type |
|---|---|---|---|
| L001 | Emergency Department | Surgical Block | department |
| L002 | Outpatient Department | Support Services | department |
| L003 | Radiology | Emergency Block | department |

**service_requests** (from the seed data)

| request_id | source | destination | category | urgency | status |
|---|---|---|---|---|---|
| SR0001 | L001 | L022 | blood-delivery | 4 | PENDING |
| SR0002 | L022 | L012 | medication-run | 4 | PENDING |

**resources**

| resource_id | type | home_location | capacity | availability_status |
|---|---|---|---|---|
| R001 | trolley | L037 | 114 | AVAILABLE |
| R002 | trolley | L049 | 38 | AVAILABLE |

## 9.5 Algorithm-run log (real captured evidence)

The `algorithm_runs` table after running a few features — this is where the
report's timing evidence originates (it is not hand-typed):

```
Lookup       | key=Radiology                 | 0.057 ms  | hit
DPKnapsack   | requests=320 budget=120       | 6.121 ms  | chosen=15 value=765
GreedyAssign | requests=320 resources=36     | 11.563 ms | assigned=108 unassigned=212
```

*(The Documentation Team may insert a screenshot of the console or a DB-browser
view here; the tables above are the exact data those screenshots would show.)*

## 9.6 Integrity features

- Foreign keys (`PRAGMA foreign_keys = ON`) tie roads/requests/resources back to
  real locations, with `ON DELETE CASCADE`/`SET NULL`.
- `CHECK` constraints enforce `urgency BETWEEN 1 AND 5`, non-negative
  distance/time/capacity.
- `UNIQUE(from_id, to_id)` stores each corridor once.
- Saves run inside a transaction (`commit`/`rollback`) so a failure never leaves
  the DB half-written. Integration tests (`DatabaseIntegrationTest`) prove a full
  save→load round-trip preserves every table.

---

# 10. Responsible Algorithm Selection

A fast algorithm is not automatically the *right* algorithm. In a hospital,
choosing a heuristic over an exact method — or vice versa — has real consequences.
This section states, plainly, where each style of algorithm is appropriate in this
system and where it is not.

## 10.1 Where a greedy / heuristic choice IS appropriate

- **Live triage ordering (priority heap).** Handling the most urgent request first
  is a greedy rule, and here it is exactly what we *want*: for patient-facing jobs,
  urgency must dominate, even if it lowers raw throughput. The min-heap gives an
  O(log n) "who's next" that matches clinical priority.
- **Everyday dispatch under time pressure.** The greedy staff-assignment
  (`GreedyAssignment`) produces a good, explainable assignment in milliseconds. For
  routine logistics (porter runs, sample deliveries) a near-optimal answer *now* is
  worth more than a perfect answer later.
- **Shortest path / MST.** Dijkstra and Prim/Kruskal are greedy but **provably
  optimal** for their problems (non-negative shortest path, minimum spanning tree),
  so using them is both fast and exactly correct — the ideal case.

## 10.2 Where a greedy shortcut is NOT appropriate

- **Never for clinical/triage decisions with patient-safety stakes.** The tool
  orders and routes *logistics*; it must not be repurposed to decide who receives
  care, in what order, clinically. Those are human decisions. A greedy score that
  looks reasonable can be badly wrong for an individual patient, and unlike a porter
  run, the cost of being wrong is not measured in minutes.
- **When the objective is a best *combination*, not a best *next step*.** Section
  7's counterexample shows the greedy dispatcher completing 1 request where the
  optimal set completes 2. When we genuinely need the best *bundle* of requests
  under a hard budget (e.g. planning a fixed overtime pool for the evening), we use
  the **dynamic-programming** selector (`DynamicSelection`), which considers
  combinations and is optimal — accepting its higher O(n·C) cost because the
  decision is planned, not split-second.
- **When weights can be negative.** Dijkstra's greedy correctness relies on
  non-negative edge costs; if a "cost" could ever be negative, Dijkstra would be the
  wrong tool and a different algorithm (e.g. Bellman–Ford) would be required.

## 10.3 The principle we followed

Match the algorithm to the **stakes and the objective**, not just the clock:

| Situation | Right choice | Why |
|---|---|---|
| "Who's next?" for urgent jobs | Greedy (priority heap) | Urgency *is* the correct objective |
| Fastest route, cheapest network | Greedy but exact (Dijkstra/Prim/Kruskal) | Provably optimal, also fast |
| Best bundle under a fixed budget | Dynamic programming | Optimal over combinations |
| Any patient-safety/triage judgement | **No algorithm** — human decision | Cost of error is not recoverable |

This is why the greedy dispatcher ships with an explicit worked counterexample in
its own source comments: so no future maintainer mistakes "fast and greedy" for
"always optimal", and so the tool is never quietly extended into decisions it
should not make.

---

# 11. Individual Contribution Statements & Oral-Defense Prep

## 11.1 Individual contribution statements (TEMPLATE — Documentation Team to fill)

> Collect a two-sentence statement from **every one of the 15 members** and paste
> it below, replacing each placeholder. Keep them factual (what the person actually
> did) and in the members' own words where possible.

| # | Member (name) | Role | Two-sentence contribution statement |
|---|---|---|---|
| 1 | Bright | Group Leader & Sole Technical Developer | *e.g. "I designed and implemented the entire Java system — the 13 data structures, the algorithms, the database layer, the tests and the benchmarks — and produced the first draft of this report. I coordinated the team and integrated everyone's data and feedback."* |
| 2 | ____________ | Data Collection | ______________________________________________ |
| 3 | ____________ | Data Collection | ______________________________________________ |
| 4 | ____________ | Data Collection | ______________________________________________ |
| 5 | ____________ | Data Collection | ______________________________________________ |
| 6 | ____________ | Database (entry & formatting) | ______________________________________________ |
| 7 | ____________ | Database (entry & formatting) | ______________________________________________ |
| 8 | ____________ | Documentation & Report | ______________________________________________ |
| 9 | ____________ | Documentation & Report | ______________________________________________ |
| 10 | ____________ | QA / Manual Testing | ______________________________________________ |
| 11 | ____________ | QA / Manual Testing | ______________________________________________ |
| 12 | ____________ | Presentation & Oral Defense | ______________________________________________ |
| 13 | ____________ | Presentation & Oral Defense | ______________________________________________ |
| 14 | ____________ | Logistics & Attendance | ______________________________________________ |
| 15 | ____________ | Logistics & Attendance | ______________________________________________ |

## 11.2 Oral-defense prep notes (plain-language one-liners)

Anyone on the team should be able to explain, in one sentence, what each piece
does. Use these as flash-cards.

**Data structures**
- **Dynamic array** — a list that automatically grows/shrinks its underlying array.
- **Linked list** — items joined by "next/previous" pointers; cheap to insert/remove.
- **Stack** — last-in-first-out, like a stack of plates (used by DFS).
- **Queue / circular queue** — first-in-first-out; the circular one reuses a fixed
  ring of slots (used by BFS).
- **Deque** — a queue you can add to and remove from at *both* ends.
- **Binary heap** — a tree-in-an-array that always gives the smallest (or largest)
  item fast; our "who's next?" priority queue.
- **Binary search tree** — sorted tree for name look-ups; can get lopsided.
- **AVL tree** — a search tree that *rebalances itself* so it never gets lopsided.
- **B-tree** — a "fat", shallow balanced tree; the shape real databases use for indexes.
- **Hash table** — near-instant look-up by key using a hash function + buckets.
- **Map / Set** — key→value and unique-membership, both built on our hash table.
- **Disjoint-set (union-find)** — tracks "which things are connected?" almost
  instantly; powers Kruskal.
- **Graph** — the map of the hospital: places (nodes) joined by corridors (edges),
  stored as both a list and a matrix.

**Algorithms**
- **Linear vs binary search** — check one by one, vs repeatedly halving a *sorted*
  list.
- **Selection/insertion/merge/quick sort** — four ways to order data; the first two
  are simple but slow (O(n²)), the last two are fast (O(n log n)).
- **BFS / DFS** — explore the network outward in rings / dive deep; both find what's
  reachable.
- **Dijkstra** — the fastest route between two places when corridors have travel
  times.
- **Prim / Kruskal** — the cheapest set of corridors that still connects every place.
- **Greedy assignment** — hand out staff most-urgent-first; fast but not always the
  best *combination*.
- **Dynamic programming (knapsack)** — pick the best *bundle* of jobs under a time
  budget; slower but optimal.

**Likely examiner questions (and short answers)**
- *Why implement your own structures?* To demonstrate understanding; the brief
  forbids `java.util` equivalents for the assessed logic.
- *Why AVL over a plain BST?* Sorted input makes a plain BST a slow linked list
  (height n−1); AVL stays at ≈log₂n — we measured exactly this (section 8.4).
- *Why does Dijkstra need non-negative weights?* Its greedy "first settled = final"
  argument breaks if a later negative edge could lower a settled distance.
- *When is greedy wrong here?* When we need the best *combination*, not the best
  next step — shown by the counterexample where greedy completes 1 job and the DP
  completes 2 (section 7.8).
- *Why SQLite?* One file, no server, real SQL and indexes — right-sized for a
  console tool.

---

# 12. References and Appendices

## 12.1 References

1. Cormen, T. H., Leiserson, C. E., Rivest, R. L., & Stein, C. (2009).
   *Introduction to Algorithms* (3rd ed.). MIT Press. — B-tree insertion/splitting,
   the 0/1-knapsack DP, Dijkstra, Prim, Kruskal, and union-find.
2. Sedgewick, R., & Wayne, K. (2011). *Algorithms* (4th ed.). Addison-Wesley. —
   quicksort with median-of-three, merge sort, heaps, and graph processing.
3. Adelson-Velsky, G. M., & Landis, E. M. (1962). *An algorithm for the
   organization of information.* — the original AVL self-balancing tree.
4. Dijkstra, E. W. (1959). *A note on two problems in connexion with graphs.*
   *Numerische Mathematik*, 1, 269–271.
5. SQLite Documentation. https://www.sqlite.org/docs.html — engine and SQL syntax.
6. xerial `sqlite-jdbc` driver. https://github.com/xerial/sqlite-jdbc — JDBC access.
7. JUnit 5 (Jupiter) User Guide. https://junit.org/junit5/docs/current/user-guide/
   — the test framework.
8. Oracle Java SE 17 API Documentation. https://docs.oracle.com/en/java/javase/17/

## 12.2 Appendix A — How to build and run

The project needs only a JDK (17+) — no Maven/Gradle.

```
bash scripts/fetch-deps.sh   # one-time: download the JUnit + SQLite jars into lib/
bash build.sh run            # compile and launch the console app
bash build.sh test           # compile and run the 60-test JUnit suite
bash build.sh bench          # run the performance benchmarks (writes CSVs + charts)
```
(Windows PowerShell users can use `.\build.ps1 run|test|bench` instead.)

To regenerate the placeholder dataset:
`java -cp out/main database.SeedDataGenerator`.

## 12.3 Appendix B — Repository map

```
src/main/java/model            plain data classes
src/main/java/datastructures   the 13 from-scratch structures
src/main/java/algorithms       search, sort, graph, greedy, DP
src/main/java/database         DataLoader, DatabaseManager, Repository, SeedDataGenerator
src/main/java/ui               Main, ConsoleApp (menu)
src/main/java/performance      BenchmarkRunner, SvgChart
src/test/java/tests            60 JUnit tests
database/                      schema.sql, seed-data/*.csv, hospital.db (generated)
performance/                   experiment-results/*.csv, graphs/*.svg, interpretation.md
report/                        this report (sections/ + final-report-draft.md)
```

## 12.4 Appendix C — Mapping brief requirements → code

| Requirement | Where |
|---|---|
| 13 data structures | `datastructures/` (section 5) |
| Search/sort/graph/greedy/DP | `algorithms/` (section 6) |
| Console behaviours (§3) | `ui/ConsoleApp.java` |
| schema + JDBC + run logging | `database/`, `database/schema.sql` |
| 40+ unit tests | `src/test/java/tests/` (60 tests) |
| Greedy counterexample | `GreedyAssignment` Javadoc + test `greedyCounterexample` |
| DP worked table | `DynamicSelection` Javadoc + test `dpWorkedExample` |
| Benchmarks + charts + interpretation | `performance/` |

## 12.5 Appendix D — Test summary

60 JUnit 5 tests across `DataStructuresTest`, `AlgorithmsTest`, and
`DatabaseIntegrationTest`, all passing. Each structure/algorithm is covered with a
normal case, a boundary case (empty/single/duplicate), and an invalid case
(out-of-range access, binary search on unsorted input, unreachable Dijkstra
target, disconnected-graph Kruskal forest, negative DP capacity), plus the greedy
counterexample and a SQLite save/load round-trip. Reproduce with `bash build.sh test`.

## 12.6 Appendix E — Placeholders still to resolve

See `report/README-for-documentation-team.md` for the authoritative list. In brief:
the 15 member names, the submission date, and the fact that the current dataset is
**placeholder** data to be swapped for the real collection.

---

