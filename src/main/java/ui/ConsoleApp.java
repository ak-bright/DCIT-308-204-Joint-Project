package ui;

import algorithms.DynamicSelection;
import algorithms.GraphAlgorithms;
import algorithms.GreedyAssignment;
import database.DataLoader;
import database.DatabaseManager;
import database.Repository;
import datastructures.DynamicArray;
import datastructures.Graph;
import model.AlgorithmRun;
import model.AuditEvent;
import model.Location;
import model.Resource;
import model.Route;
import model.ServiceRequest;

import java.util.Scanner;

/**
 * The interactive console application: owns the {@link Repository} (in-memory
 * data + our data structures), the {@link DatabaseManager} (SQLite), and the
 * text menu that exposes every required system behaviour.
 *
 * <p>Each algorithm option is <b>timed</b> with {@code System.nanoTime()} and the
 * timing/result is logged to the {@code algorithm_runs} table, so the report can
 * show a real run history rather than invented numbers. Data-changing actions are
 * logged to {@code audit_events}.</p>
 *
 * <p>The menu maps one-to-one to brief §3:
 * add/view/update entities | next request (heap) | fastest route (Dijkstra) |
 * reachability (BFS/DFS) | cheapest network (Prim/Kruskal) | greedy assignment |
 * DP selection | lookup by id/name (hash table / BST) | load/save database.</p>
 */
public final class ConsoleApp {

    private static final String SEED_DIR   = "database/seed-data";
    private static final String DB_PATH    = "database/hospital.db";
    private static final String SCHEMA_SQL = "database/schema.sql";

    private final Scanner in = new Scanner(System.in);
    private Repository repo = new Repository();
    private DatabaseManager db;

    public void run() {
        banner();
        openDatabase();
        bootstrapData();
        loop();
        shutdown();
    }

    // --------------------------------------------------------------------
    // Startup / shutdown
    // --------------------------------------------------------------------

    private void banner() {
        System.out.println("============================================================");
        System.out.println("  Hospital & Clinic Operations Optimizer  (DCIT 204/308)");
        System.out.println("  Ghanaian hospital/clinic network - console edition");
        System.out.println("============================================================");
    }

    private void openDatabase() {
        try {
            db = new DatabaseManager(DB_PATH);
            db.initSchema(SCHEMA_SQL);
            System.out.println("[db] SQLite ready at " + DB_PATH);
        } catch (RuntimeException e) {
            System.out.println("[db] WARNING: database unavailable (" + e.getMessage() + ").");
            System.out.println("     The app will still run from CSV, but load/save will be disabled.");
            db = null;
        }
    }

    /**
     * Decide where the initial data comes from: prefer the SQLite DB if it is
     * already populated; otherwise load the seed CSVs and mirror them into the DB.
     */
    private void bootstrapData() {
        boolean loadedFromDb = false;
        if (db != null && db.countRows("locations") > 0) {
            repo = db.loadAll();
            loadedFromDb = true;
            System.out.println("[data] Loaded existing data from the database.");
        } else {
            DataLoader dl = new DataLoader(SEED_DIR);
            if (dl.seedExists()) {
                repo.loadFromCsv(SEED_DIR);
                System.out.println("[data] Loaded seed CSVs from " + SEED_DIR);
                if (db != null) { db.saveAll(repo); System.out.println("[data] Mirrored seed data into the database."); }
            } else {
                System.out.println("[data] No seed data found. Add records via the menu, or run "
                        + "database.SeedDataGenerator to create placeholder data.");
            }
        }
        printSummary();
        if (!loadedFromDb) noteAudit("LOAD", "Database", "Initial bootstrap load");
    }

    private void printSummary() {
        System.out.printf("[data] %d locations | %d routes | %d requests | %d resources%n",
                repo.locations().size(), repo.routes().size(), repo.requests().size(), repo.resources().size());
    }

    private void shutdown() {
        if (db != null) db.close();
        System.out.println("Goodbye.");
    }

    // --------------------------------------------------------------------
    // Main menu loop
    // --------------------------------------------------------------------

