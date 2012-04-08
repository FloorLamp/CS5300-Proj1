package groupMembership;

import java.net.InetAddress;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;

import session.Session;

public class Server {
	public InetAddress ip;
	public Integer port;
	private final String IPP = ip+"-"+port;
	private final static String IPPnull = "000000000-0";
	
	private GroupMembershipManager gmm;

	public Server(InetAddress sip, int sport) {
		ip = sip;
		port = sport;
		
		gmm = new GroupMembershipManager(this);
		gmm.start();
	}
	
	// Does not start a group membership manager. Used by gmm
	public Server(String address) {
		String[] addr = address.split("-");
		try {
			ip = InetAddress.getByName(addr[0]);
			port = Integer.parseInt(addr[1]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Set<Server> getMbrSet() {
		return gmm.getMbrSet();
	}
	
	public void refreshMembership() {
		gmm.refreshMembers();
	}
	
	public void setGroupMembershipRunning(boolean running) {
		gmm.running = running;
	}
	
	public String toString() {
		return IPP;
	}

	public static String getNull() {
		return IPPnull;
	}

	
}
