# src/ — Java source code

All Java source for the system lives here:

- `main/java/model/`          — plain data classes (Location, Route, ServiceRequest, Resource, ...)
- `main/java/datastructures/` — the hand-written data structures (lists, stacks, queues, heap, trees, graph, hash table, disjoint set)
- `main/java/algorithms/`     — search, sort, graph, greedy, and dynamic-programming algorithms
- `main/java/database/`       — CSV loader, in-memory Repository, and the SQLite (JDBC) layer
- `main/java/performance/`    — the benchmark harness and the SVG chart writer
- `main/java/ui/`             — the interactive console application and its `main` entry point
- `test/java/`                — the JUnit 5 test suite

Nothing here depends on a build tool: everything compiles with plain `javac`
through `build.sh` / `build.ps1` at the project root. See the root `README.md`
for how to compile, test, and run.
