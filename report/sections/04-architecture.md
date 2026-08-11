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