    private void loop() {
        while (true) {
            System.out.println();
            System.out.println("---------------------------- MENU --------------------------");
            System.out.println(" 1) Locations   - add / view / update");
            System.out.println(" 2) Routes      - add / view");
            System.out.println(" 3) Requests    - add / view / update status");
            System.out.println(" 4) Resources   - add / view / update availability");
            System.out.println(" 5) Next service request to handle (priority queue / heap)");
            System.out.println(" 6) Fastest route between two locations (Dijkstra)");
            System.out.println(" 7) Reachable locations from a dispatch point (BFS / DFS)");
            System.out.println(" 8) Cheapest network connecting all locations (Prim / Kruskal)");
            System.out.println(" 9) Greedy staff assignment");
            System.out.println("10) Best set of requests under a time budget (DP knapsack)");
            System.out.println("11) Look up a location / resource by id or name (hash table / BST)");
            System.out.println("12) Database - load / save / view algorithm run log");
            System.out.println(" 0) Exit");
            System.out.print("Choose: ");
            String choice = readLine();
            if (choice == null) { System.out.println("(end of input)"); return; } // EOF => exit
            switch (choice.trim()) {
                case "1":  locationsMenu();      break;
                case "2":  routesMenu();         break;
                case "3":  requestsMenu();       break;
                case "4":  resourcesMenu();      break;
                case "5":  showNextRequest();    break;
                case "6":  fastestRoute();       break;
                case "7":  reachability();       break;
                case "8":  cheapestNetwork();    break;
                case "9":  greedyAssignment();   break;
                case "10": dpSelection();        break;
                case "11": lookup();             break;
                case "12": databaseMenu();       break;
                case "0":  return;
                default:   System.out.println("Unknown option: " + choice);
            }
        }
    }

    // --------------------------------------------------------------------
    // 1–4  CRUD submenus
    // --------------------------------------------------------------------

    private void locationsMenu() {
        System.out.println("Locations: (a)dd  (v)iew  (u)pdate name  -> ");
        switch (nz(readLine()).trim().toLowerCase()) {
            case "a": {
                String id = ask("locationId (e.g. L900)");
                Location l = new Location(id, ask("name"), ask("area"), ask("type"), "added-via-menu");
                repo.addLocation(l);
                if (db != null) db.saveAll(repo);
                noteAudit("CREATE", "Location", id);
                System.out.println("Added " + l);
                break;
            }
            case "v": printFirst(repo.locations(), 25, "locations"); break;
            case "u": {
                String id = ask("locationId to rename");
                if (repo.updateLocationName(id, ask("new name"))) {
                    if (db != null) db.saveAll(repo);
                    noteAudit("UPDATE", "Location", id);
                    System.out.println("Renamed.");
                } else System.out.println("No such location: " + id);
                break;
            }
            default: System.out.println("Cancelled.");
        }
    }

    private void routesMenu() {
        System.out.println("Routes: (a)dd  (v)iew  -> ");
        switch (nz(readLine()).trim().toLowerCase()) {
            case "a": {
                String from = ask("fromLocationId"), to = ask("toLocationId");
                double dist = askDouble("distance (m)"), time = askDouble("travelTime (s)");
                repo.addRoute(new Route(from, to, dist, time, "added-via-menu"));
                if (db != null) db.saveAll(repo);
                noteAudit("CREATE", "Route", from + "->" + to);
                System.out.println("Added route " + from + " -> " + to);
                break;
            }
            case "v": printFirst(repo.routes(), 25, "routes"); break;
            default: System.out.println("Cancelled.");
        }
    }

    private void requestsMenu() {
        System.out.println("Requests: (a)dd  (v)iew  (u)pdate status  -> ");
        switch (nz(readLine()).trim().toLowerCase()) {
            case "a": {
                String id = ask("requestId (e.g. SR9000)");
                ServiceRequest s = new ServiceRequest(id, ask("source"), ask("destination"),
                        ask("category"), (int) askDouble("urgency 1-5"),
                        DatabaseManager.now(), "", "PENDING");
                repo.addRequest(s);
                if (db != null) db.saveAll(repo);
                noteAudit("CREATE", "ServiceRequest", id);
                System.out.println("Added " + s);
                break;
            }
            case "v": printFirst(repo.requests(), 25, "requests"); break;
            case "u": {
                String id = ask("requestId");
                ServiceRequest s = repo.getRequest(id);
                if (s == null) { System.out.println("No such request."); break; }
                s.setStatus(ask("new status (PENDING/ASSIGNED/DONE)"));
                if (db != null) db.saveAll(repo);
                noteAudit("UPDATE", "ServiceRequest", id);
                System.out.println("Updated " + s);
                break;
            }
            default: System.out.println("Cancelled.");
        }
    }

