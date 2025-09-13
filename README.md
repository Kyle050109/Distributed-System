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

**- JsonUtil.java**
   - Default: parse/serialize with Gson.
   - Bonus: can switch to MiniJsonParser at the last one row with flag **USE_MINI_PARSER = true.** 


**Persistence & Crash Recovery**

- Write-Ahead Log (data/wal.log)

Each PUT is first appended to WAL before being applied. Ensures no update is lost if the server crashes.

- Snapshot (data/state.json)

Represents the full aggregate at a point in time. Written atomically (via temp file rename).

- Bootstrap on restart
```bash
load state.json
replay wal.log
→ restore latest consistent state
```

Guarantees crash safety: never observe half-written state.

**Input File Format**

Each ContentServer reads a local .txt file （/input/station）:
```bash
id:IDS60901
name:Adelaide (West Terrace / ngayirdapira)
state:SA
time_zone:CST
lat:-34.9
lon:138.6
local_date_time:15/04:00pm
local_date_time_full:20230715160000
air_temp:13.3
apparent_t:9.5
cloud:Partly cloudy
dewpt:5.7
press:1023.9
rel_hum:60
wind_dir:S
wind_spd_kmh:15
wind_spd_kt:8
```

Converted to JSON before sending:
```bash
{
  "id": "IDS60901",
  "name": "Adelaide (West Terrace / ngayirdapira)",
  "state": "SA",
  "air_temp": 13.3,
  "rel_hum": 60,
  "wind_dir": "S",
  "wind_spd_kmh": 15,
  "wind_spd_kt": 8
}
```

**Build**

Our enviroment is Java SDK 11 and maven 3.9.11, firstly, find the top level of your repo (where your pom.xml lives):

```bash
mvn clean package
```
That will compile everything and run your unit‐ and integration‐tests. If they all pass, you’re ready to go.

If not yet installed maven, use:

```bash
brew install maven
```

Then, start AggregationServer:

Open a terminal in the project root directory, run:

```bash
mvn exec:java \
  -Dexec.mainClass="ds.weather.AggregationServer" \
  -Dexec.args="4567" \
  -Dexec.cleanupDaemonThreads=false \
  &
```

The output would be [AggregationServer] Listening on port 4567.

After that, start two content servers （technically we can open unlimited server but we only show 2 different server here).

Get your input file ready （already provided 2 here）

**inputs/stationA.txt**

**inputs/stationB.txt**

In each file, each line is a valid JSON, for example:

```bash
{"id":"stationA","air_temp":21.5}
{"id":"stationA","air_temp":22.0}
```

Start station A in the **second terminal**:

```bash
mvn exec:java \
  -Dexec.mainClass="ds.weather.ContentServer" \
  -Dexec.args="http://localhost:4567 inputs/stationA.txt" \
  -Dexec.cleanupDaemonThreads=false \
  &
```

Start station B on the **third terminal**:

```bash
mvn exec:java \
  -Dexec.mainClass="ds.weather.ContentServer" \
  -Dexec.args="http://localhost:4567 inputs/stationB.txt" \
  -Dexec.cleanupDaemonThreads=false \
  &
```
They will send the JSON in their respective files to the aggregation server one per second. You will see:

```bash
PUT success: 201 → {"id":"IDS77777","name":"Another Station" ...
```
We can pull and view the aggregation result, we need to open **fourth terminal**, and run:

```bash
while true; do
  clear
  curl -s http://localhost:4567/weather.json | jq .
  sleep 3
done
```

This will get result per 3 seconds, **BUT** please be carefuly, due to our ExpiryCleaner, all the result will be cleaned after 30 seconds, so I suggest that you can keep rerun the code about the Station A and Station B, as long as you can see the aggregation result within 30 seconds after the content server sends the instruction.


**Testing**

We have auto tests, you can run the code to take tests:

```bash
mvn test
```

- JsonUtilTest → verifies Gson vs MiniJsonParser, file parsing.

- PutServiceTest → verifies Idempotency-Key and Lamport monotonicity.

- SystemIntegrationTest → full flow: ContentServer → AggregationServer → GETClient, expiry, caching.

For manul test to check persistency: YOU CAN RUN ALL THE BELOW CODES IN SAME TERMINAL

```bash
1. firstly you need to go the directory where pom.xml at

2. Start the service (first write)

mvn exec:java -Dexec.mainClass="ds.weather.AggregationServer" \
    -Dexec.args="4567" -Dexec.cleanupDaemonThreads=false &

3. PUT & GET verification

curl -X PUT http://localhost:4567/weather.json -H "Content-Type: application/json" \
     -d '{"id":"crashTest","air_temp":18.2}'
curl http://localhost:4567/weather.json | jq .

4. kill & restart（The same directory, the same command）

ps aux | grep AggregationServer （find the pid which AggreatinServer using）
kill -9 YOUR-PID
mvn exec:java -Dexec.mainClass="ds.weather.AggregationServer" \
    -Dexec.args="4567" -Dexec.cleanupDaemonThreads=false &

5 reGET，should observe the same data

curl http://localhost:4567/weather.json | jq .
```

**Bonus feature**

MiniJsonParser.java: self-written flat JSON parser via JsonUtil.java

```bash
JsonUtil.USE_MINI_PARSER = true;
```


**HTTP and Status Codes** (Implemented using Socket)

- Only GET and PUT are supported; Other methods return 400.

- Create its data entry for a certain content server for the first time: 201; Subsequent update: 200.

- Empty content: 204.

- Parse to illegal/unreasonable JSON: 500.

- Resource does not exist/parameter error: 400 (the question requires simplification).

Overload/Queue full: 503.

GET supports ETag/If-None-Match -> 304.

**Summary**

This system demonstrates:

- Distributed systems principles (Lamport, consistency, failure recovery).

- RESTful design (stateless, uniform interface).

- Resilience patterns (retry, backoff, circuit breaker).

- Crash-safe persistence (WAL + snapshot).

- Automated tests to validate correctness.
