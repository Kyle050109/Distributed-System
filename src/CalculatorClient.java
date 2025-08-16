import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

//simple client run the shared stack first and then the session version of bonus
public class CalculatorClient {
    private static final int DEFAULT_RMI_PORT = 1099;
    private static final String SHARED_BINDING  = "CalculatorService";
    private static final String SESSION_BINDING = "CalculatorSessionManager";

    public static void main(String[] args) throws Exception{
        String host=args.length > 0 ? args[0] : "localhost";
        int port=args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_RMI_PORT;

        Registry registry=LocateRegistry.getRegistry(host, port);

        //basic shared stack
        Calculator calc = (Calculator) registry.lookup(SHARED_BINDING);
        calc.pushValue(10);
        calc.pushValue(5);
        calc.pushOperation("min");
        System.out.println("Shared stack min(10,5) = " + calc.pop()); // 5

        //bonus version each client has a separate stack
        //which is now defined and implemented within CalculatorServer
        CalculatorServer.SessionManager manager =
                (CalculatorServer.SessionManager) registry.lookup(SESSION_BINDING);

        CalculatorServer.CalculatorSession sA=manager.createSession("client-A");
        CalculatorServer.CalculatorSession sB=manager.createSession("client-B");

        sA.pushValue(50);
        sB.pushValue(7);
        sB.pushValue(18);
        sB.pushOperation("max"); //->18

        System.out.println("Per-client A pop -> " + sA.pop()); //50
        System.out.println("Per-client B pop -> " + sB.pop()); //18
        System.out.println("A empty? " + sA.isEmpty() + ", B empty? " + sB.isEmpty());
    }
}
