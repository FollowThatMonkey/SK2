package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class User 
{
	
	private String name;
	private String password;
	private boolean onlineStatus = false;
	private BufferedReader buffRead;
	private BufferedWriter buffWrite;
	
	private BlockingQueue<Message> messages = new LinkedBlockingQueue<Message>();
	private List<String> friends = new ArrayList<String>(); 
	
	public User(String name, String password) 
	{
		this.name = name;
		this.password = password;
	}
	
	public BufferedReader getBuffRead()
	{
		return buffRead;
	}
	
	public void setBuffRead(BufferedReader buffRead)
	{
		this.buffRead = buffRead;
	}
	
	public BufferedWriter getBuffWrite()
	{
		return buffWrite;
	}
	
	public void setBuffWrite(BufferedWriter buffWrite)
	{
		this.buffWrite = buffWrite;
	}
	
	public void setOnlineStatus(boolean onlineStatus) 
	{
		this.onlineStatus = onlineStatus;
	}
	
	public boolean isOnline() 
	{
		return onlineStatus;
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getPassword()
	{
		return password;
	}
	
	public Message getMessage() throws InterruptedException
	{
		return messages.take();
	}
	
	public void sendMessage(String msg) throws InterruptedException
	{
		messages.put(new Message(msg));
	}
	
	public void sendMessage(String msg, boolean finalMsg) throws InterruptedException
	{
		messages.put(new Message(msg, finalMsg));
	}
	
	public void addFriend(String username)
	{
		friends.add(username);
	}
	
	public List<String> getFreinds()
	{
		return friends;
	}
	
}
