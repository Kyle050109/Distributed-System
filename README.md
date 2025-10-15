# Distributed-System Assignment3

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
