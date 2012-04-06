package session;

import groupMembership.HttpServletRequest;
import groupMembership.HttpServletResponse;
import groupMembership.Server;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.*;


@WebServlet("/SessionManager")
public class SessionManager {
	private static final long expiration = 5000;
	private static final String cookiename = "CS5300PROJECT1SESSION";	
	private static final Integer sessionTimeout = 600; // Timeout time in seconds
	// of session
	protected static final Integer sessionCleanerFrequency = 60; // Delay in
	// seconds for
	// running cleaner

	// Locks for session table
	protected static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	protected static final Lock readlock = rwl.readLock();
	protected static final Lock writelock = rwl.writeLock();

	// Sweeper for session cleanup
	protected static final SessionCleaner sessionCleaner = new SessionCleaner();
	protected static final Timer sessionCleanerTimer = new Timer();

	public static Session getAndIncrement(HttpServletRequest request, Server s) {
		long date = System.currentTimeMillis();
		Cookie[] cookies = request.getCookies();
		Cookie cookie = null;
		Session session;
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie c = cookies[i];
				if (c.getName().equals(cookiename)) {
					cookie = c;
				}
			}
		}
		if (cookie == null) {
			session = new Session(s,new Integer(0),"",expiration+date);
		} else {
			session = s.getHash().get(cookie.getValue());
			// If we are unable to get session
			if (session == null) {
				session = new Session(s,new Integer(0),"",expiration+date);
			} else {
				session.setChangecount(session.getChangecount()+1);
			}
		}
		return session;
	}

	public static void putCookie(HttpServletResponse response, Server s, String i) {
		Cookie cookie = new Cookie(cookiename, s.toString(i));
		cookie.setMaxAge(600);
		response.addCookie(cookie);
	}

	public static void destroyCookie(HttpServletRequest request,
			HttpServletResponse response, Server s, Session session, String i) {
		Cookie[] cookies = request.getCookies();
		Cookie cookie = null;
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie c = cookies[i];
				if (c.getName().equals(cookiename)) {
					cookie = c;
				}
			}
		}
		if (cookie != null) {
			cookie = new Cookie(cookiename, s.toString(i));
			cookie.setMaxAge(0);
			response.addCookie(cookie);
		}
	}

	public static void startCleaner() {
		sessionCleanerTimer.schedule(sessionCleaner,
				sessionCleanerFrequency * 1000, sessionCleanerFrequency * 1000);
	}

	public static void cleanup() {
		sessionCleanerTimer.cancel();
	}

	public static Session getSessionById(String sessionID, Integer version, Server s) {
		System.out.println("Server trying to retrieve: " + sessionID + "," + version);
		readlock.lock();
		Session session = s.getHash().get(sessionID);
		if (session == null) {
			System.out.println("server doesn't have session");
			return null;
		} else {
			if (session.getChangecount().equals(version)) {
				return session;
			} else {
				System.out.println(session.getChangecount() + " does not match with " + version);
				return null;
			}
		}
		readlock.unlock();
	}

	public static void putSession(Server s, String sessionid, String version, Integer count,
			String message) {
		long date = System.currentTimeMillis();
		System.out.println("server adding session: " + sessionid +"," + version);
		writelock.lock();
		Session session = s.getHash().get(sessionid);
		if (session == null) {
			session = new Session(s, 0, message, date+expiration);
			Hashtable<String,Session> h = s.getHash();
			h.put(sessionid, session);
		}
		else {
			session.setChangecount(count);
			session.setMessage(message);
			session.setExpiration(date+expiration);
		}
		writelock.unlock();
	}
}
