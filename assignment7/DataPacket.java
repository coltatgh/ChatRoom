package assignment7;

import java.io.Serializable;

public class DataPacket implements Serializable{
	private String meta;
	private int group;
	private String message;
	private String[] usernames;
	
	public DataPacket(String meta, int group, String message){
		this.meta = meta;
		this.group = group;
		this.message = message;
		usernames = null;
	}
	
	public DataPacket(String meta, int group, String[] usernames) {
		this.meta = meta;
		this.group = group;
		this.usernames = usernames;
		message = null;
	}
	
	public DataPacket(String meta, int group, String message, String[] usernames){
		this.meta = meta;
		this.group = group;
		this.message = message;
		this.usernames = usernames;
		//message = null;
	} 
	
	public DataPacket(String meta, int group, String[] usernames, String message){
		this.meta = meta;
		this.group = group;
		this.usernames = usernames;
		this.message = message;		// was message = message
	} 
	
	public String getMeta() { return meta; }
	public int getGroup() { return group; }
	public String getMessage() { return message; }
	public String[] getUsernames() { return usernames; }
	
	public void setMeta(String meta) { this.meta = meta; }
	public void setGroup(int group) { this.group = group; }
	public void setMessage(String message) { this.message = message; }
	public void setUsernames(String[] usernames) { this.usernames = usernames; }
}
