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
	private static Hashtable<String,Session> hash;
	
	private GroupMembershipManager gmm;

	public Server(InetAddress sip, int sport) {
		ip = sip;
		port = sport;
		
		gmm = new GroupMembershipManager(this.toString());
		gmm.start();
	}

	public Set<String> getMbrSet() {
		return gmm.getMbrSet();
	}
	
	public String toString() {
		return IPP;
	}

	public Hashtable<String,Session> getHash() {
		return hash;
	}

	public void setHash(Hashtable<String,Session> hash) {
		this.hash = hash;
	}

	public static String toString(String i) {
		Session hv = hash.get(i);
		return i+"_"+hv.getIPP().toString()+"_"+hv.getChangecount()
				+"_"+hv.getPrimary().toString()+"_"+hv.getBackup().toString();
	}
	
}
