package server;

public class Message 
{
	
	private String msgContent; // Tekst wiadomości
	private boolean msgFinal = false; // Jeśli to jest ostatnia wiadomość (połączenie zakończone), to true
	
	public Message(String msgContent, boolean msgFinal) 
	{
		this.msgContent = msgContent; // Inicjalizacja zmiennych
		this.msgFinal = msgFinal;
	}
	
	public Message(String msgContent) 
	{
		this(msgContent, false); // Inicjalizacja zmiennych (zmienna msgFinal = false)
	}
	
	public boolean finalMsg() // Zwrócenie zmiennej msgFinal
	{
		return msgFinal;
	}
	
	public String getContent() // Zwrócenie tekstu wiadomości
	{
		return msgContent;
	}
}
