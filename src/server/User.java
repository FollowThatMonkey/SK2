package server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class User 
{
	
	private String name;
	private String password;
	private boolean onlineStatus;
	
	private BlockingQueue<Message> messages = new LinkedBlockingDeque<Message>();
	
	
	public User() 
	{
		
	}
}
