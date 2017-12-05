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

	private static List<Pair<String, String>> activeSessions = new ArrayList<Pair<String, String>>();

	private Queue<Pair<Integer,String>> queue = new LinkedList<Pair<Integer,String>>();

	private Map<String,String> config = new HashMap<String, String>();

	private static List<Pair<String, String>> accessControlList = new ArrayList<Pair<String, String>>();

	private static List<Pair<String, String>> rolesList = new ArrayList<Pair<String, String>>();

	private static Map<String, Map<String,Integer>> rolesAndOperations = new HashMap<String, Map<String,Integer>>();

	private static final Boolean USE_RBAC = true;

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

	private static void registerUser(String username, String firstname, String lastname, String password, String role){
		String salt = String.valueOf((int)(Math.random() * 1000000000 + 1));
		String hashedPW = hashPassword(password, salt);
		try{
			logEvent(getEventTimeStamp() + " | " + "User " + username + " was registered");
		}
		catch(IOException ex){
		}
		try{
			Connection c = DriverManager.getConnection("jdbc:sqlite:printer.db");
			String sql = "INSERT INTO users(username,firstname,lastname,password,role) VALUES(?,?,?,?,?)";
			PreparedStatement pstmt = c.prepareStatement(sql);
			pstmt.setString(1, username);
			pstmt.setString(2, firstname);
			pstmt.setString(3, lastname);
			pstmt.setString(4, hashedPW + ":" + salt);
			pstmt.setString(5, role);
			pstmt.executeUpdate();
			c.close();
		}
		catch(SQLException ex){
			System.out.println(ex);
		}
	}

	private static Boolean authenticateSession(String sessionId) {
		for (Pair<String, String> s : activeSessions){
			if (s.getValue().equals(sessionId)){
				return true;
			}
		}
		return false;
	}

	private static String createSessionID(){
		return UUID.randomUUID().toString();
	}

	private static void printOutRolesAndOps(){
		for(String k: rolesAndOperations.keySet()){
			String key = k.toString();
			Map<String,Integer> value = rolesAndOperations.get(k);
			System.out.println(key);
			for(String o: value.keySet()){
				String opkey = o.toString();
				String vv = value.get(o).toString();
				System.out.println(opkey + " - " + vv);
			}
		}
	}

	private static void populateAccessControlList(){
		ResultSet rs = null;
		try{
			Connection c = DriverManager.getConnection("jdbc:sqlite:printer.db");
			String sql = "SELECT username,operation FROM accesscontrollist";
			PreparedStatement pstmt = c.prepareStatement(sql);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				Pair<String,String> access = new Pair<String,String>(rs.getString("username"), rs.getString("operation"));
				accessControlList.add(access);
			}
			c.close();
		}
		catch(SQLException ex){
			System.out.println(ex);
		}
	}

	private static void populateRolesList(){
		ResultSet rs = null;
		try{
			Connection c = DriverManager.getConnection("jdbc:sqlite:printer.db");
			String sql = "SELECT username,role FROM users";
			PreparedStatement pstmt = c.prepareStatement(sql);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				Pair<String,String> role = new Pair<String,String>(rs.getString("username"), rs.getString("role"));
				rolesList.add(role);
			}
			c.close();
		}
		catch(SQLException ex){
			System.out.println(ex);
		}
	}

	private static void populateRolesAndOperations(){
		ResultSet rs = null;
		try{
			Connection c = DriverManager.getConnection("jdbc:sqlite:printer.db");
			String sql = "SELECT * FROM roles";
			PreparedStatement pstmt = c.prepareStatement(sql);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String role = rs.getString("role");
				Map<String, Integer> ops = new HashMap<String, Integer>();
			       	ops.put("print", rs.getInt("print"));	
			       	ops.put("queue", rs.getInt("queue"));	
			       	ops.put("topQueue", rs.getInt("topQueue"));	
			       	ops.put("start", rs.getInt("start"));	
			       	ops.put("stop", rs.getInt("stop"));	
			       	ops.put("restart", rs.getInt("restart"));	
			       	ops.put("status", rs.getInt("status"));	
			       	ops.put("readConfig", rs.getInt("readConfig"));	
			       	ops.put("setConfig", rs.getInt("setConfig"));	
				rolesAndOperations.put(role, ops);
			}
			c.close();
		}
		catch(SQLException ex){
			System.out.println(ex);
		}
	}

	private static Boolean hasAccess(String sessionId, String operation){
		String username = null;

		//Get the current username from the session id
		for(Pair<String, String> s : activeSessions){
			if (s.getValue().equals(sessionId)) {
				username = s.getKey();
			}
		}

		if (USE_RBAC) {
			//Use RBAC for access control
			String role = null;

			for(Pair<String, String> r : rolesList){
				if (r.getKey().equals(username)) {
					role = r.getValue();
				}
			}

			if (role == null) return false;
			Integer access = rolesAndOperations.get(role).get(operation);
			if (access != 0) return true;
		}
		else {
			//Use access control list for access control
			for(Pair<String, String> a : accessControlList){
				if (a.getKey().equals(username) && a.getValue().equals(operation)) {
					return true;
				}
			}
		}

		return false;
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
		populateAccessControlList();
		populateRolesList();
		populateRolesAndOperations();
	}

	public String authenticateUser(String username, String password){
		ResultSet rs = null;
		String name = null;
		String hash = null;
		try{
			Connection c = DriverManager.getConnection("jdbc:sqlite:printer.db");
			String sql = "SELECT username,password FROM users WHERE username = ?";
			PreparedStatement pstmt = c.prepareStatement(sql);
			pstmt.setString(1, username);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				name = rs.getString("username");
				hash = rs.getString("password");
			}
			else {
				return new UUID(0L, 0L).toString();
			}
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
			String sessionId = createSessionID();
			Pair<String, String> activeSession = new Pair<String, String>(username, sessionId);
			activeSessions.add(activeSession);
			return sessionId;
		}
		else {
			return new UUID(0L, 0L).toString();
		}

	}

	public String print(String filename, String printer, String sessionId){
		if (!authenticateSession(sessionId)){ return "Unauthorized";};
		if (!hasAccess(sessionId, Thread.currentThread().getStackTrace()[1].getMethodName())){ return "You do not have access to this operation"; };
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

	public String queue(String sessionId){
		if (!authenticateSession(sessionId)){ return "Unauthorized";};
		if (!hasAccess(sessionId, Thread.currentThread().getStackTrace()[1].getMethodName())){ return "You do not have access to this operation"; };
		String q = "";
		for(Pair<Integer,String> printjob : queue){
			q += printjob.getKey() + " - " + printjob.getValue() + " | ";
		}
		return q;
	}

	public String topQueue(int job,String sessionId){
		if (!authenticateSession(sessionId)){ return "Unauthorized";};
		if (!hasAccess(sessionId, Thread.currentThread().getStackTrace()[1].getMethodName())){ return "You do not have access to this operation"; };
		return "Job with id: " + job + " has been moved to the top of the print queue";
	}

	public String start(String sessionId){
		if (!authenticateSession(sessionId)){ return "Unauthorized";};
		if (!hasAccess(sessionId, Thread.currentThread().getStackTrace()[1].getMethodName())){ return "You do not have access to this operation"; };
		return "Starting the print server...";
	}

	public String stop(String sessionId){
		if (!authenticateSession(sessionId)){ return "Unauthorized";};
		if (!hasAccess(sessionId, Thread.currentThread().getStackTrace()[1].getMethodName())){ return "You do not have access to this operation"; };
		return "Stopping the print server...";
	}

	public String restart(String sessionId){
		if (!authenticateSession(sessionId)){ return "Unauthorized";};
		if (!hasAccess(sessionId, Thread.currentThread().getStackTrace()[1].getMethodName())){ return "You do not have access to this operation"; };
		this.stop(sessionId);
		queue.clear();
		this.start(sessionId);
		return "Restarting the print server...hold on...";
	}

	public String status(String sessionId){
		if (!authenticateSession(sessionId)){ return "Unauthorized";};
		if (!hasAccess(sessionId, Thread.currentThread().getStackTrace()[1].getMethodName())){ return "You do not have access to this operation"; };
		return "Status: All good.";
	}

	public String readConfig(String parameter,String sessionId){
		if (!authenticateSession(sessionId)){ return "Unauthorized";};
		if (!hasAccess(sessionId, Thread.currentThread().getStackTrace()[1].getMethodName())){ return "You do not have access to this operation"; };
		return config.get(parameter);
	}

	public String setConfig(String parameter, String value,String sessionId){
		if (!authenticateSession(sessionId)){ return "Unauthorized";};
		if (!hasAccess(sessionId, Thread.currentThread().getStackTrace()[1].getMethodName())){ return "You do not have access to this operation"; };
		config.put(parameter, value);
		return "Config '"+ parameter  +"' was set successfully";
	}

	public static void main(String[] args){
		RMISetup();

	}
}
