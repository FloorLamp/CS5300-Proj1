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
	
	protected static final Hashtable<String, Session> cache = new Hashtable<String, Session>();
	protected static final LinkedList<String> session_order = new LinkedList<String>();
	
	public SessionCache(){
		
	}
	
	public Session readFromCache(String string){
		readlock.lock();
		Session retval = cache.get(string);
		readlock.unlock();
		return retval;
	}
	
	public void addToCache(String string, Session session){
		writelock.lock();

		if (readFromCache(string) != null){
			session_order.remove(string);
		}
		
		// If it's not in the cache and the cache is full
		else if(session_order.size() > CACHE_SIZE){
			String str = session_order.pop();
			cache.remove(str);
		}
		
		cache.put(string, session);
		session_order.add(string);
		
		writelock.unlock();
	}
}
