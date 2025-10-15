# Distributed-System Assignment3

# Project objectives
- Implement 9 councilors (M1...) with Paxos M9) The one-time election of the "Council President"
- TCP Socket communication is used between processes.
- Each member simultaneously assumes the roles of Proposer, Acceptor, and Learner.
- The network behavior profile (reliable, delayed, failed, standard, cafe) of members can be configured/switched at runtime.
- The elected result that reaches a single consensus in various scenarios (ideal network, concurrent proposal, fault tolerance, persistence, resolution stability)
- Audit logs are produced through scripts and tests as evidence.

1.Requirements:
- OS: macOS or Linux (tested on macOS)
- Java: JDK 11
- Maven: 3.8+ (3.9.x OK)
- Ports:
  - Data-plane (member sockets): 19001..19009
  - Control-plane (admin sockets): 10081..10089

2.Network Configuration:
```bash
M1,localhost,19001
M2,localhost,19002
M3,localhost,19003
M4,localhost,19004
M5,localhost,19005
M6,localhost,19006
M7,localhost,19007
M8,localhost,19008
M9,localhost,19009
```
- These data-plane ports are where members listen for Paxos messages.

3.Build:
- Use ```mvn compile ```
