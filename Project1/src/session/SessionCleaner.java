package session;

import java.util.Date;
import java.util.Enumeration;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class SessionCleaner extends TimerTask {
	//Compare the time of each entry in Session table with the time now
	//If the difference is greater than SessionManager.sessionTimeout, remove the session
	public void run() {		
		SessionManager.writelock.lock();
		long now = (new Date().getTime());
		ConcurrentHashMap<String,Session> h = SessionManager.hash;
		for(Enumeration<String> e = h.keys(); e.hasMoreElements();) {
			String key = e.nextElement();
			Session se = h.get(key);
			if((now+SessionManager.sessionTimeout*1000) >= se.getExpiration()) {
				h.remove(key);
				System.out.println("Removed "+key);
			}
		}
		System.out.println("Cleaner Run");
		SessionManager.writelock.unlock();
	}
}

