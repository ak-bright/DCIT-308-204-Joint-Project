# 11. Individual Contribution Statements & Oral-Defense Prep

## 11.1 Contributions by team

The project was delivered by role-based teams. Each team's contribution is
summarised below; individual member statements can be attached here at submission
time if the submission requires named statements.

| Team | Contribution |
|---|---|
| Group Leader & Technical Developer | Designed and implemented the entire Java system — the from-scratch data structures, the search/sort/graph/greedy/DP algorithms, the CSV + SQLite database layer, the JUnit test suite, and the performance benchmarks — and produced the first draft of this report. Coordinated the team and integrated everyone's data and feedback. |
| Data Collection Team | Defined and gathered the raw location, route, service-request and resource figures against the agreed column templates. |
| Database Team | Entered and formatted the data into clean CSVs matching the exact headers the loader expects. |
| Documentation & Report Team | Compiled, formatted and proofread this report and produced the final PDF/DOCX. |
| QA / Manual Testing Team | Worked through the console features, recorded results, and reported anything unexpected. |
| Presentation & Oral-Defense Team | Prepared the slides and demo and led the oral defence. |
| Logistics & Attendance Coordinator | Managed scheduling, communication and attendance records. |

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
