package groupMembership;

import java.net.InetAddress;
import java.util.Set;

public class Server {
	public InetAddress ip;
	public Integer port;
	private final String IPP = ip+"-"+port;
	private final static String IPPnull = "000000000-0";
	public static GroupMembershipManager gm;
	
	public Server(InetAddress sip, int sport) {
		ip = sip;
		port = sport;		
	}
	
	public String toString() {
		return IPP;
	}

	public static String getNull() {
		return IPPnull;
	}

	
}
