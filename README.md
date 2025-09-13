# Distributed-System Assignment2
**1.Project Purpose**
- This project implements a weather data aggregation system with:
- Aggregation Server: Collects JSON weather updates from multiple Content Servers and serves GET requests to clients.
- Content Server(s): Reads local txt files (key:value format), converts them into JSON, and sends them via PUT to the Aggregation Server.
- GET Client: Fetches the aggregated weather feed via GET.
- Consistency & Resilience:
   - Logical ordering using Lamport Clocks.
   - Write-Ahead Log (wal.log) + atomic snapshot (state.json) for crash-safe persistence.
   - 30s expiry to remove stale content.
   - Circuit Breaker + bounded queue for overload protection.
   - Idempotency-Key for safe retries.
     
`This assignment demonstrates understanding of distributed systems concepts from the course lectures (Lamport clocks, RPC vs REST, resilience, consistency).`


**2.System Architecture**

**Components**

- AggregationServer.java
   - Listens on TCP sockets, parses HTTP requests.
   - Dispatches GET/PUT via Router.
   - Manages thread pool with bounded queue → applies backpressure (503).
   - Maintains ServerState with WAL + snapshot persistence.
   - Spawns ExpiryCleaner to drop stale sources (>30s).

- ContentServer.java
    - Reads a local txt file in key:value format.
    - Converts it to JSON (via JsonUtil).
    - Sends it with PUT /weather.json to AggregationServer.
    - Retries with exponential backoff+jitter on failure.
    - Includes Idempotency-Key in headers for deduplication.

- GETClient.java
    - Sends GET /weather.json (optionally with station ID).
    - Prints JSON as human-readable key:value lines.
    - Uses ETag + If-None-Match headers to support HTTP caching.

- ServerState.java
   - In-memory aggregation: Map<contentServerId, JSON object>.
   - Tracks lastSeen timestamps for expiry.
   - Provides applyCommit() = WAL → memory → snapshot (atomic).
   - Provides bootstrap() = load snapshot + replay WAL on restart.

- WalStore.java / StateStore.java
   - WAL = append-only log of PUTs.
   - StateStore = atomic snapshot (write tmp → rename).

- JsonUtil.java
   - Default: parse/serialize with Gson.
   - Bonus: can switch to MiniJsonParser at the last one row with flag USE_MINI_PARSER = true.
