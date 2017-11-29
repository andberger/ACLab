import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Printerface extends Remote {
	String print(String filename, String printer, String sId) throws RemoteException;
	String queue(String sId) throws RemoteException;
	String topQueue(int job, String sId) throws RemoteException;
	String start(String sId) throws RemoteException;
	String stop(String sId) throws RemoteException;
	String restart(String sId) throws RemoteException;
	String status(String sId) throws RemoteException;
	String readConfig(String parameter, String sId) throws RemoteException;
	String setConfig(String parameter, String value, String sId) throws RemoteException;
	String authenticateUser(String username, String password) throws RemoteException;

}

