# Distributed System Assignment1

## Files
- `Calculator.java` — RMI interface (5 required methods)
- `CalculatorImplementation.java` — server-side implementation (shared stack baseline)
- `CalculatorServer.java` — starts RMI registry on port 1099 and binds:
  - `CalculatorService` (shared stack)
  - `CalculatorSessionManager` (bonus: per-client stacks)
- `CalculatorClient.java` — simple client demonstrating all remote methods + bonus
- `MultiClientTest.java` — automated tests (single client + multi-client concurrency)

No packages; place all `.java` files under `src/`.

## Compile
```bash
cd src
javac *.java
```

# Run the server
```bash
cd src
java CalculatorServer
```
This starts an in-process RMI registry on port 1099 and binds the two services: CalculatorService (shared stack) and CalculatorSessionManager (per-client isolated stacks, Bonus).

# Run a Single Client
Open another terminal:
```bash
cd src
java CalculatorClient
```
Expected behavior:
Tests shared stack with pushValue(10), pushValue(5), pushOperation(min) → pop() returns 5.
For bonus: creates session client-A and client-B, showing isolated stacks.

# Run Automated Tests (Single + Multi Client)
Open another terminal:
```bash
cd src
java MultiClientTest
```
What it covers:
- Multi-client (>3): 6 threads push values concurrently, then min reduces to 7.
- Single client: verifies gcd(12,18,30)=6, lcm(4,6,14)=84.
- delayPop (single client): pushes 123, pops after delay.
- delayPop contention: 4 threads competing; exactly one gets 999.
- Bonus session isolation: sessions S-A and S-B behave independently.

# Verifying Basic vs Bonus
A. Verify Basic Requirements (Shared Stack)
- 1. Start server
  ```bash
   cd src
   java CalculatorServer
  ```
- 2. Run automated test:
  ```bash
  cd src
  java MultiClientTest
  ```
Expected output includes:
- OK: Shared min of {7,14,21,28,35,42}
- OK: gcd(12,18,30)
- OK: lcm(4,6,14)
- OK: delayPop clears

