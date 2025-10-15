# Distributed-System Assignment3

# Project objectives
- Implement 9 councilors (M1...) with Paxos M9) The one-time election of the "Council President"
- TCP Socket communication is used between processes.
- Each member simultaneously assumes the roles of Proposer, Acceptor, and Learner.
- The network behavior profile (reliable, delayed, failed, standard, cafe) of members can be configured/switched at runtime.
- The elected result that reaches a single consensus in various scenarios (ideal network, concurrent proposal, fault tolerance, persistence, resolution stability)
- Audit logs are produced through scripts and tests as evidence.

# 1. Requirements:
- OS: macOS or Linux (tested on macOS)
- Java: JDK 11
- Maven: 3.8+ (3.9.x OK)
- Ports:
  - Data-plane (member sockets): 19001..19009
  - Control-plane (admin sockets): 10081..10089

# 2. Network Configuration:
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

# 3. Build:
- Use
```
mvn package
mvn compile
```
# 4. Quick Start:
```
# To initialize environment
./start_fresh.sh --keep

# Ideal Network: Let M4 nominate M5
printf "propose M5\n" | nc -w 1 localhost 10084

# Prove
grep -h "CONSENSUS:" logs/*.out logs/*.log | sort | uniq -c
```

# 5. Admin command:
- One-click clearance and start:
  - By default, 9 processes will be stopped at the end.
  - "keep" means to keep running, which is convenient for you to operate manually.
```
chmod +x start_fresh.sh
./start_fresh.sh --keep
```
- Initiate a proposal: propose <VALUE> (VALUE = candidate, such as M5)
```
printf "propose M5\n" | nc -w 1 localhost 10084
# Expected Response: OK propose M5
# If decided: OK (ignored; already decided Mx)
```
- Change state: profile set <ID> <reliable|standard|latent|failure|cafe> ('cafe' is same as 'reliable', just be convinent for simulating that when M2 likes to work at Cafe, and 'cafe' can be used for all members as well.)
```
printf "profile set M2 cafe\n" | nc -w 1 localhost 10082
# Only when sent to "the member's own adminPort" and the ID matches will it take effect and return OK
```
- Reset state: clean the memory disk state to facilitate repeated experiments.
```
printf "reset\n" | nc -w 1 localhost 10081
```
- Check result: winner? -> WINNER: NONE | Mx
```
printf "winner?\n" | nc -w 1 localhost 10081
```

# 6. Logs and Verification:
- Two types of logs exist simultaneously
  - logs/M?.out：Standard output (plain text, often 'CONSENSUS:' at the beginning of the line).
  - logs/M? .log: Structured (including the prefix [time][node=M?]).

- Plain text starting with "CONSENSUS" :
```
grep -h "CONSENSUS:" logs/*.out | wc -l # expected: 9
```
- Merge and view (scan both types):
```
grep -h "CONSENSUS:" logs/*.out logs/*.log | sort | uniq -c
# Will see: a line of "9 CONSENSUS:..." Each node has one structured line
```
# 7. Manual demonstration: (If you see 'Already decided:...' It indicates that a resolution was reached before; First './start_fresh.sh --keep' then start the round again.)
- Scenario 1: The Ideal Network
```
# all members reliable（start_fresh will do this）
printf "propose M5\n" | nc -w 1 localhost 10085
sleep 1
grep -h "CONSENSUS:" logs/*.out | wc -l   # expected 9
```
- Scenario 2: Concurrent Proposals:
```
printf "propose M1\n" | nc -w 1 localhost 10081 &
sleep 0.35
printf "propose M8\n" | nc -w 1 localhost 10088 &
wait
sleep 2
grep -h "CONSENSUS:" logs/*.out | wc -l
# expeted: only one winner M1, and shows IGNORED...
```
- Scenario 3: Fault-Tolerance: (state change（M1=reliable, M2=latent, M3=failure, others are standard)
- Once run 3a/3b/3c, ./start_fresh.sh --keep first, input below code then run.
```
printf "profile set M1 reliable\n" | nc -w 1 localhost 10081
printf "profile set M2 latent\n"   | nc -w 1 localhost 10082
printf "profile set M3 failure\n"  | nc -w 1 localhost 10083
for i in 4 5 6 7 8 9; do
  p=$((10080+i)); id=M$i
  printf "profile set $id standard\n" | nc -w 1 localhost $p
done
```
- 3a: (M4 propose)
```
printf "propose M4\n" | nc -w 1 localhost 10084
sleep 3
# expected: OK propose M4
grep -h "CONSENSUS:" logs/*.out | wc -l # expected: 9
```
- 3b: (M2 (latent) propose -> change to cafe (reliable))
```
printf "propose M2\n" | nc -w 1 localhost 10082
sleep 2
#expected: OK propose M2
printf "profile set M2 cafe\n" | nc -w 1 localhost 10082 
sleep 2
#expected: OK profile M2 reliable or cafe
grep -h "CONSENSUS:" logs/*.out | wc -l # expected: 9
```
- 3c: After M3 proposed, it crashes -> M1 takes the relay
```
# crash (kill the processes) after propose
printf "propose M3\n" | nc -w 1 localhost 10083 # expected: OK propose M3
sleep 1
kill "$(cat logs/M3.pid)" 2>/dev/null || pkill -f "--id M3"

# other members take the relay
printf "propose M1\n" | nc -w 1 localhost 10081 #expected: OK propose M1
sleep 2
grep -h "CONSENSUS:" logs/*.out | wc -l
# expected: 8  because the M3 process has been killed, it will not print
```

