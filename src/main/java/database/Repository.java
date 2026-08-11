package database;

import datastructures.AVLTree;
import datastructures.BinaryHeap;
import datastructures.BinarySearchTree;
import datastructures.DynamicArray;
import datastructures.Graph;
import datastructures.HashTable;
import model.Location;
import model.Resource;
import model.Route;
import model.ServiceRequest;

import java.util.Comparator;

/**
 * In-memory domain store that ties the hand-written data structures together and
 * is the single object the UI talks to. It holds the four entity tables plus the
 * indexes and graphs derived from them:
 *
 * <ul>
 *   <li>{@link HashTable} id→entity indexes for O(1) "look up by id".</li>
 *   <li>A {@link BinarySearchTree} and an {@link AVLTree} keyed on location name
 *       for ordered/alphabetical name lookup (both kept so the report and
 *       benchmark can compare balanced vs. unbalanced behaviour on real names).</li>
 *   <li>Two {@link Graph}s built from the same routes: one weighted by travel
 *       time (Dijkstra / BFS / DFS) and one weighted by distance (Prim / Kruskal).</li>
 * </ul>
 *
 * <p>On load it also derives the two operational fields the greedy/DP algorithms
 * need — {@code serviceMinutes} (from category) and {@code value} (from urgency)
 * — since those are not part of the raw CSV.</p>
 */
public final class Repository {

    // --- primary tables (our own dynamic array) ---
    private final DynamicArray<Location> locations = new DynamicArray<>();
    private final DynamicArray<Route> routes = new DynamicArray<>();
    private final DynamicArray<ServiceRequest> requests = new DynamicArray<>();
    private final DynamicArray<Resource> resources = new DynamicArray<>();

    // --- indexes (our own hash table + trees) ---
    private final HashTable<String, Location> locById = new HashTable<>();
    private final HashTable<String, ServiceRequest> reqById = new HashTable<>();
    private final HashTable<String, Resource> resById = new HashTable<>();
    private BinarySearchTree<String, Location> locByNameBst = new BinarySearchTree<>();
    private AVLTree<String, Location> locByNameAvl = new AVLTree<>();

    // --- graphs (built from routes) ---
    private Graph timeGraph = new Graph();  // edge weight = travel time (seconds)
    private Graph distGraph = new Graph();  // edge weight = distance (metres)

    // ---------------------- accessors ----------------------
    public DynamicArray<Location> locations()             { return locations; }
    public DynamicArray<Route> routes()                   { return routes; }
    public DynamicArray<ServiceRequest> requests()        { return requests; }
    public DynamicArray<Resource> resources()             { return resources; }
    public Graph timeGraph()                              { return timeGraph; }
    public Graph distGraph()                              { return distGraph; }
    public Location getLocation(String id)                { return locById.get(id); }
    public ServiceRequest getRequest(String id)           { return reqById.get(id); }
    public Resource getResource(String id)                { return resById.get(id); }
    public Location findLocationByName(String name)       { return locByNameBst.search(name); }
    public BinarySearchTree<String, Location> nameBst()   { return locByNameBst; }
    public AVLTree<String, Location> nameAvl()            { return locByNameAvl; }

    // ---------------------- bulk load ----------------------

    /** Replace all data with freshly-loaded lists, then rebuild every index/graph. */
    public void replaceAll(DynamicArray<Location> locs, DynamicArray<Route> rts,
                           DynamicArray<ServiceRequest> reqs, DynamicArray<Resource> res) {
        locations.clear(); for (Location l : locs) locations.add(l);
        routes.clear();    for (Route r : rts)     routes.add(r);
        requests.clear();  for (ServiceRequest s : reqs) requests.add(s);
        resources.clear(); for (Resource r : res)  resources.add(r);
        deriveRequestEconomics();
        rebuildIndexes();
        rebuildGraphs();
    }

    /** Convenience: load everything from the seed-data CSVs via {@link DataLoader}. */
    public void loadFromCsv(String seedDir) {
        DataLoader dl = new DataLoader(seedDir);
        replaceAll(dl.loadLocations(), dl.loadRoutes(), dl.loadRequests(), dl.loadResources());
    }

