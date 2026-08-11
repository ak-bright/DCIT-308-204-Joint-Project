package model;

/**
 * A pending piece of work in the hospital: a patient transfer, a lab sample that
 * must reach a department, an equipment delivery, a cleaning call, etc.
 *
 * <p>The critical field for the assessed logic is {@code urgency} (1 = most
 * urgent … larger = less urgent). Requests are fed into our own
 * {@link datastructures.BinaryHeap} (a min-heap on urgency) so the "next request
 * to handle" is always the most urgent one. They are also the items chosen by
 * the greedy staff-assignment and the DP request-selection algorithms.</p>
 */
public class ServiceRequest {
    private final String requestId;
    private String source;        // origin locationId
    private String destination;   // target locationId
    private String category;      // patient-transfer | lab-sample | equipment | cleaning | ...
    private int urgency;          // 1 = highest priority (triage-like), higher = lower priority
    private String timeSubmitted; // ISO-ish timestamp string
    private String deadline;      // ISO-ish timestamp string (soft target)
    private String status;        // PENDING | ASSIGNED | DONE

    // Extra operational fields used by the greedy / DP algorithms. They are not
    // in the raw CSV; the loader derives sensible values so the algorithms have
    // something meaningful to optimise over. See DataLoader for how they're set.
    private int serviceMinutes = 15; // estimated staff-time this request consumes
    private int value = 0;           // "benefit" score for DP knapsack (derived from urgency)

    public ServiceRequest(String requestId, String source, String destination, String category,
                          int urgency, String timeSubmitted, String deadline, String status) {
        this.requestId = requestId;
        this.source = source;
        this.destination = destination;
        this.category = category;
        this.urgency = urgency;
        this.timeSubmitted = timeSubmitted;
        this.deadline = deadline;
        this.status = status;
    }

    public String getRequestId()     { return requestId; }
    public String getSource()        { return source; }
    public String getDestination()   { return destination; }
    public String getCategory()      { return category; }
    public int    getUrgency()       { return urgency; }
    public String getTimeSubmitted() { return timeSubmitted; }
    public String getDeadline()      { return deadline; }
    public String getStatus()        { return status; }
    public int    getServiceMinutes(){ return serviceMinutes; }
    public int    getValue()         { return value; }

    public void setSource(String source)           { this.source = source; }
    public void setDestination(String destination) { this.destination = destination; }
    public void setCategory(String category)       { this.category = category; }
    public void setUrgency(int urgency)            { this.urgency = urgency; }
    public void setDeadline(String deadline)       { this.deadline = deadline; }
    public void setStatus(String status)           { this.status = status; }
    public void setServiceMinutes(int m)           { this.serviceMinutes = m; }
    public void setValue(int v)                    { this.value = v; }

    @Override
    public String toString() {
        return String.format("%s | urgency=%d | %-16s | %s->%s | %s",
                requestId, urgency, category, source, destination, status);
    }
}
