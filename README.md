# Distributed-System Assignment2
1. Project Purpose
This project implements a weather data aggregation system with:

Aggregation Server: Collects JSON weather updates from multiple Content Servers and serves GET requests to clients.

Content Server(s): Reads local txt files (key:value format), converts them into JSON, and sends them via PUT to the Aggregation Server.

GET Client: Fetches the aggregated weather feed via GET.

Consistency & Resilience:

Logical ordering using Lamport Clocks.

Write-Ahead Log (wal.log) + atomic snapshot (state.json) for crash-safe persistence.

30s expiry to remove stale content.

Circuit Breaker + bounded queue for overload protection.

Idempotency-Key for safe retries.

This assignment demonstrates understanding of distributed systems concepts from the course lectures (Lamport clocks, RPC vs REST, resilience, consistency).
