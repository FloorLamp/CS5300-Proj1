package cs5300.servlets;

import groupMembership.GroupMembershipManager;

import java.io.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.http.*;

import rpc.RPCClient;
import rpc.RPCServer;

public class SessionManager extends HttpServlet {
	
	private final static Logger LOGGER = Logger.getLogger(SessionManager.class.getName());
	
	// keeps track of which session ID to use
	private int lastSessionID = -1;
	
	// to stop server
	private boolean running = true;
	
	// This server's info
	private String thisIP;
	private int thisPort;
	
	// half an hour before expiration
	private static long EXPIRATION_TIME = 30*60;
	
	private static long DISCARD_TIME = 120*60;
	
	// Cookie name
	private static String cookieName = "CS53001AJSH263";
	
	// IP NULL
	public static String IP_NULL = "0.0.0.0";
	
	// locks for hash table
	public final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	public final Lock table_readlock = rwl.readLock();
	public final Lock table_writelock = rwl.writeLock();
	
	// RPCServers
	public RPCServer rpcServer = new RPCServer(this);
	
	//Group membership
	GroupMembershipManager gm;
	
	public SessionCache cache = new SessionCache();
	
	// Objects of this class are put in the session table sessionMap
	public static class SessionState {
		public String sessionID;
		public int version;
		public String message;
		public long expirationTime;
		
		// Automatically sets expiration time when instantiated
		public SessionState(int sID, String ipO, int portO, String msg, int v){
			sessionID = Integer.toString(sID) + "OF" + ipO + ":" + Integer.toString(portO);
			version = v;
			message = msg;
			Date date = new Date();
			expirationTime = date.getTime() + DISCARD_TIME*1000;
		}
		
		public SessionState(String sessID, String msg, int v){
			sessionID = sessID;
			version = v;
			message = msg;
			Date date = new Date();
			expirationTime = date.getTime() + DISCARD_TIME*1000;
		}
	}
	
	// the session table
	public ConcurrentHashMap<String, SessionState> sessionMap = new ConcurrentHashMap<String, SessionState>();
	
	// Thread/ variables for stopping it
	private boolean stopCleanup = false;
	private boolean cleanupStopped = false;
	Runnable cleanup = new Runnable() {
		public void run() {
			try {
				while(!stopCleanup){
					Iterator<String> iter = sessionMap.keySet().iterator();
					while(iter.hasNext()) {
						String str = iter.next();
						table_writelock.lock();
						SessionState state = sessionMap.get(str);
						Date date = new Date();
						if(state != null && state.expirationTime < date.getTime()){
							sessionMap.remove(str);
							
						}
						table_writelock.unlock();
					}
					Thread.sleep(1000);
				}
				System.out.println("Cleanup thread terminated");
				cleanupStopped = true;
		    } catch (Exception e){
		    	System.out.println(e.getMessage());
		    }
		}
	};
	
