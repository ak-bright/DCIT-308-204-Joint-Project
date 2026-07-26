# UG Legon Campus Service Hub Optimizer
DCIT 204/308 — Joint DSA Semester Project
Local context: University of Ghana, Legon — campus service hub (hostels, lecture halls, labs, shuttle stops, maintenance requests) &nbsp;·&nbsp; Duration: 10 days

## What this is
See `/report/` for the full write-up. This repo is organised so each team role
(from the "Team Role Distribution & Work Instructions" document) has one clear
folder to drop its deliverable into. Do not scatter files outside your assigned
folder — Team Leader builds the Java system from what's in here.

## Folder map
- `src/` — the actual Java system (Team Lead + Co-Developer own this)
- `data/` — localization & dataset team's CSVs and evidence note
- `database/` — schema + seed/import scripts (Database Lead)
- `specs/` — data-structure and algorithm specs that get turned into code
- `testing/` — test plan, JUnit tests, trace tables, proof sketches
- `performance/` — experiment plan, raw results, graphs
- `report/` — the evolving final report
- `presentation/` — slides, demo video, oral-defense assignment
- `logs/` — weekly development log (required submission item)

## How to run
(Fill in once the console entry point exists, e.g.)
```
javac -d out $(find src/main/java -name "*.java")
java -cp out ui.Main
```

## Team
| Role | Member |
|---|---|
| Team Lead & Lead Developer | Team Leader |
| Co-Developer (Claude Code support) | ______________________ |
| Localization & Dataset Team | ______________________ / ______________________ |
| Database Schema & Integration Lead | ______________________ |
| Data Structures Spec Team | ______________________ / ______________________ |
| Algorithms Spec & Analysis Team | ______________________ / ______________________ |
| Testing & Correctness Lead | ______________________ |
| Performance & Empirical Analysis Lead | ______________________ |
| Report & Documentation Lead | ______________________ |
| Presentation, Demo & Oral Defense Coordinator | ______________________ |
