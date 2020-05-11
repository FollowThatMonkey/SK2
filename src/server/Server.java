package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
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
	
	private void initServerThreads() throws IOException 
	{
		ExecutorService threads = Executors.newFixedThreadPool(nConnections);
		
		while(true) 
		{
			System.out.println("Listening for connection...");
			final Socket accConnect = serverSocket.accept();
			
			Runnable serverThread = () -> 
			{ // każdy połączony użytkownik ma dwa wątki -> czytanie/pisanie
				User user = logIn(accConnect);
				
				if(user != null)
				{
					Runnable read = () -> { readData(accConnect, user); };
					Runnable write = () -> { writeData(accConnect, user); };

					Thread readThread = new Thread(read); // Wątek w którym odbywa się odbiór wiadomości od klienta
					Thread writeThread = new Thread(write); // Wątek w którym odbywa się wysyłanie wiadomości do klienta
					
					readThread.start(); // Uruchomienie wątków
					writeThread.start();
					
					keepAlive(readThread, writeThread);	
					logout(accConnect, user);
				}
				else
					System.out.println(accConnect.getRemoteSocketAddress() + " has disconnected...");
			};
			
			threads.submit(serverThread); // wystartowanie wątku użytkownika
		}
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
					String[] splitted_words = scanner.nextLine().split(" ");
					// name_password[0] == userName, name_password[1] == password;
					User user = new User(splitted_words[0], splitted_words[1]);
					users.put(splitted_words[0], user);
					for(int i=2; i<splitted_words.length; i++)
						user.addFriend(splitted_words[i]);
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

	private User logIn(Socket socket) // Logowanie użytkownika
	{
		try
		{
			System.out.println(socket.getRemoteSocketAddress() + " connected to server. Logging in...");
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
				buffWrite.newLine();
				
				if(text.matches("^REJESTRUJ\\s.*")) // opcja rejestracji
				{
					String username = text.split("\\s+")[1];
					String password = text.split("\\s+")[2];
					if(!username.matches("\\w+")) // jeśli nazwa użytkownika posiada znaki nieliterowe i nienumeryczne
					{
						buffWrite.write("Niepoprawne znaki w nazwie użytkownika! Nazwa użytkownika może się składać jedynie z liter (łacińskich) i liczb!\n");
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
							user.setBuffRead(buffRead);
							user.setBuffWrite(buffWrite);
							
							System.out.println(user.getName() + " registered in...");
							user.setOnlineStatus(true);
							backupUsers();
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
							if(!users.get(username).isOnline())
							{
								user = users.get(username);
								buffWrite.write("Zalogowano pomyślnie.\n");
								buffWrite.flush();
								user.setBuffRead(buffRead);
								user.setBuffWrite(buffWrite);
								
								System.out.println(user.getName() + " logged in...");
								users.get(username).setOnlineStatus(true);
								success = true;	
							}
							else
							{
								buffWrite.write("Użytkownik " + username + " jest online. Nie można się zalogować.\n");
								buffWrite.flush();
							}
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
				else if(text.matches("KONIEC"))
				{
					buffWrite.write("Nastąpiło wylogowanie.\n");
					buffWrite.close();
					buffRead.close();
					
					success = true;
				}
				else
				{
					buffWrite.write("Nie rozpoznano komendy. Spróbuj ponownie.\n");
					buffWrite.flush();
				}
			}
			
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
	
	private void writeData(Socket socket, User client) // Pisanie do użytkownika
	{
		// Sprawdzanie jakiegoś bufora czy nie ma wiadomości do wysłania temu użytkownikowi
		// Jeśli są, to wysłać wiadomość
		try
		{
			BufferedWriter buffWrite = client.getBuffWrite();
			Message message = client.getMessage();
			while(message.finalMsg() == false)
			{
				buffWrite.write(message.getContent());
				buffWrite.flush();
				
				message = client.getMessage();
			}
		} 
		catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
		}
	}
	
	private void readData(Socket socket, User client) // Czytanie wiadomości odebranych od użytkownika
	{
		// Sprawdzanie wiadomości - czy nie jest to komenda do serwera
		// Sprawdzanie do kogo jest dana wiadomość - czy taki użytkownik istnieje
		// Przesłanie wiadomości - to jeszcze przemyślę w jaki sposób zrobić
		try
		{
			BufferedReader buffRead = client.getBuffRead();
			String line = buffRead.readLine();
			boolean logout = false;
			
			while(!"KONIEC".equals(line) && !socket.isClosed() && !logout)
			{
				logout = parseText(socket, client, line);
				line = buffRead.readLine();
			}
			
			client.sendMessage("END", true);
		} 
		catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
		}
		
	}
	
	private void logout(Socket socket, User user) // co wykonać po wylogowaniu
	{
		try
		{
			socket.close();
			user.setOnlineStatus(false);
			System.out.println(user.getName() + " has logged out...\n");
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private boolean parseText(Socket socket, User user, String text) // Przetwarzanie wiadomości - rozpoznanie czy komenda, czy zwykła wiadomość
	{
		if(text.equals("ZNAJOMI"))
		{
			// Wyślij listę znajomych do użytkownika
			String msg = "Lista znajomych online:\n";
			for(String username : user.getFreinds())
			{
				if(users.get(username).isOnline())
					msg += "* " + username + '\n';
			}
			try
			{
				user.sendMessage(msg);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		else if(text.matches("DODAJ\\s\\w+"))
		{
			// Dodaj użytkownika do znajomych
			String username = text.split("DODAJ\\s+")[1];
			if(users.containsKey(username) && !user.getFreinds().contains(username))
				user.addFriend(username);
			backupUsers();
		}
		else if(text.equals("WYREJESTRUJ"))
		{
			// Usuń użytkownika z serwera
			String username = user.getName();
			try
			{
				user.sendMessage("Wyrejestrowano z serwera\n");
				user.sendMessage("END", true);
			} catch (InterruptedException e)
			{
				e.printStackTrace();
			}
			
			users.remove(username);
			for(Map.Entry<String, User> entry : users.entrySet())
				entry.getValue().getFreinds().remove(username);
			backupUsers();
			System.out.println(username + " has been removed from server...");
			return true;
		}
		else if(text.matches("^\\w+:\\s+.*"))
		{
			// Spróbuj wysłać wiadomość
			String username = text.split(":")[0];
			String msg = text.split("^\\w+:\\s+")[1];
			// Sprawdź czy użytkownik ma w znajomych adresata
			// Sprawdź czy adresat ma nadawcę w znajomych
			// Sprawdź czy adresat online
			try
			{
				
				if(user.getFreinds().contains(username) && users.get(username).getFreinds().contains(user.getName()) && users.get(username).isOnline())
				{
					System.out.println(user.getName() + " is sending message to " + username);
					users.get(username).sendMessage("Wiadomość od " + user.getName() + ": " + msg + '\n');
				}
				else if(!user.getFreinds().contains(username))
					user.sendMessage("Nie posiadasz " + username + " na lisćie znajomych!\n");
				else if(!users.get(username).getFreinds().contains(user.getName()))
					user.sendMessage("Użytkownik " + username + " nie posiada Cię na liście znajomych!\n");
				else if(!users.get(username).isOnline())
					user.sendMessage("Użytkownik " + username + " jest offline.");
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		return false;
	}
	
	private void backupUsers() // Zapisanie użytkowników do pliku backup
	{
		try
		{
			FileWriter writer = new FileWriter(usersFileBackup);
			
			for(Map.Entry<String, User> entry : users.entrySet())
			{
				writer.write(entry.getValue().getName() + " " + entry.getValue().getPassword() + ' ');
				
				for(String friend : entry.getValue().getFreinds()) // zapisz znajomych
					writer.write(friend + ' ');
				
				writer.write('\n');
				writer.flush();
			}
			
			writer.close();
			
			usersFileBackup.renameTo(usersFile);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}	
	

	public static void main(String[] args) 
	{
		try
		{
			@SuppressWarnings("unused")
			Server server = new Server(40123, 2);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
	}

}
