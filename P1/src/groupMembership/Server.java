package groupMembership;

import java.net.InetAddress;

public class Server {
	public InetAddress ip;
	public Integer port;

	Server(InetAddress sip, int sport) {
		ip = sip;
		port = sport;
	}
	
	public String toString() {
		return ip+":"+port;
	}
}
