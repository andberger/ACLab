import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.*;

public interface Printerface extends Remote {
	String print(String filename, String printer, UUID sId) throws RemoteException;
	String queue(UUID sId) throws RemoteException;
	String topQueue(int job, UUID sId) throws RemoteException;
	String start(UUID sId) throws RemoteException;
	String stop(UUID sId) throws RemoteException;
	String restart(UUID sId) throws RemoteException;
	String status(UUID sId) throws RemoteException;
	String readConfig(String parameter,UUID sId) throws RemoteException;
	String setConfig(String parameter, String value,UUID sId) throws RemoteException;
	UUID authenticateUser(String username, String password) throws RemoteException;

}

