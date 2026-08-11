package model;

/**
 * A staff member, piece of equipment, ambulance, or bed — anything that can be
 * <b>assigned</b> to a {@link ServiceRequest}.
 *
 * <p>Resources are the "workers" in the greedy staff-assignment algorithm and
 * the capacity units in the DP selection. {@code capacity} is how many
 * minutes/shifts of work the resource can take on in the planning window;
 * {@code availabilityStatus} gates whether it can be used at all right now.</p>
 */
public class Resource {
    private final String resourceId;
    private String type;               // nurse | porter | ambulance | wheelchair | bed | ...
    private String homeLocation;       // locationId the resource is based at
    private int capacity;              // available minutes/units in the planning window
    private String availabilityStatus; // AVAILABLE | BUSY | OFFLINE

    public Resource(String resourceId, String type, String homeLocation,
                    int capacity, String availabilityStatus) {
        this.resourceId = resourceId;
        this.type = type;
        this.homeLocation = homeLocation;
        this.capacity = capacity;
        this.availabilityStatus = availabilityStatus;
    }

    public String getResourceId()         { return resourceId; }
    public String getType()               { return type; }
    public String getHomeLocation()       { return homeLocation; }
    public int    getCapacity()           { return capacity; }
    public String getAvailabilityStatus() { return availabilityStatus; }

    public void setType(String type)                 { this.type = type; }
    public void setHomeLocation(String homeLocation) { this.homeLocation = homeLocation; }
    public void setCapacity(int capacity)            { this.capacity = capacity; }
    public void setAvailabilityStatus(String s)      { this.availabilityStatus = s; }

    /** Convenience for the greedy/DP algorithms. */
    public boolean isAvailable() { return "AVAILABLE".equalsIgnoreCase(availabilityStatus); }

    @Override
    public String toString() {
        return String.format("%s | %-10s | home=%s | cap=%d | %s",
                resourceId, type, homeLocation, capacity, availabilityStatus);
    }
}
