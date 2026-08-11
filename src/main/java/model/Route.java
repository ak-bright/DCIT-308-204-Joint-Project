package model;

/**
 * A corridor / road / connecting route between two {@link Location}s.
 *
 * <p>Routes are the <b>edges</b> of the {@link datastructures.Graph}. Each route
 * carries two independent weights:</p>
 * <ul>
 *   <li>{@code distance}   — physical length in metres (used by Prim/Kruskal MST,
 *       "cheapest network of corridors to keep lit/maintained").</li>
 *   <li>{@code travelTime} — walking/porter time in seconds (used by Dijkstra,
 *       "fastest way to move a patient from A to B").</li>
 * </ul>
 *
 * <p>Routes are treated as <b>undirected</b> by the graph loader (a corridor can
 * be walked both ways), so each row produces edges in both directions.</p>
 */
public class Route {
    private final String fromLocationId;
    private final String toLocationId;
    private double distance;    // metres
    private double travelTime;  // seconds
    private String notes;

    public Route(String fromLocationId, String toLocationId,
                 double distance, double travelTime, String notes) {
        this.fromLocationId = fromLocationId;
        this.toLocationId = toLocationId;
        this.distance = distance;
        this.travelTime = travelTime;
        this.notes = notes;
    }

    public String getFromLocationId() { return fromLocationId; }
    public String getToLocationId()   { return toLocationId; }
    public double getDistance()       { return distance; }
    public double getTravelTime()     { return travelTime; }
    public String getNotes()          { return notes; }

    public void setDistance(double distance)     { this.distance = distance; }
    public void setTravelTime(double travelTime) { this.travelTime = travelTime; }
    public void setNotes(String notes)           { this.notes = notes; }

    @Override
    public String toString() {
        return String.format("%s -> %s | %.0f m | %.0f s", fromLocationId, toLocationId, distance, travelTime);
    }
}
