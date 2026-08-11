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