    private void resourcesMenu() {
        System.out.println("Resources: (a)dd  (v)iew  (u)pdate availability  -> ");
        switch (nz(readLine()).trim().toLowerCase()) {
            case "a": {
                String id = ask("resourceId (e.g. R900)");
                Resource r = new Resource(id, ask("type"), ask("homeLocation"),
                        (int) askDouble("capacity"), "AVAILABLE");
                repo.addResource(r);
                if (db != null) db.saveAll(repo);
                noteAudit("CREATE", "Resource", id);
                System.out.println("Added " + r);
                break;
            }
            case "v": printFirst(repo.resources(), 25, "resources"); break;
            case "u": {
                String id = ask("resourceId");
                Resource r = repo.getResource(id);
                if (r == null) { System.out.println("No such resource."); break; }
                r.setAvailabilityStatus(ask("new status (AVAILABLE/BUSY/OFFLINE)"));
                if (db != null) db.saveAll(repo);
                noteAudit("UPDATE", "Resource", id);
                System.out.println("Updated " + r);
                break;
            }
            default: System.out.println("Cancelled.");
        }
    }

    // --------------------------------------------------------------------
    // 5  Next request via the min-heap
    // --------------------------------------------------------------------

    private void showNextRequest() {
        long t0 = System.nanoTime();
        ServiceRequest next = repo.peekMostUrgent(); // builds a heap and peeks the min-urgency
        long dt = System.nanoTime() - t0;
        if (next == null) { System.out.println("No pending requests."); return; }
        System.out.println("Next to handle (most urgent): " + next);
        logRun("Heap.peekMostUrgent", "requests=" + repo.requests().size(),
                "next=" + next.getRequestId() + " urgency=" + next.getUrgency(), dt);
    }

    // --------------------------------------------------------------------
    // 6  Dijkstra fastest route
    // --------------------------------------------------------------------

    private void fastestRoute() {
        Graph g = repo.timeGraph();
        String fromId = ask("from locationId"), toId = ask("to locationId");
        int s = g.indexOf(fromId), t = g.indexOf(toId);
        if (s < 0 || t < 0) { System.out.println("Unknown location id."); return; }

        long t0 = System.nanoTime();
        GraphAlgorithms.ShortestPaths sp = GraphAlgorithms.dijkstra(g, s);
        DynamicArray<Integer> path = sp.pathTo(t);
        long dt = System.nanoTime() - t0;

        if (path.isEmpty()) {
            System.out.println("No route from " + fromId + " to " + toId + " (unreachable).");
            logRun("Dijkstra", "from=" + fromId + " to=" + toId + " |V|=" + g.vertexCount(), "unreachable", dt);
            return;
        }
        System.out.printf("Fastest route (%.0f seconds, %d hops):%n", sp.dist[t], path.size() - 1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            String id = g.idOf(path.get(i));
            Location loc = repo.getLocation(id);
            sb.append(id).append(loc != null ? " (" + loc.getName() + ")" : "");
            if (i < path.size() - 1) sb.append("  ->  ");
        }
        System.out.println("  " + sb);
        logRun("Dijkstra", "from=" + fromId + " to=" + toId + " |V|=" + g.vertexCount(),
                String.format("cost=%.0fs hops=%d", sp.dist[t], path.size() - 1), dt);
    }

    // --------------------------------------------------------------------
    // 7  BFS / DFS reachability
    // --------------------------------------------------------------------

