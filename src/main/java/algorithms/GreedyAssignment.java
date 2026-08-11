package algorithms;

import datastructures.BinaryHeap;
import datastructures.DynamicArray;
import model.Resource;
import model.ServiceRequest;

import java.util.Comparator;

/**
 * Greedy staff/equipment assignment for a real operational decision: given the
 * pending {@link ServiceRequest}s and the currently available {@link Resource}s,
 * decide who does what.
 *
 * <p><b>The greedy rule:</b> handle requests <em>most-urgent-first</em> (a min-heap
 * on urgency), and give each request the available resource with the most
 * remaining capacity that can still fit its service time ("best-fit"). This is a
 * sensible, fast dispatch heuristic and mirrors how a charge nurse triages.</p>
 *
 * <p><b>Time:</b> O(R log R + R·K) for R requests and K resources.
 * <b>Space:</b> O(R + K).</p>
 *
 * <hr>
 * <p><b>WORKED COUNTEREXAMPLE — where this greedy choice is NOT optimal.</b><br>
 * Suppose the objective is to <em>maximise the number of requests completed</em>
 * with a single team that has <b>10 minutes</b> of capacity, and three pending
 * requests:</p>
 * <pre>
 *   Request  Urgency  ServiceMinutes
 *      A        1          10
 *      B        2           5
 *      C        3           5
 * </pre>
 * <p>The greedy rule sorts by urgency and takes A first. A consumes all 10
 * minutes, so B and C cannot be served: greedy completes <b>1</b> request.
 * The optimal choice ignores urgency and takes B and C (5 + 5 = 10 minutes),
 * completing <b>2</b> requests. So greedy-by-urgency is strictly sub-optimal for
 * throughput here — there is no exchange that lets urgency-first recover the
 * second completion once A is chosen. (This is exactly the gap the 0/1-knapsack
 * dynamic program in {@link DynamicSelection} closes, at higher time cost.)</p>
 *
 * <p><b>Why we still use greedy in the product:</b> for live triage, urgency
 * ordering is the ethically correct objective even when it lowers raw
 * throughput — see the report's "responsible algorithm selection" section. The
 * counterexample is about the <em>throughput</em> objective, not patient safety.</p>
 */
public final class GreedyAssignment {
    private GreedyAssignment() {}

    /** One (request → resource) decision produced by the greedy dispatcher. */
    public static final class Assignment {
        public final ServiceRequest request;
        public final Resource resource; // null == left unassigned (no fit)
        public Assignment(ServiceRequest request, Resource resource) { this.request = request; this.resource = resource; }
        @Override public String toString() {
            return request.getRequestId() + " (urg " + request.getUrgency() + ") -> " +
                   (resource == null ? "UNASSIGNED" : resource.getResourceId());
        }
    }

    /** The full result: the per-request decisions plus a quick summary count. */
    public static final class Result {
        public final DynamicArray<Assignment> assignments = new DynamicArray<>();
        public int assignedCount = 0;
        public int unassignedCount = 0;
    }

    /**
     * Run the greedy dispatcher. Resources are consumed by remaining capacity
     * (each assignment subtracts the request's service minutes). Only AVAILABLE
     * resources are considered.
     */
    public static Result assign(DynamicArray<ServiceRequest> requests, DynamicArray<Resource> resources) {
        Result result = new Result();

        // Working copy of remaining capacity per resource (only AVAILABLE ones).
        DynamicArray<Resource> pool = new DynamicArray<>();
        DynamicArray<Integer> remaining = new DynamicArray<>();
        for (Resource r : resources) {
            if (r.isAvailable()) { pool.add(r); remaining.add(r.getCapacity()); }
        }

        // Feed requests into a min-heap keyed on urgency (1 = most urgent first).
        ServiceRequest[] arr = new ServiceRequest[requests.size()];
        for (int i = 0; i < requests.size(); i++) arr[i] = requests.get(i);
        BinaryHeap<ServiceRequest> heap =
                new BinaryHeap<>(arr, Comparator.comparingInt(ServiceRequest::getUrgency));

        while (!heap.isEmpty()) {
            ServiceRequest req = heap.extractTop();   // most urgent remaining
            int need = req.getServiceMinutes();

            // Best-fit: the available resource with the most remaining capacity
            // that still covers this request's service time.
            int bestIdx = -1, bestCap = -1;
            for (int i = 0; i < pool.size(); i++) {
                int cap = remaining.get(i);
                if (cap >= need && cap > bestCap) { bestCap = cap; bestIdx = i; }
            }

            if (bestIdx >= 0) {
                remaining.set(bestIdx, bestCap - need);    // consume capacity
                result.assignments.add(new Assignment(req, pool.get(bestIdx)));
                result.assignedCount++;
            } else {
                result.assignments.add(new Assignment(req, null)); // nobody can fit it
                result.unassignedCount++;
            }
        }
        return result;
    }
}
