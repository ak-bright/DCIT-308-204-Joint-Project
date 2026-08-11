# Claude Code Prompt — Hospital & Clinic Operations Optimizer
Paste everything below into Claude Code, run from the root of this repository.

---

I'm building a Java console application for a university Data Structures & Algorithms
group project (DCIT 204/308, "Joint DSA Semester Project"). I'm the only technical
contributor on a 15-person team — everyone else has handled data collection, testing
feedback, and documentation review. I need you to build the entire technical system and
write the project's documentation and report.

## Local context
A Ghanaian hospital/clinic network. Real seed data (departments/wards as locations,
corridors/routes between them, patient service requests, and staff/equipment resources)
will be in `/database/seed-data/` — read whatever is there and use it as the actual
dataset; if a file is still empty, generate a realistic placeholder dataset in the same
format (50+ locations, 100+ routes, 300+ service requests, 30+ resources) so the system
is runnable while real data is being finalized, and clearly mark it as placeholder data
to be replaced.

## Hard constraints
- Java, no external DSA libraries. Do NOT use `java.util.HashMap`, `TreeMap`,
  `PriorityQueue`, `Stack`, `ArrayDeque`, or any other built-in implementation of a
  required data structure for the assessed core logic — implement every one of these
  from scratch. You may use plain arrays and `java.util.List`/arrays only as backing
  storage inside your own implementations.
- Follow the existing package layout: `src/main/java/datastructures`,
  `src/main/java/algorithms`, `src/main/java/database`, `src/main/java/model`,
  `src/main/java/ui`, `src/test/java`.
- Console menu only — no GUI required.

## 1. Data structures to implement from scratch (in `datastructures/`)
For each one, include a short Javadoc comment with its required operations and a
one-paragraph note on where it's used in the system:
1. Dynamic array / array-list — insert, get, set, remove, resize
2. Linked list (singly or doubly) — addFirst, addLast, insertAfter, remove, iterator
3. Stack — push, pop, peek, isEmpty
4. Queue and circular queue — enqueue, dequeue, wrap-around
5. Deque — addFront, addRear, removeFront, removeRear
6. Priority queue / binary heap — insert, extractMin/Max, heapify
7. Binary search tree — insert, search, inorder traversal
8. A self-balancing tree (red-black OR AVL) — insert with rotation/recolouring
9. B-tree — insert with node splitting, search
10. Hash table — put, get, remove, your own collision handling (chaining or open addressing)
11. Set and Map — built on your own hash table or BST, not `java.util`
12. Disjoint-set / union-find — makeSet, find (with path compression), union (by rank or size)
13. Graph — both adjacency-list and adjacency-matrix representations

## 2. Algorithms to implement from scratch (in `algorithms/`)
- Searching: linear search, binary search
- Sorting: selection sort, insertion sort, merge sort, quicksort
- Graph: BFS, DFS, Dijkstra's shortest path, Prim's MST, Kruskal's MST (using your
  disjoint-set)
- One greedy algorithm for a real operational decision — e.g. assigning available staff
  to pending requests by priority — including a code comment showing one worked example
  where the greedy choice is NOT optimal (a genuine counterexample, not hand-waved)
- One dynamic-programming algorithm — e.g. selecting the best set of requests to fulfil
  under a staffing/budget/time constraint (0/1-knapsack-style), with a comment showing
  the memoisation/tabulation table for one example run

For every algorithm, add a Javadoc block with Big-O time complexity (best/average/worst
where relevant) and a short correctness note (loop invariant, induction argument, or
greedy/DP exchange argument as appropriate).

## 3. System behaviour (wire these into `ui/` as console menu options)
- Add / view / update a location, route, service request, resource
- Show the next service request to handle, using the priority queue (by urgency)
- Find the fastest route between two locations (Dijkstra)
- Show which locations are reachable from a given dispatch point (BFS/DFS)
- Show a minimum-cost network connecting all locations (Prim or Kruskal)
- Run the greedy staff-assignment algorithm and show the result
- Run the DP request-selection algorithm under a given constraint and show the result
- Look up a location/resource by ID or name (hash table / BST)
- Load data from and save data to the database

## 4. Database integration (in `database/`)
- Design and write `database/schema.sql`: tables for `locations`, `roads`,
  `service_requests`, `resources`, `algorithm_runs`, `audit_events`, with sensible keys
  and foreign keys.