	// Start SessionManager, which starts the whole show
	// Get the RPC Server and GroupMembershipManager up and running
	public SessionManager(){
		try{
			thisIP = InetAddress.getLocalHost().getHostAddress();
			thisPort = rpcServer.getServerPort();
			new Thread(rpcServer).start();
			gm = new GroupMembershipManager(thisIP + ":" + Integer.toString(thisPort));
			gm.start();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	// Checks from existence of cookie whether we need a fresh session
	private boolean newSessionNeeded(SessionState state){
		return (state == null || state.expirationTime < new Date().getTime());
	}
	
	// Read the session whose ID is in the given cookie
	// Returns the session if found, null otherwise
	// req is an argument so we can tell how the read was completed
	private SessionState sessionRead(String cookie_value, HttpServletRequest req){
		SessionState state = null;
		
		String[] sInfo = cookie_value.split("S");
		String sID = sInfo[0];
		int version = Integer.parseInt(sInfo[1]);
		String ipP = (sInfo[2]).split(":")[0];
		int portP = Integer.parseInt((sInfo[2]).split(":")[1]);
		String ipB = (sInfo[3]).split(":")[0];
		int portB = Integer.parseInt((sInfo[3]).split(":")[1]);
		
		if(!ipP.equals(IP_NULL)){
			state = RPCClient.sessionRead(sID, version, ipP, portP);
			if(state == null && !ipB.equals(IP_NULL)){
				state = RPCClient.sessionRead(sID, version, ipB, portB);
				if(state != null){
					req.setAttribute("found", "IPP_BACKUP");
				}
				
			} else {
				req.setAttribute("found", "IPP_PRIMARY");
			}
		}
		if(state == null){
			LOGGER.info("State fetch failed");
		}
		return state;
	}
	
	// Delete the session whose ID is in the given cookie
	private void sessionDelete(String cookie_value){
		
		String[] sInfo = cookie_value.split("S");
		String sID = sInfo[0];
		int version = Integer.parseInt(sInfo[1]);
		String ipP = (sInfo[2]).split(":")[0];
		int portP = Integer.parseInt((sInfo[2]).split(":")[1]);
		String ipB = (sInfo[3]).split(":")[0];
		int portB = Integer.parseInt((sInfo[3]).split(":")[1]);
		
		if(!ipP.equals(IP_NULL)){
			RPCClient.sessionDelete(sID, version, ipP, portP);
		};
		
		if(!ipP.equals(IP_NULL)){
			RPCClient.sessionDelete(sID, version, ipB, portB);
		};
	}
	
	// Write session to some backup server
	// Returns the backup server, IPP_NULL if none was found
	private String sessionWrite(SessionState session, String cookie_value){
		String ipBackup_and_port = IP_NULL + ":0";
		String thisIPP = thisIP + ":" + Integer.toString(thisPort);
		
		boolean result = false;
		
		// First check the old cookie to attempt to write to old primary/backup
		if(cookie_value != null && !cookie_value.split("S")[2].equals(thisIPP)){
			result = RPCClient.sessionWrite(session, cookie_value.split("S")[2].split(":")[0],
										Integer.parseInt(cookie_value.split("S")[2].split(":")[1]));
			if(result){
				gm.addMember(cookie_value.split("S")[2]);
				ipBackup_and_port = cookie_value.split("S")[2];
			}
			else{
				gm.removeMember(cookie_value.split("S")[2]);
			}
		}
		
		if(!result && cookie_value != null && !cookie_value.split("S")[3].equals(thisIPP)){
			result = RPCClient.sessionWrite(session, cookie_value.split("S")[3].split(":")[0],
					Integer.parseInt(cookie_value.split("S")[3].split(":")[1]));
			
			if(result){
				gm.addMember(cookie_value.split("S")[3]);
				ipBackup_and_port = cookie_value.split("S")[3];
			}
			else{
				gm.removeMember(cookie_value.split("S")[3]);
			}
		}
		
		// failing to write to the formerly used servers, go through every other
		// server in the membership set until one responds positively
		if(!result){
			ArrayList<String> mbrList = new ArrayList<String>(gm.getMbrSet());
			mbrList.remove(thisIPP);
			if(cookie_value != null){
				mbrList.remove(cookie_value.split("S")[2]);
				mbrList.remove(cookie_value.split("S")[3]);
			}
			java.util.Collections.shuffle(mbrList);
			
			int i = 0;
			
			while(i < mbrList.size() && !result){
				String [] temp = mbrList.get(i).split(":");
				result = RPCClient.sessionWrite(session, temp[0], Integer.parseInt(temp[1]));
				if(result){
					ipBackup_and_port = mbrList.get(i);
					gm.addMember(mbrList.get(i));
					break;
				} else {
					gm.removeMember(mbrList.get(i));
				}
				i++;
			}
		}
		
		// Delete from ipPrimary/ipBackup server if they aren't holding the updated session
		if(cookie_value != null && !ipBackup_and_port.equals(cookie_value.split("S")[2])){
			RPCClient.sessionDelete(session.sessionID, session.version - 1,
					cookie_value.split("S")[2].split(":")[0],
					Integer.parseInt(cookie_value.split("S")[2].split(":")[1]));
		}
		if(cookie_value != null && !ipBackup_and_port.equals(cookie_value.split("S")[3])){
			RPCClient.sessionDelete(session.sessionID, session.version - 1,
					cookie_value.split("S")[3].split(":")[0],
					Integer.parseInt(cookie_value.split("S")[3].split(":")[1]));
		}
		
		return ipBackup_and_port;
	}
	
	// Create a new session and place it in the table
	private Cookie createNewSession(String greeting){
		// Create a new cookie
		int sessionID = ++lastSessionID;
		SessionState state = new SessionState(sessionID, thisIP, thisPort,
												greeting, 0);
		sessionMap.put(state.sessionID,
				       state);
		
		// Write to some backup server
		String ipB_portB = sessionWrite(state, null);
		
		return (new Cookie(cookieName,
				state.sessionID
				+ "S" + Integer.toString(state.version)
		        + "S" + thisIP + ":" + Integer.toString(thisPort)
		        + "S" + ipB_portB));
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		doPost(request, response);
	}
	
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
		throws ServletException, IOException {
		
		// Get cookies/current session if it exists
		Cookie[] cookies = request.getCookies();
		Cookie desiredCookie = null;
		if (cookies != null) {
			for(Cookie cookie: cookies) {
				if (cookieName.equals(cookie.getName())) {
					desiredCookie = cookie;
				}
			}
		}
		
		String cmd = request.getParameter("cmd");
		
		if (cmd != null && cmd.equals("RefreshMembership")) {
	    	  gm.refreshMembers();
	    } 
		
		if (cmd != null && cmd.equals("ServerCrash")) {
	    	  gm.setRunning(false);
	    	  rpcServer.setRunning(false);
	    	  this.running = false;
	    	  System.exit(0);
	    }
		
		SessionState state = null;
		request.setAttribute("expiration", "");
		request.setAttribute("logout", "false");
		request.setAttribute("serverID", thisIP + ":" + Integer.toString(thisPort));
		request.setAttribute("IPPPrimary", "");
		request.setAttribute("IPPBackup", "");
		request.setAttribute("found", "");
		request.setAttribute("discard", "");
		request.setAttribute("evicted", "");
		// Get cookie info if it exists, and then attempt to read the session
		// First read cache, then sessionRead for primary/backup
		// Place session into cache if found
		if(desiredCookie != null){
			LOGGER.info("Cookie exists");
			String cacheKey = desiredCookie.getValue().split("S")[0]
					+ "S" + desiredCookie.getValue().split("S")[1];
			state = cache.readFromCache(cacheKey);
			request.setAttribute("found", "CACHE");
			if(state == null){
				LOGGER.info("Cookie exists, session not found in cache");
				request.setAttribute("found", "");
				state = sessionRead(desiredCookie.getValue(),request);
				if(state == null){
					LOGGER.info("Session not found in server");
					request.setAttribute("logout", "true");
				} else {
					String evicted = cache.addToCache(cacheKey, state);
					request.setAttribute("evicted", evicted);
				}
			}
		}
		
		String greeting;
		
		if(cmd == null){cmd = "";} // So we don't keep checking if cmd is null
		
		// Check through command possibilities
		if( cmd.equals("LogOut") || ((String)(request.getAttribute("logout"))).equals("true")){ // We switch to a different view (sorta)
			table_writelock.lock();
			LOGGER.info("Logging out (or there was a failure");
			if(desiredCookie != null){
				sessionDelete(desiredCookie.getValue());
			} else {
				desiredCookie = new Cookie(cookieName, "");
			}
			desiredCookie.setMaxAge(0);
			table_writelock.unlock();
		}
		else {
			table_writelock.lock();
			if ( cmd.equals("Replace") ){
				greeting = request.getParameter("NewText");
				
				if(newSessionNeeded(state)){
					desiredCookie = createNewSession(greeting);
					// Awkward, but I liked the neatness of createNewSession
					state = sessionMap.get(desiredCookie.getValue().split("S",0)[0]);
				} else { // Update message and version if necessary
					//state = sessionMap.get(sessID);
					int version = state.version;
					if(!state.message.equals(greeting)){
						version = version + 1;
					}
					state = new SessionState(state.sessionID,
							greeting, version);
					sessionMap.put(state.sessionID, state);
					String ipB_portB = sessionWrite(state, desiredCookie.getValue());
					desiredCookie = new Cookie(cookieName,
							state.sessionID
							+ "S" + Integer.toString(state.version)
							+ "S" + thisIP + ":" + Integer.toString(thisPort)
					        + "S" + ipB_portB);
				}
			} else { // Command is refresh
				greeting = "Hello New User!";
				
				// If a new cookie is needed
				if(newSessionNeeded(state)){
					desiredCookie = createNewSession(greeting);
					state = sessionMap.get(desiredCookie.getValue().split("S",0)[0]);
				} else {
					if((state.expirationTime - EXPIRATION_TIME) < new Date().getTime()){
						state = new SessionState(state.sessionID,
								state.message, state.version);
						sessionMap.put(state.sessionID, state);
						String ipB_portB = sessionWrite(state, desiredCookie.getValue());
						
						desiredCookie = new Cookie(cookieName,
								state.sessionID
								+ "S" + Integer.toString(state.version)
								+ "S" + thisIP + ":" + Integer.toString(thisPort)
						        + "S" + ipB_portB);
						
						String cacheKey = desiredCookie.getValue().split("S")[0]
								+ "S" + desiredCookie.getValue().split("S")[1];
						
						cache.addToCache(cacheKey, state);
					} else {
						if( ((String)request.getAttribute("found")).equals("IPP_PRIMARY")
								&& desiredCookie.getValue().split("S")[3].split(":")[0].equals(IP_NULL)
								&& !(desiredCookie.getValue().split("S")[2].equals(thisIP + ":" + Integer.toString(thisPort)))){
							// If IPP_Backup is null, set this is as the backup
							sessionMap.put(state.sessionID, state);
							desiredCookie = new Cookie(cookieName,
									state.sessionID
									+ "S" + Integer.toString(state.version)
									+ "S" + desiredCookie.getValue().split("S")[2]
							        + "S" + thisIP + ":" + Integer.toString(thisPort));
						}
						desiredCookie = new Cookie(cookieName,
								state.sessionID
								+ "S" + Integer.toString(state.version)
								+ "S" + desiredCookie.getValue().split("S")[2]
						        + "S" + desiredCookie.getValue().split("S")[3]);
					}
				}
			}
			table_writelock.unlock();
			
			request.setAttribute("expiration", new Date());
			request.setAttribute("displayMsg", state.message);
			ArrayList<String> list = new ArrayList<String>(gm.getMbrSet());
			request.setAttribute("mbrList", list);
			
			desiredCookie.setMaxAge((int)(EXPIRATION_TIME + 60));
			
			request.setAttribute("discard", (new Date(state.expirationTime)).toString());
		}
		
		request.setAttribute("IPPPrimary", desiredCookie.getValue().split("S")[2]);
		request.setAttribute("IPPBackup", desiredCookie.getValue().split("S")[3]);
		
		// Set cookie and send out the request
		response.addCookie(desiredCookie);
		request.getRequestDispatcher("/app.jsp").forward(request, response);
	}
	
	// Starts clean up thread
	@Override
	public void init(ServletConfig config)
		throws ServletException {
		
		Thread cleanupThread = new Thread(cleanup);
		cleanupThread.start();
		
		super.init(config);
	}
	
	// Modified to destroy cleanup thread
	@Override
	public void destroy(){

		stopCleanup = true;
		System.out.println("Cleanup thread started");
		while(!cleanupStopped){
			try{
				Thread.sleep(100);
			}catch(Exception e){
				System.out.println(e.getMessage());
			}
		}
		
		super.destroy();
	}
}
