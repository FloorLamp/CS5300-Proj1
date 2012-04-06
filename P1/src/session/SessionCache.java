package session;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SessionCache {
	private static final int CACHE_SIZE = 2;
	
	protected static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	protected static final Lock readlock = rwl.readLock();
	protected static final Lock writelock = rwl.writeLock();
	
	protected static final Hashtable<String, Session> cached_sessions = new Hashtable<String, Session>();
	protected static final LinkedList<String> session_order = new LinkedList<String>();
	
	public SessionCache(){
		
	};
	
	public Session readFromCache(){
		
	}
	
	public void addToCache(String string, Session session){
		
	}
}