    // ---------------------- mutations (used by the UI) ----------------------

    public void addLocation(Location l) {
        locations.add(l);
        locById.put(l.getLocationId(), l);
        locByNameBst.insert(l.getName(), l);
        locByNameAvl.insert(l.getName(), l);
    }

    public boolean updateLocationName(String id, String newName) {
        Location l = locById.get(id);
        if (l == null) return false;
        l.setName(newName);
        rebuildNameTrees(); // names changed: rebuild the by-name indexes
        return true;
    }

    public void addRoute(Route r) {
        routes.add(r);
        // keep both graphs in sync incrementally
        timeGraph.addEdge(r.getFromLocationId(), r.getToLocationId(), r.getTravelTime(), true);
        distGraph.addEdge(r.getFromLocationId(), r.getToLocationId(), r.getDistance(), true);
    }

    public void addRequest(ServiceRequest s) {
        deriveOne(s);
        requests.add(s);
        reqById.put(s.getRequestId(), s);
    }

    public void addResource(Resource r) {
        resources.add(r);
        resById.put(r.getResourceId(), r);
    }

    /** The most urgent pending request via a min-heap on urgency (peek only). */
    public ServiceRequest peekMostUrgent() {
        if (requests.isEmpty()) return null;
        ServiceRequest[] arr = new ServiceRequest[requests.size()];
        for (int i = 0; i < requests.size(); i++) arr[i] = requests.get(i);
        BinaryHeap<ServiceRequest> heap =
                new BinaryHeap<>(arr, Comparator.comparingInt(ServiceRequest::getUrgency));
        return heap.peek();
    }

    // ---------------------- index / graph builders ----------------------

    private void rebuildIndexes() {
        // clear-and-refill the hash indexes
        for (Location l : locations)       locById.put(l.getLocationId(), l);
        for (ServiceRequest s : requests)  reqById.put(s.getRequestId(), s);
        for (Resource r : resources)       resById.put(r.getResourceId(), r);
        rebuildNameTrees();
    }

    private void rebuildNameTrees() {
        locByNameBst = new BinarySearchTree<>();
        locByNameAvl = new AVLTree<>();
        for (Location l : locations) {
            locByNameBst.insert(l.getName(), l);
            locByNameAvl.insert(l.getName(), l);
        }
    }

    private void rebuildGraphs() {
        timeGraph = new Graph();
        distGraph = new Graph();
        // Register every location as a vertex first (so isolated nodes still exist).
        for (Location l : locations) { timeGraph.addVertex(l.getLocationId()); distGraph.addVertex(l.getLocationId()); }
        for (Route r : routes) {
            timeGraph.addEdge(r.getFromLocationId(), r.getToLocationId(), r.getTravelTime(), true);
            distGraph.addEdge(r.getFromLocationId(), r.getToLocationId(), r.getDistance(), true);
        }
    }

    // ---------------------- derived economics ----------------------

    /**
     * Fill in serviceMinutes and value for every request. These are needed by
     * the greedy dispatcher and the DP selector but are not in the raw CSV, so we
     * derive them deterministically from the request's own fields:
     *   serviceMinutes ← a per-category base estimate,
     *   value          ← higher for more urgent requests (6 - urgency scaled).
     */
    private void deriveRequestEconomics() {
        for (ServiceRequest s : requests) deriveOne(s);
    }

    private void deriveOne(ServiceRequest s) {
        s.setServiceMinutes(baseServiceMinutes(s.getCategory()));
        s.setValue((6 - clampUrgency(s.getUrgency())) * 10 + 5); // urgency 1 -> 55, urgency 5 -> 15
    }

    private int baseServiceMinutes(String category) {
        if (category == null) return 15;
        switch (category) {
            case "patient-transfer":    return 25;
            case "porter-escort":       return 20;
            case "equipment-delivery":  return 18;
            case "blood-delivery":      return 12;
            case "medication-run":      return 10;
            case "lab-sample":          return 10;
            case "specimen-collection": return 8;
            case "cleaning":            return 30;
            default:                    return 15;
        }
    }

    private int clampUrgency(int u) { return u < 1 ? 1 : (u > 5 ? 5 : u); }
}
