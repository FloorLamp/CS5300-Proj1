package cs5300.servlets;

import java.io.*;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.*;
import javax.servlet.http.*;

public class TestProject extends HttpServlet {
	
	// keeps track of which session ID to use
	private int lastSessionID = -1;
	
	// half an hour before expiration
	private static long EXPIRATION_TIME = 30*60;
	private static String cookieName = "CS53001AJSH263";
	
	// Objects of this class are put in the session table sessionMap
	static class SessionState {
		public int sessionID;
		public int version;
		public String message;
		public long expirationTime;
		
		// Automatically sets expiration time when instantiated
		public SessionState(int sID, String msg, int v){
			sessionID = sID;
			version = v;
			message = msg;
			Date date = new Date();
			expirationTime = date.getTime() + EXPIRATION_TIME*1000;
		}
	}
	
	// the session table
	ConcurrentHashMap<String, SessionState> sessionMap = new ConcurrentHashMap<String, SessionState>();
	
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
						SessionState state = sessionMap.get(str);
						Date date = new Date();
						if(state != null && state.expirationTime < date.getTime()){
							sessionMap.remove(str);
						}
					}
					Thread.sleep(50);
				}
				System.out.println("Cleanup thread terminated");
				cleanupStopped = true;
		    } catch (Exception e){
		    	System.out.println(e.getMessage());
		    }
		}
	};
	
	// Checks from existence of cookie whether we need a fresh session
	private boolean newSessionNeeded( Cookie cookie, String sessID){
		return (cookie == null
				|| sessionMap.get(sessID) == null
				|| sessionMap.get(sessID).expirationTime < (new Date().getTime()));
	}
	
	// Create a new session and place it in the table
	private Cookie createNewSession(String greeting){
		// Create a new cookie
		int sessionID = ++lastSessionID;
		SessionState state = new SessionState(sessionID, greeting, 0);
		sessionMap.put(Integer.toString(sessionID), state);
		return (new Cookie(cookieName,
				Integer.toString(sessionID) + "S0"));
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
		
		// Get cookie info if it exists
		String sessID = null;
		if(desiredCookie != null){
			String s = desiredCookie.getValue();
			String[] split = s.split("S");
			sessID = split[0];
		}
		
		SessionState state = null;
		String greeting;
		
		String cmd = request.getParameter("cmd");
		if(cmd == null){cmd = "";} // So we don't keep checking if cmd is null
		
		// Check through command possibilities
		if( cmd.equals("LogOut") ){ // We switch to a different view (sorta)
			if(desiredCookie != null){
				sessionMap.remove(sessID);
				desiredCookie.setMaxAge(0);
			}
		}
		else {
			if ( cmd.equals("Replace") ){
				greeting = request.getParameter("NewText");
				
				if(newSessionNeeded(desiredCookie, sessID)){
					desiredCookie = createNewSession(greeting);
					// Awkward, but I liked the neatness of createNewSession
					state = sessionMap.get(desiredCookie.getValue().split("S",0)[0]);
				} else { // Update message and version if necessary
					state = sessionMap.get(sessID);
					int version = state.version;
					if(!state.message.equals(greeting)){
						version = version + 1;
					}
					state = new SessionState(state.sessionID, greeting, version);
					sessionMap.put(sessID, state);
					desiredCookie = new Cookie(cookieName,
							Integer.toString(state.sessionID) + "S" + Integer.toString(version));	
				}
			} else {
				greeting = "Hello New User!";
				
				// If a new cookie is needed
				if(newSessionNeeded(desiredCookie, sessID)){
					desiredCookie = createNewSession(greeting);
					state = sessionMap.get(desiredCookie.getValue().split("S",0)[0]);
				} else {
					state = sessionMap.get(sessID);
					state = new SessionState(state.sessionID, state.message, state.version);
					sessionMap.put(sessID, state);
					desiredCookie = new Cookie(cookieName,
							Integer.toString(state.sessionID) + "S" + Integer.toString(state.version));
				}
			}
			
			request.setAttribute("expiration", new Date(state.expirationTime));
			request.setAttribute("displayMsg", state.message);

			desiredCookie.setMaxAge((int)(EXPIRATION_TIME + 60));
		}
		
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
