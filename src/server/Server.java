package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server 
{
	
	public final int PORT; // PORT na którym działa serwer
	public final int nConnections; // Liczba hostów którzy będą akceptowani na raz
	
	private ServerSocket serverSocket; // Socket serwera
	private List<User> users = new ArrayList<User>();
	
	public Server(int PORT, int nConnections) throws IOException 
	{
		this.PORT = PORT; // Przypisanie portu
		this.nConnections = nConnections; // Przypisanie max liczby połączeń

		// należy dodać wczytywanie użytkowników z pliku (użytkowników i hasła)
		loadUsers();		
		
		serverSocket = new ServerSocket(this.PORT);
		initServerThreads();
	}
	
	private void loadUsers() 
	{
		File loadFile = new File("./users.dat");
		if(!loadFile.exists()) 
		{ // jeśli nie istnieje plik to go utwórz
			try 
			{
				loadFile.createNewFile();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		} 
		else 
		{
			try 
			{
				Scanner scanner = new Scanner(loadFile);
				
				while(scanner.hasNextLine()) 
				{
					String[] line = scanner.nextLine().split(" ");
					
					users.add(new User(line[0], line[1]));
				}
			} 
			catch (FileNotFoundException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	private void initServerThreads() throws IOException 
	{
		ExecutorService threads = Executors.newFixedThreadPool(nConnections);
		
		while(true) 
		{
			final Socket accConnect = serverSocket.accept();
			
			Runnable serverThread = () -> 
			{ // każdy połączony użytkownik ma dwa wątki -> czytanie/pisanie				
				Runnable read = () -> { readData(accConnect); };
				Runnable write = () -> { writeData(accConnect); };

				Thread readThread = new Thread(read);// wystartowanie wątku odczytywania danych od konkretnego użytkownika
				Thread writeThread = new Thread(write); // wystartowanie wątku wysyłania danych do konkretnego użytkownika
				
				keepAlive(readThread, writeThread);
			};
			
			threads.submit(serverThread); // wystartowanie wątku użytkownika
		}
	}
	
	private void keepAlive(Thread thread1, Thread thread2) 
{
		while(thread1.isAlive() || thread2.isAlive()) 
		{
			try 
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}
	}
	
	private void readData(Socket socket) 
	{
		// Sprawdzanie wiadomości - czy nie jest to komenda do serwera
		// Sprawdzanie do kogo jest dana wiadomość - czy taki użytkownik istnieje
		// Przesłanie wiadomości - to jeszcze przemyślę w jaki sposób zrobić
		//BufferedReader buffRead = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}
	
	private void writeData(Socket socket) 
	{
		// Sprawdzanie jakiegoś bufora czy nie ma wiadomości do wysłania temu użytkownikowi
		// Jeśli są, to wysłać wiadomość
		//BufferedWriter buffWrite = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
	}
	
	public void closeServer() 
	{
		try 
		{
			serverSocket.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] args) 
	{
		
		Server serwer = null;
		try 
		{
			serwer = new Server(40123, 1);
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			serwer.closeServer();
		}
		
	}

}
