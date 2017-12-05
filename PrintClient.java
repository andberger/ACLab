import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class PrintClient {
	
	public static void main(String[] args){
		String user = (args.length < 1) ? null : args[0];
		String pw = (args.length < 1) ? null : args[1];
		try {
		    String name = "PrintServer";
		    Registry registry = LocateRegistry.getRegistry();
		    Printerface p = (Printerface) registry.lookup(name);
		    String sessionId = null;
		    sessionId = p.authenticateUser(user, pw);
		    System.out.println("----------------------------------------------------------------------------");
		    System.out.println(p.print("data-file.pdf", "printer100A", sessionId));
		    System.out.println(p.queue(sessionId));
		    System.out.println(p.topQueue(999,sessionId));
		    System.out.println(p.start(sessionId));
		    System.out.println(p.stop(sessionId));
		    System.out.println(p.restart(sessionId));
		    System.out.println(p.status(sessionId));
		    System.out.println(p.setConfig("is_fun_config","Always",sessionId));
		    System.out.println(p.readConfig("is_fun_config",sessionId));
		} catch (Exception e) {
		    System.out.println(e);
		    System.err.println("PrintClient  exception:");
		    e.printStackTrace();
		}	
	}

}
