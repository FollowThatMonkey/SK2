package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server 
{
	
	public final int PORT; // PORT na którym działa serwer
	public final int nConnections; // Liczba hostów którzy będą akceptowani na raz
	
	private File usersFile = new File("./users.dat");
	private File usersFileBackup = new File("./users_backup.dat");
	private ServerSocket serverSocket; // Socket serwera
	private Map<String, User> users = new HashMap<String, User>();
	
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
		try
		{
			if(!usersFile.exists()) // jeśli nie ma pliku users, to go stwórz
				usersFile.createNewFile();
			else
			{
				Scanner scanner = new Scanner(usersFile);
				
				while(scanner.hasNextLine())
				{
					String[] name_password = scanner.nextLine().split(" ");
					// name_password[0] = userName, name_password[1] = password;
					users.put(name_password[0], new User(name_password[0], name_password[1]));
				}
				
				scanner.close();
			}
			System.out.println("Successfully loaded users");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void appendUserToFile(User user)
	{
		try
		{
			FileWriter writer = new FileWriter(usersFile, true);
			writer.write(user.getName() + " " + user.getPassword() + '\n');
			writer.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void initServerThreads() throws IOException 
	{
		ExecutorService threads = Executors.newFixedThreadPool(nConnections);
		
		while(true) 
		{
			System.out.println("Waiting for connection");
			final Socket accConnect = serverSocket.accept();
			
			Runnable serverThread = () -> 
			{ // każdy połączony użytkownik ma dwa wątki -> czytanie/pisanie
				User user = logIn(accConnect);
						System.out.println("Zalogowano");
				Runnable read = () -> { readData(accConnect, user); };
				Runnable write = () -> { writeData(accConnect, user); };

				Thread readThread = new Thread(read);// wystartowanie wątku odczytywania danych od konkretnego użytkownika
				Thread writeThread = new Thread(write); // wystartowanie wątku wysyłania danych do konkretnego użytkownika
				
				keepAlive(readThread, writeThread);
			};
			
			threads.submit(serverThread); // wystartowanie wątku użytkownika
		}
	}
	
	private User logIn(Socket socket) // Logowanie użytkownika
	{
		try
		{
			System.out.println("Somebody connected to server. Logging in...");
			BufferedReader buffRead = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			BufferedWriter buffWrite = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			boolean success = false;
			User user = null;
			
			
			buffWrite.write("Witamy na serwerze Gawariet-Gawariet!\n");
			
			while(!success)
			{
				buffWrite.write("W celu rejestracji wpisz 'REJESTRUJ użytkownik hasło'.\n");
				buffWrite.write("W celu zalogowania się do serwera wpisz 'LOGUJ użytkownik hasło'.\n");
				buffWrite.flush();
				
				String text = buffRead.readLine();
				buffWrite.write('\n');
				
				if(text.matches("^REJESTRUJ\\s.*")) // opcja rejestracji
				{
					String username = text.split("\\s+")[1];
					String password = text.split("\\s+")[2];
					if(username.matches("\\W")) // jeśli nazwa użytkownika posiada znaki nieliterowe i nienumeryczne
					{
						buffWrite.write("Niepoprawne znaki w nazwie użytkownika! Nazwa użytkownika może się składać jedynie z liter i liczb!\n");
						buffWrite.write("Spróbuj ponownie.\n");
						buffWrite.flush();
					}
					else
					{
						if(users.containsKey(username))
						{
							buffWrite.write("Nazwa użytkownika jest zajęta. Spróbuj ponownie.\n");
							buffWrite.flush();
						}
						else
						{
							buffWrite.write("Zarejestrowano pomyślnie. Zalogowano do serwera.\n");
							buffWrite.flush();
							
							user = new User(username, password);
							users.put(username, user);
							appendUserToFile(user);
							
							user.setOnlineStatus(true);
							success = true;
						}
					}
				}
				else if(text.matches("^LOGUJ\\s.*"))
				{
					String username = text.split("\\s+")[1];
					String password = text.split("\\s+")[2];
					if(users.containsKey(username))
					{
						if(users.get(username).getPassword().equals(password))
						{
							buffWrite.write("Zalogowano pomyślnie.\n");
							buffWrite.flush();
							
							users.get(username).setOnlineStatus(true);
							success = true;
						}
						else
						{
							buffWrite.write("Podano niepoprawne hasło. Spróbuj ponownie.\n");
							buffWrite.flush();
						}
					}
					else
					{
						buffWrite.write("Podano niepoprawną nazwę użytkownika. Spróbuj ponownie.\n");
						buffWrite.flush();
					}
				}
				else
				{
					buffWrite.write("Nie rozpoznano komendy. Spróbuj ponownie.\n");
					buffWrite.flush();
				}
				System.out.println(text);
			}
			
			buffRead.close();
			buffWrite.close();
			return user;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	private void keepAlive(Thread thread1, Thread thread2) // Utrzymanie wątku głównego przy życiu
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
	
	private void readData(Socket socket, User client) // Czytanie wiadomości odebranych od użytkownika
	{
		// Sprawdzanie wiadomości - czy nie jest to komenda do serwera
		// Sprawdzanie do kogo jest dana wiadomość - czy taki użytkownik istnieje
		// Przesłanie wiadomości - to jeszcze przemyślę w jaki sposób zrobić
		try
		{
			BufferedReader buffRead = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line = buffRead.readLine();
			while(!"KONIEC".equals(line))
			{
				parseText(line);
				line = buffRead.readLine();
			}
			logout(client);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}
	
	private void logout(User user) // co wykonać po wylogowaniu
	{
		user.setOnlineStatus(false);
	}
	
	private void parseText(String text) // Przetwarzanie wiadomości - rozpoznanie czy komenda, czy zwykła wiadomość
	{
		
	}
	
	private void writeData(Socket socket, User client) // Pisanie do użytkownika
	{
		// Sprawdzanie jakiegoś bufora czy nie ma wiadomości do wysłania temu użytkownikowi
		// Jeśli są, to wysłać wiadomość
		//BufferedWriter buffWrite = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
	}
	
	public void saveUsers() // Zapisanie użytkowników do pliku backup
	{
		try
		{
			FileWriter writer = new FileWriter(usersFileBackup);
			
			for(Map.Entry<String, User> entry : users.entrySet())
				writer.write(entry.getValue().getName() + " " + entry.getValue().getPassword() + '\n');
			
			writer.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}
	
	public void closeServer() // Zamknięcie socketa serwera
	{
		try 
		{
			serverSocket.close();
			saveUsers();
			System.out.println("Server closed!");
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	

	public static void main(String[] args) 
	{
		Server server = null;
		try
		{
			server = new Server(40123, 1);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}

}
