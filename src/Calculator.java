import java.rmi.Remote;
import java.rmi.RemoteException;

//remote interface for the calculator stack service (shared stack)
//this interface defines the calculator methods for remote invocation
//all methods must throw a RemoteException because it is an RMI call
public interface Calculator extends Remote {

    //push an integer value onto the shared server-side stack
    //integer value to push
    //throws RemoteException if network or remote-side failure
    void pushValue(int val) throws RemoteException;

    //pop all values currently on the stack, aggregate them using the operator
    //then push a single result back
    //supported operators: "min", "max", "gcd", "lcm".
    //operator aggregation operator: "min" | "max" | "gcd" | "lcm"
    //throws RemoteException if operator is unsupported or remote failure occurs
    void pushOperation(String operator) throws RemoteException;

    //pop and return the integer at the top of the stack
    //return the popped integer value
    //throws RemoteException if the stack is empty or remote failure occurs
    int pop() throws RemoteException;


    //to check if the shared stack is empty
    //return true when empty, false otherwise
    //throws RemoteException on remote failure
    boolean isEmpty() throws RemoteException;

    //wait for the given number of milliseconds, then pop the number at the top
    //this is useful for testing synchronisation with multiple clients
    //use ms non-negative delay in milliseconds
    //return the value popped after the delay
    //throws RemoteException if interrupted, empty stack, or remote failure
    int delayPop(int ms) throws RemoteException;
}
