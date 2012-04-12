package cs5300.servlets;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SessionCache {
	private static final int CACHE_SIZE = 2;
	
	// Locks
	protected static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	protected static final Lock readlock = rwl.readLock();
	protected static final Lock writelock = rwl.writeLock();
	
	// Initialize data structurs
	protected static final Hashtable<String, SessionManager.SessionState> cache = new Hashtable<String, SessionManager.SessionState>();
	protected static final LinkedList<String> session_order = new LinkedList<String>();
	
	public SessionCache(){
		
	}
	
	public SessionManager.SessionState readFromCache(String string){
		readlock.lock();
		SessionManager.SessionState retval = cache.get(string);
		readlock.unlock();
		return retval;
	}
	
	public String addToCache(String string, SessionManager.SessionState session){
		writelock.lock();

		String removed = "";
		
		if (readFromCache(string) != null){
			session_order.remove(string);
		}
		
		// If it's not in the cache and the cache is full
		else if(session_order.size() == CACHE_SIZE){
			String str = session_order.pop();
			cache.remove(str);
			removed = str;
		}
		
		cache.put(string, session);
		session_order.add(string);
		
		writelock.unlock();
		
		return removed;
	}
}