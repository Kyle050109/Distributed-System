# Distributed-System Assignment3

# Project objectives
- Implement 9 councilors (M1...) with Paxos M9) The one-time election of the "Council President"
- TCP Socket communication is used between processes.
- Each member simultaneously assumes the roles of Proposer, Acceptor, and Learner.
- The network behavior profile (reliable, delayed, failed, standard, cafe(same as reliable)) of members can be configured/switched at runtime.
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

Then make sure you already located at root.
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
  - "keep" means to keep running, which is convenient for you to operate manually, so if you want to run command manually, please use ./start_fresh.sh --keep, but if run auto tests, ./start_fresh.sh is enough.
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
# 7. Manual demonstration: (If you see 'Already decided:...' It indicates that a resolution was reached before; First './start_fresh.sh --keep' then start the round again. Technically, you need to run ./start_fresh.sh --keep when start another one scenario test.)
- Scenario 1: The Ideal Network
```
# all members reliable（start_fresh will do this）
printf "propose M5\n" | nc -w 1 localhost 10085
sleep 1
grep -h "CONSENSUS:" logs/*.out | wc -l
# expected:
OK propose M5
9
```
- Scenario 2: Concurrent Proposals:
```
(1)
printf "propose M1\n" | nc -w 1 localhost 10081 &
sleep 0.35

(2)
printf "propose M8\n" | nc -w 1 localhost 10088 &
wait
sleep 2
# expected: OK (ignored; already decided M1)

(3)
grep -h "CONSENSUS:" logs/*.out | wc -l
# expected: 9
```
- Scenario 3: Fault-Tolerance: (state change（M1=reliable, M2=latent, M3=failure, others are standard)
- Once run 3a/3b, ./start_fresh.sh --keep first, input below code then run.
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
#expected: OK profile M2 cafe PS: here, cafe = reliable
grep -h "CONSENSUS:" logs/*.out | wc -l # expected: 9
```
- 3c: After M3 proposed, it crashes -> M1 takes the relay
  - (1): As before,
  ```
  ./start_fresh.sh --keep
  ```
  - (2): Slow down the "majority node" to prevent M3 from quickly obtaining the majority:
  ```
  # set up states for each number
  printf "profile set M1 reliable\n" | nc -G 2 -w 2 localhost 10081
  printf "profile set M2 latent\n"   | nc -G 2 -w 2 localhost 10082
  printf "profile set M3 failure\n"  | nc -G 2 -w 2 localhost 10083
  for i in 4 5 6 7 8 9; do
    p=$((10080+i))
    printf "profile set M%d standard\n" $i | nc -G 2 -w 2 localhost $p
  done
  ```
  - (3) M3 propose:
  ```
  printf "propose M3\n" | nc -G 2 -w 2 localhost 10083
  sleep 0.04
  kill -9 "$(<logs/M3.pid)" 2>/dev/null || pkill -9 -f 'app\.CouncilMember.*--id[[:space:]]M3' || true

  # make sure it is really gone
  pgrep -fl 'app\.CouncilMember.*--id[[:space:]]M3' || echo "M3 gone"
  nc -z -w 1 localhost 10083 || echo "admin 10083 closed"
  ```
  - (4) Have M1 take the relay:
  ```
  printf "propose M1\n" | nc -G 2 -w 2 localhost 10081
  sleep 2
  ```
  - (5) Prove:
  ```
  # Expected: 8 (M3 is dead and will not be printed)
  # wait around 10 seconds then input:
  grep -h "CONSENSUS:" logs/*.out | wc -l

  # Expected: WINNER: M1, becuase M3 crashed.
  for p in 10081 10082 10084 10085 10086 10087 10088 10089; do
    printf "winner?\n" | nc -G 2 -w 2 localhost $p
  done
  ```

- Scenario 4: Persistency
```
# propose first,(make acceptors have promised/accepted)
printf "propose M5\n" | nc -w 1 localhost 10084
sleep 2

# kill M5
kill "$(cat logs/M5.pid)" 2>/dev/null || pkill -f "--id M5"
sleep 1

# restart M5 with the same parameters
java -cp target/classes app.CouncilMember \
  --id M5 --config network.config \
  --profile standard \
  --adminPort 10085 --seed 45 > logs/M5.out 2>&1 &
echo $! > logs/M5.pid

# submit another proposal and observe whether M5 will make promises/nack... ,based on history
printf "propose M1\n" | nc -w 1 localhost 10081
sleep 2
grep -E "PROMISE|NACK|ACCEPTED|CONSENSUS" logs/M5.* | tail -n 30
#PROMISE: do not accept a new proposal which is less than n.
#NACK: reject new proposal, because already received a higher one.
#ACCEPTED: already accepted your values.
#CONSENSUS: learnt a result after ACCEPTED.
```

- Scenario 5: Stable Proposal:
```
printf "propose M5\n" | nc -w 1 localhost 10084
sleep 1
printf "propose M3\n" | nc -w 1 localhost 10081
sleep 1
grep -h "CONSENSUS:" logs/*.out | sort | uniq -c
# still the winner of first time
```

# 8. Auto Tests:
```
# we have two auto tests, for assignemnt requirements, we have one sheel file, use:
chmod +x run_tests.sh
./run_tests.sh
# above tests can collect evidences to artifacts/, including each members' details.


# and for internal debugging and tests, we can also use:
mvn test
```

# Troubleshooting:
```
# Un terminal, check if there are all WINNER: NONE -> check whether the message has really been sent/received (see PREPARE/ACCEPTED)
for p in 10081 10082 10083 10084 10085 10086 10087 10088 10089; do
  printf "winner?\n" | nc -w 1 localhost $p
done

# Then, confirm that the data port is listening (admin port OK does not equal data port OK)
for p in 9001 9002 9003 9004 9005 9006 9007 9008 9009; do
  nc -z -w 1 localhost $p && echo "LISTEN $p" || echo "no listen $p"
done
# if there is 'no listen', the ports are occupied or the node binding failed, ./start_fresh.sh



