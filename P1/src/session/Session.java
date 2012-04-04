package session;

import groupMembership.Server;

public class Session {
	private Integer changecount;
	private String message;
	private Long expiration;
	private Server primary,backup;
	
	Session(Integer a, String b, Long c) {
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
	
}
