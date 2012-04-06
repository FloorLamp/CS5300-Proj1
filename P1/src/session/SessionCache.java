package session;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SessionCache {
	private static final int CACHE_SIZE = 2;
	
	protected static final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	protected static final Lock readlock = rwl.readLock();
	protected static final Lock writelock = rwl.writeLock();
	
	
}
