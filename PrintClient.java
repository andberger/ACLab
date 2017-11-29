import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class PrintClient {
	
	public static void main(String[] args){
		String host = (args.length < 1) ? null : args[0];
		try {
		    String name = "PrintServer";
		    Registry registry = LocateRegistry.getRegistry(host);
		    Printerface p = (Printerface) registry.lookup(name);
		    UUID activeSessionID = null;
		    activeSessionID = p.authenticateUser("testuser", "asdasdasd");
		    System.out.println(p.print("data-file.pdf", "printer100A",activeSessionID));
		    System.out.println(p.queue(activeSessionID));
		    System.out.println(p.setConfig("printer-policy","universal",activeSessionID));
		    System.out.println(p.readConfig("printer-policy",activeSessionID));
		} catch (Exception e) {
		    System.out.println(e);
		    System.err.println("PrintClient  exception:");
		    e.printStackTrace();
		}	
	}

}
