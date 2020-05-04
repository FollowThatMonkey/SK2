package server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class User {
	
	private String name;
	private String password;
	private boolean onlineStatus = false;
	
	private BlockingQueue<Message> messages = new LinkedBlockingQueue<Message>();
	
	public User(String name, String password) 
	{
		this.name = name;
		this.password = password;
	}
	
	public void setOnlineStatus(boolean onlineStatus) 
	{
		this.onlineStatus = onlineStatus;
	}
	
	public boolean isOnline() 
	{
		return onlineStatus;
	}
	
}
