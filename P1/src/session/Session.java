package session;

import groupMembership.Server;

import java.util.Date;

public class Session {
	private Server creator;
	private Integer sess_num;
	private Integer change_count;
	private Server primary,backup;
	private Date timestamp;
	private String message;
	
	public Session(Server creator,Integer sess_num,
			Server primary, Server backup) {
		this.setCreator(creator);
		this.setSess_num(sess_num);
		this.setPrimary(primary);
		this.setBackup(backup);
		this.change_count=0;
		this.timestamp = new Date();
	}

	public void updateTimestamp() {
		this.timestamp = new Date();
	}
	//Gets timestamp of session in seconds since epoch
	public long getTimestamp() {
		return timestamp.getTime()/1000;
	}

	public void incrementVersion() {
		this.change_count++;
	}

	public Server getCreator() {
		return creator;
	}

	public void setCreator(Server creator) {
		this.creator = creator;
	}
	
	public Integer getSess_num() {
		return sess_num;
	}

	public void setSess_num(Integer sess_num) {
		this.sess_num = sess_num;
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

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public String toString() {
		return sess_num+","+creator.toString()+","+","+
				change_count+","+primary.toString()+","+
				backup.toString();
	}
}
