package session;

import groupMembership.Server;

public class Session {
	private Integer sessionnum;
	private Server IPP;
	private Integer changecount;
	private String message;
	private Long expiration;
	private Server primary,backup;
	
	public Session(Integer a, String b, Long c) {
		this.setChangecount(a);
		this.setMessage(b);
		this.setExpiration(c);
	}
	
	public Server getPrimary() {
		return primary;
	}

	public void setPrimary(Server primary) {
		this.primary = primary;
	}
	
	public Server getBackup() {
		return backup;
	}

	public void setBackup(Server backup) {
		this.backup = backup;
	}

	public Integer getChangecount() {
		return changecount;
	}

	public void setChangecount(Integer changecount) {
		this.changecount = changecount;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Long getExpiration() {
		return expiration;
	}

	public void setExpiration(Long expiration) {
		this.expiration = expiration;
	}

	public Server getIPP() {
		return IPP;
	}

	public void setIPP(Server iPP) {
		IPP = iPP;
	}

	public Integer getSessionnum() {
		return sessionnum;
	}

	public void setSessionnum(Integer sessionnum) {
		this.sessionnum = sessionnum;
	}
	
	public String getSID() {
		return this.sessionnum.toString() + this.IPP.toString();
	}
	
	public String toString() {
		return sessionnum+"_"+IPP.toString()+"_"+changecount+
				"_"+primary.toString()+"_"+backup.toString();
	}
	
}
