import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

//server startup class that we bind two names
//1: CalculatorService which is a shared stack
//2: CalculatorSessionManager which is for bonus that is each client has its own stack
public class CalculatorServer {
    private static final int DEFAULT_RMI_PORT = 1099;
    private static final String SHARED_BINDING  = "CalculatorService";
    private static final String SESSION_BINDING = "CalculatorSessionManager";
    //function: start the local RMI registry (1099) and bind the shared stack with the session manager
    //input: none
    //output: none (console print status)
    //special: if the registry already exists it will print a prompt but continue to reuse it
    public static void main(String[] args) throws Exception{
        int port = DEFAULT_RMI_PORT;
        try {
            LocateRegistry.createRegistry(port);
            System.out.println("RMI registry started on port " + port);
        } catch (Exception e) {
            System.out.println("Registry may already be running: " + e.getMessage());
        }
        Registry registry = LocateRegistry.getRegistry(port);

        //basic version for shared stack
        CalculatorImplementation shared = new CalculatorImplementation();
        registry.rebind(SHARED_BINDING, shared);
        System.out.println("Bound: " + SHARED_BINDING);

        //bonus version interface for each client has its own stack (the embedded interface/implementation below)
        SessionManager manager = new SessionManagerImpl();
        registry.rebind(SESSION_BINDING, manager);
        System.out.println("Bound: " + SESSION_BINDING);

        System.out.println("Server ready.");
    }

    //the following is the embedded interface and implementation of bonus

    //session interface which is consistent with the five methods in the question
    //but it operates on the stack of the session itself
    public interface CalculatorSession extends Remote{
        void pushValue(int val) throws RemoteException;
        void pushOperation(String operator) throws RemoteException;
        int pop() throws RemoteException;
        boolean isEmpty() throws RemoteException;
        int delayPop(int ms) throws RemoteException;
    }

    //session interface for: create/obtain sessions based on clientId
    public interface SessionManager extends Remote{
        CalculatorSession createSession(String clientId) throws RemoteException;
    }

    //session implementation to maintain a mapping of clientId->session objects
    //multiple calls to the same clientId will return the same session instance (the same session stack)
    public static class SessionManagerImpl extends UnicastRemoteObject implements SessionManager{
        private final ConcurrentMap<String, CalculatorSession> sessions = new ConcurrentHashMap<>();
        protected SessionManagerImpl() throws RemoteException {super();}


        //function: create or return the session of the specified clientId (each client's own stack)
        //input: clientId (non-empty)
        //output: the remote session object of this client
        //special: if clientId is empty or completely blank, a RemoteException will be thrown
        @Override
        public CalculatorSession createSession(String clientId) throws RemoteException{
            if (clientId == null || clientId.isBlank()) throw new RemoteException("clientId must not be empty");
            return sessions.computeIfAbsent(clientId, id ->{
                try { return new CalculatorSessionImpl(id); }
                catch (RemoteException e) { throw new RuntimeException(e);}
            });
        }
    }

    //session implementation that each session has an independent stack(synchronized is also used here to ensure thread safety)
    public static class CalculatorSessionImpl extends UnicastRemoteObject implements CalculatorSession{
        //prevent magic number that we make operator to a member (same as shared stack version)
        private static final String OP_MIN = "min";
        private static final String OP_MAX = "max";
        private static final String OP_GCD = "gcd";
        private static final String OP_LCM = "lcm";

        private final String clientId;
        private final Deque<Integer> stack = new ArrayDeque<>();

        protected CalculatorSessionImpl(String clientId) throws RemoteException{
            super();
            this.clientId = clientId;
        }
        //synchronized to prevent multiple threads from modifying the same stack simultaneously
        //function: push an integer onto the top of the shared stack
        //input: val (integer)
        //output: none
        //special: none if assumed that the input is a valid number
        @Override
        public synchronized void pushValue(int val) throws RemoteException{
            stack.push(val);
        }

        //synchronized to pop out and aggregate all the values at once and then push back the result
        //function: aggregate all values in the current stack (min/max/gcd/lcm)
        //and then push the result back to the top of the stack
        //input: operator which supports "min", "max", "gcd", "lcm"
        //output: none, because the result pushed onto stack
        //special: not processed when the stack is empty and throw a RemoteException for unsupported operators
        @Override
        public synchronized void pushOperation(String operator) throws RemoteException{
            String op = operator == null ? "" : operator.toLowerCase().trim();
            if (stack.isEmpty()) return;  //handling to don't do anything if it is empty

            int result = stack.pop();
            while (!stack.isEmpty()){
                int v = stack.pop();
                switch (op) {//use math logic and define functions
                    case OP_MIN: result = Math.min(result, v); break;
                    case OP_MAX: result = Math.max(result, v); break;
                    case OP_GCD: result = gcd(result, v); break;
                    case OP_LCM: result = lcm(result, v); break;
                    default: throw new RemoteException("Unsupported operator: " + operator);
                }
            }
            stack.push(result);
        }


        //synchronized to pop the top of the stack
        //function: pop and return the top element of the shared stack
        //input: none
        //output: the integer on the top of the stack
        //special: throw a RemoteException if the stack is empty
        @Override
        public synchronized int pop() throws RemoteException{
            if (stack.isEmpty()) throw new RemoteException("Pop on empty stack (client " + clientId + ")");
            return stack.pop();
        }


        //synchronized reading to determine if it is empty
        //function: determine whether the shared stack is empty
        //input: none
        //output: "true" indicates empty and "false" indicates non-empty
        //special: none
        @Override
        public synchronized boolean isEmpty() throws RemoteException{
            return stack.isEmpty();
        }


        //do not sleep in the locked state here to avoid blocking other threads
        //function: wait for the given number of milliseconds and then execute pop() once
        //input: ms (non-negative, milliseconds)
        //output: the integer which is popped
        //special: a RemoteException will be thrown when sleep is interrupted
        //if the waiting stack is empty, pop() will throw a RemoteException
        @Override
        public int delayPop(int ms) throws RemoteException{
            try { Thread.sleep(Math.max(0, ms)); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RemoteException("Interrupted", e); }
            return pop();//the actual stack popping is done in the synchronous method pop()
        }

        //basic utility functions but write another one here for file independence
        //helper for gcd
        private static int gcd(int a, int b){
            a=Math.abs(a); b= Math.abs(b);
            while (b!=0){ int t=a%b; a=b; b=t;}
            return a;
        }
        //helper for lcm and with a overflow check
        private static int lcm(int a, int b) throws RemoteException{
            if (a==0 || b==0) return 0;
            long g=gcd(a, b);
            long res=Math.abs((long)a / g * (long)b);
            if (res > Integer.MAX_VALUE) throw new RemoteException("LCM overflow");
            return (int) res;
        }
    }//bonus version finished
}
