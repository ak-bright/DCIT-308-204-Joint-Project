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
