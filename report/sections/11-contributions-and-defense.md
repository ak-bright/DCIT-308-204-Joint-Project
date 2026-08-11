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
