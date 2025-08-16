import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayDeque;
import java.util.Deque;

//server-side implementation but in public stack
//all clients share the same stack in this implementation
public class CalculatorImplementation extends UnicastRemoteObject implements Calculator {
   // prevent magic number so we make operator to a member
   // although this is not a matter
    private static final String OP_MIN = "min";
    private static final String OP_MAX = "max";
    private static final String OP_GCD = "gcd";
    private static final String OP_LCM = "lcm";

    private final Deque<Integer> stack=new ArrayDeque<>();
    protected CalculatorImplementation() throws RemoteException { super(); }

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
        String op=operator == null ? "" : operator.toLowerCase().trim();
        if (stack.isEmpty()) return; //handling to don't do anything if it is empty

        int result = stack.pop();
        while (!stack.isEmpty()){
            int v = stack.pop();
            switch (op){//use math logic and define functions
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
        if (stack.isEmpty()) throw new RemoteException("Pop on empty stack");
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
        try {
            Thread.sleep(Math.max(0, ms));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteException("Interrupted while waiting", e);
        }
        //the actual stack popping is done in the synchronous method pop()
        return pop();
    }
    //helper for gcd
    private static int gcd(int a, int b){
        a=Math.abs(a); b=Math.abs(b);
        while (b!=0) { int t=a%b; a=b; b=t;}
        return a;
    }
    //helper for lcm and with a overflow check
    private static int lcm(int a, int b) throws RemoteException{
        if (a==0 || b==0) return 0;
        long g=gcd(a, b);
        long res=Math.abs((long)a/g*(long)b);
        if (res > Integer.MAX_VALUE) throw new RemoteException("LCM overflow: "+a+","+b);
        return (int) res;
    }
}
