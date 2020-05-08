package server;

public class Message 
{
	
	private String msgContent;
	private boolean msgFinal = false;
	
	public Message(String msgContent, boolean msgFinal) 
	{
		this.msgContent = msgContent;
		this.msgFinal = msgFinal;
	}
	
	public Message(String msgContent) 
	{
		this(msgContent, false);
	}
	
	public boolean finalMsg()
	{
		return msgFinal;
	}
	
	public String getContent()
	{
		return msgContent;
	}
}
