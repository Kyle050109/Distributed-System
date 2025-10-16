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
- This implementation prints several types of keywords that correspond one-to-one with the Paxos stage in the log, facilitating the evaluation script and manual inspection：
```
Message type:

PREPARE
Proposer 进入 Phase 1，向所有 Acceptors 发送 “准备号” n。
日志示例：SEND PREPARE inst=ELECTION-1 n=... to=Mk

PROMISE
Acceptor 收到 PREPARE(n) 且 n >= promisedN 时返回。包含该 Acceptor 先前已接受的最高 (accN, accV)（若有）。
作用：Proposer 收集到多数 PROMISE 后，进入 Phase 2；若收到的 accV 非空，必须沿用最高 accN 对应的 accV。
日志示例（收）：RECV PROMISE from=Mk n=<n> accN=<accN> accV=<accV> (x/5)

ACCEPT
Proposer 在拿到多数 PROMISE 后，选择值 v（若有 accV 就用 accV，否则用自己的候选值），向所有 Acceptors 发送 ACCEPT(n, v)。
日志示例：PHASE2 SEND ACCEPT n=<n> v=<v>

ACCEPTED
Acceptor 对 ACCEPT(n, v) 进行“接受”，条件是 n >= promisedN。它会记录 acceptedN=n, acceptedV=v 并回复 ACCEPTED。
作用：Proposer 观察到多数 ACCEPTED 后即可决定。
日志示例（收）：RECV ACCEPTED from=Mk n=<n> (x/5)

NACK
Acceptor 在 n < promisedN 时拒绝 PREPARE 或 ACCEPT，并携带当前更高的 promisedN（日志里显示 higher=...）。
作用：Proposer 需要提升 proposal number 后重试（本实现会自动回退重试）。
日志示例：NACK higher=<promisedN> (on PREPARE/ACCEPT ...) / RECV NACK higher=... -> backoff+retry

DECIDE
当 Proposer 观察到多数 ACCEPTED 时，会在本地 decide，并向全体广播 DECIDE(v)。Learner 收到后进行本地落盘与幂等处理。
日志示例：DECIDE BROADCAST v=<v> / DECIDE REBROADCAST (recovered) v=<v>

Human-readable markers:
CONSENSUS:
Learner 本地“学习到”最终结果时打印的固定行，形式为：
CONSENSUS: <Mx> has been elected Council President!

这是场景判题/grep 的主依据。9 个成员都活着时会出现 9 行；若某成员崩溃，可能少于 9 行（例如 8 行）。

同时我们也把这行写入 logs/<Mi>.log 与 target/itest-logs/<Mi>.log，便于脚本收集。

Already decided …; ignoring propose …
集群已经有最终赢家时，新的 propose 被忽略；管理员命令仍返回 OK (ignored; already decided ...)。该提示能验证决议稳定性（post-decision stability）。

RECOVER promised=… acceptedN=… acceptedV=… decided=…
进程启动时打印已从磁盘恢复的持久化状态（持久化测试的关键证据）。

未决定前崩溃的理想输出可能是：promised=... acceptedN=null acceptedV=null decided=null（或 accepted 也非空，看时机）。

已决定后重启会看到 decided=<Mx>，并自动补打一条 CONSENSUS: 以及一次 DECIDE 复播。


```
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
./start_fresh.sh --keep

(1) all reliable 
for i in 1 2 3 4 5 6 7 8 9; do p=$((10080+i)); printf "profile set M%d reliable\n" $i | nc -G 2 -w 2 localhost $p; done

(2) M4 propose M5
printf "propose M5\n" | nc -G 2 -w 2 localhost 10084
sleep 0.03
kill -9 "$(<logs/M5.pid)" 2>/dev/null || pkill -9 -f 'app\.CouncilMember.*--id[[:space:]]M5' || true

(3) Restart M5
java -cp target/classes app.CouncilMember \
  --id M5 --config network.config \
  --profile reliable \
  --adminPort 10085 --seed 45 > logs/M5.out 2>&1 & echo $! > logs/M5.pid
sleep 0.5

(4) Check, we hope see decided=null
grep -n "RECOVER" logs/M5.out

(5) Propose again
printf "propose M5\n" | nc -G 2 -w 2 localhost 10084
sleep 2

(6) Verify consistency and logs
grep -h "CONSENSUS:" logs/*.out | sort | uniq -c
sed -n '1,120p' state_M5.txt
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



