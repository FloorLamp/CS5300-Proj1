package session;

import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.*;

public class SessionManager {
	private static final long expiration = 600000;
	private static final String cookiename = "CS5300PROJECT1SESSION";	
	protected static final Integer sessionTimeout = 600; // Timeout time in seconds
	protected static final Integer sessionCleanerFrequency = 60; // Delay in

	protected static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	protected static final Lock readlock = rwl.readLock();
	protected static final Lock writelock = rwl.writeLock();
	private static Integer global = 0;
	protected static final ConcurrentHashMap<String, Session> hash = new ConcurrentHashMap<String, Session>();

	protected static final SessionCleaner sessionCleaner = new SessionCleaner();
	protected static final Timer sessionCleanerTimer = new Timer();

	public static Session sessionRead(String SID, Integer change_count) {
		System.out.println("Server trying to retrieve: " + SID + "," + change_count);
		readlock.lock();
		Session session = hash.get(SID);
		if (session==null) { 
			readlock.unlock();
			return null; }
		else { 
			readlock.unlock();
			return session; 
		}	
	}
	
	public static void sessionWrite(String SID, String message, Integer version) {
		long now = System.currentTimeMillis();
		System.out.println("server adding session: " + SID +"," + version);
		writelock.lock();
		Session session = hash.get(SID);
		if (session == null) {
			session = new Session(version,message,now+expiration);
			hash.put(SID,session);
		}
		session.setChangecount(version);
		session.setMessage(message);
		session.setExpiration(now+expiration);
		hash.put(SID, session);
		writelock.unlock();
	}
	
	public static void sessionWriteBackup(String SID, String message, Integer version, Long expire) {
		System.out.println("server adding session: " + SID +"," + version);
		writelock.lock();
		Session session = hash.get(SID);
		if (session == null) {
			session = new Session(version,message,expire);
			hash.put(SID,session);
		}
		session.setChangecount(version);
		session.setMessage(message);
		session.setExpiration(expire);
		hash.put(SID, session);
		writelock.unlock();
	}
	
	public static void sessionDelete(String SID, Integer change_count) {
		System.out.println("Server trying to delete: " + SID + "," + change_count);
		writelock.lock();
		hash.remove(SID);
		writelock.unlock();
	}
	 
	public static Session getAndIncrement(HttpServletRequest request) {
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
			session = new Session(new Integer(0),"",expiration+date);
			session.setSessionnum(global);
			global++;			
		} else {
			readlock.lock();
			String value = cookie.getValue();
			String[] split = value.split("_");
			session = hash.get(split[0]+"_"+split[1]);
			readlock.unlock();
			// If we are unable to get session
			if (session == null) {
				session = new Session(new Integer(0),"",expiration+date);
				session.setSessionnum(global);
				global++;
			} else {
				session.setChangecount(session.getChangecount()+1);
				session.setExpiration(expiration+date);
			}
		}
		return session;
	}

	public static void putCookie(HttpServletResponse response, Session sess) {
		Cookie cookie = new Cookie(cookiename, sess.toString());
		cookie.setMaxAge(sessionTimeout);
		response.addCookie(cookie);
	}

	public static void destroyCookie(HttpServletRequest request,
			HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();
		Cookie cookie = null;
		if (cookies != null) {
			for (int is = 0; is < cookies.length; is++) {
				Cookie c = cookies[is];
				if (c.getName().equals(cookiename)) {
					cookie = c;
				}
			}
		}
		if (cookie != null) {
			cookie = new Cookie(cookiename, "");
			cookie.setMaxAge(0);
			response.addCookie(cookie);
		}
	}

	public static void startCleaner() {
		sessionCleanerTimer.schedule(sessionCleaner,
				sessionCleanerFrequency * 1000, sessionCleanerFrequency * 1000);
	}

	public static void donecleanup() {
		sessionCleanerTimer.cancel();
	}
}
