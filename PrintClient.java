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
		    System.out.println("print: " + p.print("data-file.pdf", "printer100A", sessionId));
		    System.out.println("queue: " + p.queue(sessionId));
		    System.out.println("topQueue: " + p.topQueue(999,sessionId));
		    System.out.println("start: " + p.start(sessionId));
		    System.out.println("stop: " + p.stop(sessionId));
		    System.out.println("restart: " + p.restart(sessionId));
		    System.out.println("status: " + p.status(sessionId));
		    System.out.println("setConfig: " + p.setConfig("is_fun_config","Always",sessionId));
		    System.out.println("readConfig: " + p.readConfig("is_fun_config",sessionId));
		} catch (Exception e) {
		    System.out.println(e);
		    System.err.println("PrintClient  exception:");
		    e.printStackTrace();
		}	
	}

}
