/*
 * Autorzy: Rafał Jarmakiewicz i Zuzanna Łaska
 * Data modyfikacji: 19.05.2020r.
 * 
 */

package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class User 
{
	
	private String name; // Nazwa użytkownika
	private String password; // Hasło użytkownika
	private boolean onlineStatus = false; // Status online
	private BufferedReader buffRead; // Bufor wejścia (dane odbierane od klienta)
	private BufferedWriter buffWrite; // Bufor wyjścia (dane wysyłane do klienta)
	
	private BlockingQueue<Message> messages = new LinkedBlockingQueue<Message>(); // Kolejka wiadomości skierowanych do użytkownika  
	private List<String> friends = new ArrayList<String>(); // Lista znajomych użytkownika
	
	public User(String name, String password) 
	{
		this.name = name; // Inicjalizacja zmiennych
		this.password = password;
	}
	
	public BufferedReader getBuffRead() // Zwrócenie obiektu buffRead
	{
		return buffRead;
	}
	
	public void setBuffRead(BufferedReader buffRead) // Przypisanie obiektu buffRead
	{
		this.buffRead = buffRead;
	}
	
	public BufferedWriter getBuffWrite() // Zwrócenie obiektu buffWrite
	{
		return buffWrite;
	}
	
	public void setBuffWrite(BufferedWriter buffWrite) // Przypisanie obiektu buffWrite
	{
		this.buffWrite = buffWrite;
	}
	
	public void setOnlineStatus(boolean onlineStatus) // Ustawienie zmiennej onlineStatus (statusu aktywności) 
	{
		this.onlineStatus = onlineStatus;
	}
	
	public boolean isOnline() // Zwrócenie zmiennej onlineStatus (sprawdzenie czy użytkownik jest online)
	{
		return onlineStatus;
	}
	
	public String getName() // Zwrócenie zmiennej name
	{
		return name;
	}
	
	public String getPassword() // Zwrócenie zmiennej password
	{
		return password;
	}
	
	public Message getMessage() throws InterruptedException // Pobranie wiadomości z kolejki
	{
		return messages.take();
	}
	
	public void sendMessage(String msg) throws InterruptedException // Wstawienie wiadomości do kolejki
	{
		messages.put(new Message(msg));
	}
	
	public void sendMessage(String msg, boolean finalMsg) throws InterruptedException // Wstawienie wiadomości do kolejki
	{
		messages.put(new Message(msg, finalMsg));
	}
	
	public void addFriend(String username) // Dodanie użytkownika do listy znajomych
	{
		friends.add(username);
	}
	
	public void deleteFriend(String username) // Usunięcie użytkownika z listy znajomych
	{
		friends.remove(username);
	}
	
	public List<String> getFriends() // Zwrócenie listy znajomych
	{
		return friends;
	}
	
}
