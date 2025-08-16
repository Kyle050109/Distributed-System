# Distributed System Assignment1

## Files
- `Calculator.java` — RMI interface (5 required methods)
- `CalculatorImplementation.java` — server-side implementation (shared stack baseline)
- `CalculatorServer.java` — starts RMI registry on port 1099 and binds:
  - `CalculatorService` (shared stack)
  - `CalculatorSessionManager` (bonus: per-client stacks)
- `CalculatorClient.java` — simple client demonstrating all remote methods + bonus
- `MultiClientTest.java` — automated tests (single client + multi-client concurrency)

> No packages; place all `.java` files under `src/`.

## Build (Linux)
```bash
cd src
javac *.java
