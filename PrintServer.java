import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;
import javafx.util.Pair;
import java.security.*;
import javax.crypto.*;
import java.math.BigInteger;
import java.sql.*;

public class PrintServer implements Printerface {
	private static List<UUID> activeSessions = new ArrayList<UUID>();
	private Queue<Pair<Integer,String>> queue = new LinkedList<Pair<Integer,String>>();
	private Map<String,String> config = new HashMap<String, String>();
	private static void logEvent(String event) throws IOException{
		FileWriter fileWriter = new FileWriter("logfile.log",true);
		PrintWriter printWriter = new PrintWriter(fileWriter);
		printWriter.print(event);
		printWriter.print("\n");
		printWriter.close();
	}
	private static String getEventTimeStamp(){
		SimpleDateFormat sdfr = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		java.util.Date now = new java.util.Date();
		return sdfr.format(now);
	}
	private static String hashPassword(String password, String salt){
		String passwordWithSalt = password + salt;
		byte[] pwBytes = passwordWithSalt.getBytes();
		MessageDigest md5 = null;
		try{
			md5 = MessageDigest.getInstance("MD5");
		}
		catch(Exception ex){
		}
		byte[] hashedBytes = md5.digest(pwBytes);
		String hashedPW = String.format("%032X", new BigInteger(1, hashedBytes));
		return hashedPW;
	}
	private static void registerUser(String username, String password){
		String salt = String.valueOf((int)(Math.random() * 1000000000 + 1));
		String hashedPW = hashPassword(password, salt);
		try{
			logEvent(getEventTimeStamp() + " | " + "User " + username + " was registered");
		}
		catch(IOException ex){
		}
		try{
			Connection c = DriverManager.getConnection("jdbc:sqlite:printer.db");
			String sql = "INSERT INTO users(name,password) VALUES(?,?)";
			PreparedStatement pstmt = c.prepareStatement(sql);
			pstmt.setString(1, username);
			pstmt.setString(2, hashedPW + ":" + salt);
			pstmt.executeUpdate();
			c.close();
		}
		catch(SQLException ex){
			System.out.println(ex);
		}
	}
	private static Boolean authenticateSession(UUID sessionId) {
		if (!activeSessions.contains(sessionId)){
			return false;
		}
		return true;
	}
	private static UUID createSessionID(){
		return UUID.randomUUID();
	}
	private static void RMISetup(){
		try {
		    String name = "PrintServer";
		    Printerface ps = new PrintServer();
		    Printerface stub = (Printerface) UnicastRemoteObject.exportObject(ps, 0);
		    Registry registry = LocateRegistry.getRegistry();
		    registry.rebind(name, stub);
		    System.out.println("PrintServer bound");
		} catch (Exception e) {
		    System.err.println("PrintServer exception:");
		    e.printStackTrace();
		}
	}

	public PrintServer() {
		super();
	}

	public UUID authenticateUser(String username, String password){
		ResultSet rs = null;
		String name = null;
		String hash = null;
		try{
			Connection c = DriverManager.getConnection("jdbc:sqlite:printer.db");
			String sql = "SELECT name,password FROM users WHERE name = ?";
			PreparedStatement pstmt = c.prepareStatement(sql);
			pstmt.setString(1, username);
			rs = pstmt.executeQuery();
			name = rs.getString("name");
			hash = rs.getString("password");
			c.close();
		}
		catch(SQLException ex){
			System.out.println(ex);
		}
		String[] split = hash.split(":");
		String userPW = hashPassword(password, split[1]);
		if (userPW.equals(split[0])) {
			try{
				logEvent(getEventTimeStamp() + " | " + "User " + username + " has been authenticated");
			}
			catch(IOException ex){
			}
			UUID sID = createSessionID();
			activeSessions.add(sID);
			return sID;
		}
		else {
			return new UUID(0L, 0L);
		}

	}

	public String print(String filename, String printer, UUID sId){
		if (!authenticateSession(sId)){ return "Unauthorized";};
		Random ran = new Random();
		int x = ran.nextInt(1600) + 5000;
		Pair<Integer,String> printjob = new Pair<Integer,String>(x, filename);
		queue.add(printjob);
		String message = filename + " is being processes by " + printer;
		try{
			SimpleDateFormat sdfr = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
			logEvent(getEventTimeStamp() + " | " + message);
		}
		catch(IOException ex){
		}
		return message;
	}

	public String queue(UUID sId){
		if (!authenticateSession(sId)){ return "Unauthorized";};
		String q = "";;
		for(Pair<Integer,String> printjob : queue){
			q += printjob.getKey() + " - " + printjob.getValue() + " | ";
		}
		return q;
	}

	public String topQueue(int job,UUID sId){
		if (!authenticateSession(sId)){ return "Unauthorized";};
		return "Job with id: " + job + "has been moved to the top of the print queue";
	}

	public String start(UUID sId){
		if (!authenticateSession(sId)){ return "Unauthorized";};
		return "Starting the print server...";
	}

	public String stop(UUID sId){
		if (!authenticateSession(sId)){ return "Unauthorized";};
		return "Stopping the print server...";
	}

	public String restart(UUID sId){
		if (!authenticateSession(sId)){ return "Unauthorized";};
		this.stop(sId);
		queue.clear();
		this.start(sId);
		return "";
	}

	public String status(UUID sId){
		if (!authenticateSession(sId)){ return "Unauthorized";};
		return "Status: All good.";
	}

	public String readConfig(String parameter,UUID sId){
		if (!authenticateSession(sId)){ return "Unauthorized";};
		return config.get(parameter);
	}

	public String setConfig(String parameter, String value,UUID sId){
		if (!authenticateSession(sId)){ return "Unauthorized";};
		config.put(parameter, value);
		return "";
	}

	public static void main(String[] args){
		RMISetup();
	}
}
