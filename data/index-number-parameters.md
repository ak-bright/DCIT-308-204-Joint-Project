# Algorithm Parameters Derived from Index Number

To ensure deterministic but unique parameter tuning for downstream algorithms (e.g., pathfinding, request batching), core variables are derived mathematically from our team's designated index number.

**Base Index Number:** 22268310

### Parameter 1: Request Batch Size
- **Derivation:** `(22268310 % 25) + 10`
- **Result:** 20
- **Purpose:** Determines the maximum number of `service_requests` processed by a campus resource (like a shuttle or maintenance van) in a single algorithm cycle.

### Parameter 2: Route Optimization Threshold (Meters)
- **Derivation:** `(22268310 % 500) + 200`
- **Result:** 510
- **Purpose:** Acts as the maximum walking distance before the algorithm mandates switching the routing type from `footpath` to `shuttle_route`.

### Parameter 3: Resource Allocation Timeout (Seconds)
- **Derivation:** `(22268310 % 30) + 15`
- **Result:** 15
- **Purpose:** Sets the time limit the system will wait for an `available` resource in `resources.csv` to acknowledge a high-priority service request before escalating to the next available asset.