    private void reachability() {
        Graph g = repo.timeGraph();
        String fromId = ask("dispatch locationId");
        int s = g.indexOf(fromId);
        if (s < 0) { System.out.println("Unknown location id."); return; }
        System.out.print("Use (b)fs or (d)fs? ");
        boolean useBfs = !nz(readLine()).trim().equalsIgnoreCase("d");

        long t0 = System.nanoTime();
        DynamicArray<Integer> order = useBfs ? GraphAlgorithms.bfs(g, s) : GraphAlgorithms.dfs(g, s);
        long dt = System.nanoTime() - t0;

        System.out.printf("%s reached %d of %d locations from %s:%n",
                useBfs ? "BFS" : "DFS", order.size(), g.vertexCount(), fromId);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(order.size(), 20); i++) sb.append(g.idOf(order.get(i))).append(' ');
        System.out.println("  " + sb + (order.size() > 20 ? "..." : ""));
        logRun(useBfs ? "BFS" : "DFS", "from=" + fromId + " |V|=" + g.vertexCount(),
                "reached=" + order.size(), dt);
    }

    // --------------------------------------------------------------------
    // 8  MST (Prim / Kruskal) on distance
    // --------------------------------------------------------------------

    private void cheapestNetwork() {
        Graph g = repo.distGraph();
        System.out.print("Use (p)rim or (k)ruskal? ");
        boolean usePrim = nz(readLine()).trim().equalsIgnoreCase("p");

        long t0 = System.nanoTime();
        GraphAlgorithms.MST mst = usePrim ? GraphAlgorithms.prim(g) : GraphAlgorithms.kruskal(g);
        long dt = System.nanoTime() - t0;

        System.out.printf("%s minimum spanning network: %d edges, total distance %.0f m%n",
                usePrim ? "Prim" : "Kruskal", mst.edges.size(), mst.totalWeight);
        for (int i = 0; i < Math.min(mst.edges.size(), 10); i++) {
            Graph.Edge e = mst.edges.get(i);
            System.out.printf("   %s -- %s  (%.0f m)%n", g.idOf(e.from), g.idOf(e.to), e.weight);
        }
        if (mst.edges.size() > 10) System.out.println("   ... (" + (mst.edges.size() - 10) + " more)");
        logRun(usePrim ? "Prim" : "Kruskal", "|V|=" + g.vertexCount() + " |E|=" + g.edgeCount(),
                String.format("edges=%d totalDist=%.0f", mst.edges.size(), mst.totalWeight), dt);
    }

    // --------------------------------------------------------------------
    // 9  Greedy assignment
    // --------------------------------------------------------------------

    private void greedyAssignment() {
        long t0 = System.nanoTime();
        GreedyAssignment.Result res = GreedyAssignment.assign(repo.requests(), repo.resources());
        long dt = System.nanoTime() - t0;

        System.out.printf("Greedy assignment: %d assigned, %d unassigned (of %d requests)%n",
                res.assignedCount, res.unassignedCount, repo.requests().size());
        for (int i = 0; i < Math.min(res.assignments.size(), 12); i++)
            System.out.println("   " + res.assignments.get(i));
        if (res.assignments.size() > 12) System.out.println("   ... (" + (res.assignments.size() - 12) + " more)");
        logRun("GreedyAssign", "requests=" + repo.requests().size() + " resources=" + repo.resources().size(),
                "assigned=" + res.assignedCount + " unassigned=" + res.unassignedCount, dt);
    }

    // --------------------------------------------------------------------
    // 10  DP knapsack request selection
    // --------------------------------------------------------------------

    private void dpSelection() {
        int budget = (int) askDouble("staff-minute budget (e.g. 120)");
        long t0 = System.nanoTime();
        DynamicSelection.Selection sel = DynamicSelection.selectBest(repo.requests(), budget);
        long dt = System.nanoTime() - t0;

        System.out.printf("DP selected %d requests, total value %d, using %d/%d minutes%n",
                sel.chosen.size(), sel.totalValue, sel.totalWeight, budget);
        for (int i = 0; i < Math.min(sel.chosen.size(), 12); i++) {
            ServiceRequest r = sel.chosen.get(i);
            System.out.printf("   %s  urgency=%d  minutes=%d  value=%d%n",
                    r.getRequestId(), r.getUrgency(), r.getServiceMinutes(), r.getValue());
        }
        if (sel.chosen.size() > 12) System.out.println("   ... (" + (sel.chosen.size() - 12) + " more)");
        logRun("DPKnapsack", "requests=" + repo.requests().size() + " budget=" + budget,
                "chosen=" + sel.chosen.size() + " value=" + sel.totalValue, dt);
    }

    // --------------------------------------------------------------------
    // 11  Lookup by id or name
    // --------------------------------------------------------------------

    private void lookup() {
        String key = ask("id or name to find");
        long t0 = System.nanoTime();
        Location byId = repo.getLocation(key);          // hash table O(1)
        Resource resById = repo.getResource(key);       // hash table O(1)
        Location byName = repo.findLocationByName(key);  // BST O(h)
        long dt = System.nanoTime() - t0;

        boolean found = false;
        if (byId != null)   { System.out.println("Location by id   : " + byId);   found = true; }
        if (resById != null){ System.out.println("Resource by id   : " + resById);found = true; }
        if (byName != null) { System.out.println("Location by name : " + byName); found = true; }
        if (!found) System.out.println("Nothing matched \"" + key + "\".");
        logRun("Lookup", "key=" + key, found ? "hit" : "miss", dt);
    }

    // --------------------------------------------------------------------
    // 12  Database menu
    // --------------------------------------------------------------------

    private void databaseMenu() {
        if (db == null) { System.out.println("Database is unavailable in this session."); return; }
        System.out.println("Database: (l)oad from DB  (s)ave to DB  (c)sv save  (r)un log  -> ");
        switch (nz(readLine()).trim().toLowerCase()) {
            case "l":
                repo = db.loadAll();
                System.out.println("Reloaded from database.");
                printSummary();
                break;
            case "s":
                db.saveAll(repo);
                System.out.println("Saved current state to database.");
                break;
            case "c":
                new DataLoader(SEED_DIR).saveAll(repo.locations(), repo.routes(), repo.requests(), repo.resources());
                System.out.println("Wrote current state back to the seed CSVs.");
                noteAudit("SAVE", "Database", "CSV export");
                break;
            case "r": {
                DynamicArray<AlgorithmRun> runs = db.recentRuns(15);
                System.out.println("Most recent algorithm runs:");
                for (AlgorithmRun r : runs) System.out.println("   " + r);
                if (runs.isEmpty()) System.out.println("   (none yet - run an algorithm first)");
                break;
            }
            default: System.out.println("Cancelled.");
        }
    }

    // --------------------------------------------------------------------
    // Small helpers
    // --------------------------------------------------------------------

    /** Time-and-log an algorithm run to the DB (no-op if DB unavailable). */
    private void logRun(String algo, String input, String result, long nanos) {
        System.out.printf("   [timing] %s took %.3f ms%n", algo, nanos / 1_000_000.0);
        if (db != null) db.logAlgorithmRun(new AlgorithmRun(algo, input, nanos, result, DatabaseManager.now()));
    }

    private void noteAudit(String type, String entity, String detail) {
        if (db != null) db.logAudit(new AuditEvent(type, entity, detail, DatabaseManager.now()));
    }

    private <T> void printFirst(DynamicArray<T> items, int n, String label) {
        System.out.println(label + " (" + items.size() + " total, showing up to " + n + "):");
        for (int i = 0; i < Math.min(items.size(), n); i++) System.out.println("   " + items.get(i));
    }

    private String ask(String prompt)  {
        System.out.print("  " + prompt + ": ");
        String s = readLine();
        return s == null ? "" : s; // treat end-of-input as empty so submenus don't NPE
    }
    private double askDouble(String p) {
        while (true) {
            String s = ask(p);
            try { return Double.parseDouble(s.trim()); }
            catch (Exception e) { System.out.println("  (please enter a number)"); }
        }
    }

    /** Read one line, returning null on end-of-input so callers can exit cleanly. */
    private String readLine() {
        if (!in.hasNextLine()) return null;
        return in.nextLine();
    }

    /** Null-to-empty coalesce, so submenu readers never NPE at end-of-input. */
    private static String nz(String s) { return s == null ? "" : s; }
}