- Use SQLite for simplicity (single file, no server setup required) unless you find an
  existing choice elsewhere in the repo — if so, use that instead.
- Write the JDBC connection/load/save code in `database/`, reading from and writing to
  the seed data.
- Log each algorithm run (inputs, timing, result summary) to `algorithm_runs`.

## 5. Testing (in `src/test/java/`, using JUnit)
- At least 40 unit tests across the data structures and algorithms above, each covering
  a normal case, a boundary case (empty, single element, duplicate keys), and an invalid
  case (e.g. binary search on unsorted input, disconnected graph, unreachable path, full
  hash table).
- Include the greedy counterexample and one invalid-precondition case as explicit tests.
- All tests should pass; if something can't reasonably pass, fix the implementation
  rather than weakening the test.

## 6. Performance benchmarking (in `performance/`)
- Write a small benchmarking harness (can be a `main` method or a JUnit-adjacent runner,
  your choice) that times: search algorithms, sort algorithms, hash table operations at
  varying load factors, BST vs. your balanced tree (height and search time), heap
  dispatch, and the graph algorithms — at input sizes 100, 500, 1,000, 5,000, 10,000
  (and 50/100/200/500 for the graph experiments), each run averaged over 3 trials.
- Export raw results as CSV into `performance/experiment-results/`.
- Generate simple line/bar charts from the CSVs (a small Python script using
  matplotlib is fine, or any approach you prefer) into `performance/graphs/`.
- Write `performance/interpretation.md`: a short, plain-language comparison of the
  observed timings against the theoretical Big-O for each structure/algorithm, noting
  and explaining any mismatch.

## 7. Documentation and the final report — please generate this too
Once the code, tests, and performance results exist, write the full first draft of the
project report directly into `report/sections/` as separate Markdown files (one per
section, matching the numbering below), plus a combined `report/final-report-draft.md`.
A non-technical two-person Documentation Team will review and format this into Word
afterwards, so write it clearly enough for someone without a coding background to follow,
while still being technically accurate:

1. Cover page (title, local context, leave name placeholders for all 15 team members)
2. Problem statement, assumptions, input/output definitions, system boundaries
3. Dataset description, data dictionary, and the database schema (pull from schema.sql)
4. System architecture and module design (a short diagram description is fine, described
   in words or simple ASCII/mermaid — the Documentation Team will redraw it properly)
5. Data-structure implementation: one subsection per structure, explaining the design
   and how it's used, referencing the actual code
6. Algorithm implementation: pseudocode plus the key Java snippets for each algorithm,
   with the Big-O analysis
7. Correctness evidence: the trace tables, invariants, and proof sketches for the
   required algorithms (binary search, insertion sort, merge/quicksort, Dijkstra,
   Kruskal/Prim, the DP table), plus the two counterexamples
8. Performance analysis: pull in the real numbers from `performance/experiment-results/`
   and the interpretation from `performance/interpretation.md` — do not invent numbers
9. Database integration evidence: schema, a few sample records, and how load/save works
10. A short section on responsible algorithm selection — where a greedy/heuristic choice
    is appropriate for this hospital/clinic context and where it isn't (e.g. don't use a
    greedy shortcut for triage decisions with real patient-safety consequences)
11. Individual contribution statements — leave this as a template with one placeholder
    per member for the Documentation Team to fill in, plus general oral-defense prep
    notes summarising each data structure and algorithm in plain language
12. References and appendices

Also write `report/README-for-documentation-team.md`: a short plain-language note
telling the two-person Documentation Team exactly what's still a placeholder (team
member names, any placeholder dataset, anything that needs their judgement) versus
what's already final and shouldn't be changed.

## Working style
- Work through this in the order above (data structures → algorithms → system wiring →
  database → tests → performance → documentation), committing as you go so progress is
  visible in git history.
- If the real seed data in `/database/seed-data/` is still incomplete when you reach a
  step that needs it, use clearly-marked placeholder data and note in
  `report/README-for-documentation-team.md` that it needs to be swapped for the final
  dataset once the Database Team delivers it.
- Flag anything you had to assume or simplify due to ambiguity in this prompt, in a
  short `NOTES.md` at the repo root.
