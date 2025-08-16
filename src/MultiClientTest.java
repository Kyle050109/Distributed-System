import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

// automated testing: Covering single-client, multi-client concurrency, delayPop contention, and session isolation (bonus)
public class MultiClientTest{
    private static final int DEFAULT_RMI_PORT = 1099;
    private static final String SHARED_BINDING  = "CalculatorService";
    private static final String SESSION_BINDING = "CalculatorSessionManager";

    private static final int CONCURRENCY = 6;     // >3，meet the scoring points
    private static final int TEST_DELAY_MS = 200;

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port  = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_RMI_PORT;
        Registry registry = LocateRegistry.getRegistry(host, port);

        //shared stack concurrency
        Calculator calc=(Calculator) registry.lookup(SHARED_BINDING);
        while (!calc.isEmpty()) { calc.pop(); } //claer
        CountDownLatch ready = new CountDownLatch(CONCURRENCY);
        CountDownLatch go = new CountDownLatch(1);
        List<Thread> ts = new ArrayList<>();
        for (int i=0;i<CONCURRENCY;i++){
            final int val=(i + 1)*7;
            Thread t=new Thread(() ->{
                try { ready.countDown(); go.await(); calc.pushValue(val);}
                catch (Exception e) { throw new RuntimeException(e);}
            });
            t.start(); ts.add(t);
        }
        ready.await(); go.countDown();
        for (Thread t : ts) t.join();
        calc.pushOperation("min");
        assertEq(7, calc.pop(), "Shared min of {7,14,21,28,35,42}");

        //gcd/lcm single-client assertion
        calc.pushValue(12); calc.pushValue(18); calc.pushValue(30);
        calc.pushOperation("gcd");
        assertEq(6, calc.pop(), "gcd(12,18,30)");

        calc.pushValue(4); calc.pushValue(6); calc.pushValue(14);
        calc.pushOperation("lcm");
        assertEq(84, calc.pop(), "lcm(4,6,14)");

        //delayPop single client
        calc.pushValue(123);
        calc.delayPop(TEST_DELAY_MS);
        assertTrue(calc.isEmpty(), "delayPop clears");

        //concurrent delayPop race that only one thread can successfully obtain 999
        calc.pushValue(999);
        int racers = 4;
        CountDownLatch rReady = new CountDownLatch(racers);
        CountDownLatch rGo = new CountDownLatch(1);
        List<Thread> racersList = new ArrayList<>();
        List<Integer> results = new ArrayList<>();

        for (int i=0;i<racers;i++){
            Thread t=new Thread(() -> {
                try {
                    rReady.countDown(); rGo.await();
                    int v = calc.delayPop(TEST_DELAY_MS);
                    synchronized (results) { results.add(v); }
                } catch (Exception ignore){}
            });
            t.start(); racersList.add(t);
        }
        rReady.await(); rGo.countDown();
        for (Thread t : racersList) t.join();
        assertEq(1, results.size(), "exactly one delayPop succeeds");
        assertEq(999, results.get(0), "winner gets 999");
        assertTrue(calc.isEmpty(), "stack empty after race");

        //session isolated tests for bonus
        CalculatorServer.SessionManager mgr =
                (CalculatorServer.SessionManager) registry.lookup(SESSION_BINDING);
        CalculatorServer.CalculatorSession a = mgr.createSession("S-A");
        CalculatorServer.CalculatorSession b = mgr.createSession("S-B");
        a.pushValue(1); a.pushValue(9); a.pushOperation("max"); // -> 9
        b.pushValue(6); b.pushValue(8); b.pushOperation("gcd"); // -> 2
        assertEq(9, a.pop(), "Session A result");
        assertEq(2, b.pop(), "Session B result");
        assertTrue(a.isEmpty() && b.isEmpty(), "both sessions empty");

        System.out.println("all tests passed");
    }

    private static void assertEq(int exp, int act, String msg){
        if (exp != act) throw new AssertionError(msg + " expected " + exp + " got " + act);
        System.out.println("OK: " + msg);
    }
    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError("Assertion failed: " + msg);
        System.out.println("OK: " + msg);
    }
}
