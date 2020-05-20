/*
 * Autorzy: Rafał Jarmakiewicz i Zuzanna Łaska
 * Data modyfikacji: 19.05.2020r.
 * 
 */

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
	
	private final int PORT; // PORT serwera
	private final int nConnections; // Maksymalna liczba jednoczesnych połączeń z serwerem
	
	private File usersFile = new File("./users.dat"); // Plik przechowujący dane użytkowników
	private File usersFileBackup = new File("./users_backup.dat"); // Plik zapasowy, przechowujący dane użytkowników
	private ServerSocket serverSocket; // Socket serwera
	private Map<String, User> users = new HashMap<String, User>(); // HashMapa (tablica asocjacyjna) zawierająca zarejestrowanych użytkowników
	
	public Server(int PORT, int nConnections) throws IOException 
	{
		this.PORT = PORT; // Przypisanie portu
		this.nConnections = nConnections; // Przypisanie max. liczby połączeń

		loadUsers(); // Wczytywanie użytkowników z pliku		
		
		serverSocket = new ServerSocket(this.PORT); // Utworzenie socketu dla serwera
		initServerThreads(); // Inicjalizacja wątków serwera
	}
	

	private void initServerThreads() throws IOException
	{
		ExecutorService threads = Executors.newFixedThreadPool(nConnections); // Utworzenie maksymalnej puli wątków (max. liczba jednoczesnych połączeń)
		
		System.out.println();
		System.out.println("!! Kill this process or close the termial to shut down the server (e.g. ctrl+c on linux) !!");
		System.out.println();
		
		while(true)
		{
			System.out.println("Listening for connection...");
			final Socket accConnect = serverSocket.accept(); // Nasłuchiwanie na połączenie - akceptowanie przychodzących połączeń
			
			Runnable serverThread = () ->
			{
				User user = logIn(accConnect); // Logowanie/Rejestracja użytkownika
				
				if(user != null)
				{
					Runnable read = () -> { readData(accConnect, user); };
					Runnable write = () -> { writeData(accConnect, user); };

					Thread readThread = new Thread(read); // Wątek w którym odbywa się odbiór wiadomości od klienta
					Thread writeThread = new Thread(write); // Wątek w którym odbywa się wysyłanie wiadomości do klienta
					
					readThread.start(); // Uruchomienie wątków
					writeThread.start();
					
					keepAlive(readThread, writeThread); // Utrzymanie wątku głównego przy życiu, póki działają wątki odczytywania/zapisu
					logout(accConnect, user); // Rozłączenie użytkownika
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
			if(!usersFile.exists()) // Utworzenie pliku "users.dat" jeśli nie istniał wcześniej
				usersFile.createNewFile();
			else
			{
				Scanner scanner = new Scanner(usersFile);
				
				while(scanner.hasNextLine()) // Wczytywanie użytkowników z istniejącego pliku "users.dat"
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
			System.out.println("Error while loading users!");
		}
	}
	
	private void appendUserToFile(User user) // Dodanie nowo zarejestrowanego użytkownika do pliku "users.dat"
	{
		try
		{
			FileWriter writer = new FileWriter(usersFile, true);
			writer.write(user.getName() + " " + user.getPassword() + '\n');
			writer.close();
		}
		catch (IOException e)
		{
			System.out.println("Error while saving users!");
		}
	}

	private User logIn(Socket socket) // Logowanie/Rejestrowanie użytkownika
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
				buffWrite.write("Aby uzyskać pomoc, wpisz 'POMOC'.\n");
				buffWrite.write("W każdym momencie możesz opuścić serwer, pisząc komendę 'KONIEC'.\n");
				buffWrite.flush();
				
				String text = buffRead.readLine();
				buffWrite.newLine();
				
				if(text.matches("^REJESTRUJ\\s.*")) // Opcja rejestracji
				{
					if(text.split("\\s+").length < 2) // Sprawdzenie formatu zmiennej text (wymagane: komenda + login + hasło)
					{
						buffWrite.write("Nie podano nazwy użytkownika oraz hasła. Spróbuj ponownie.\n");
						buffWrite.flush();
						continue;
					}
					else if(text.split("\\s+").length < 3)
					{
						buffWrite.write("Nie podano hasła. Spróbuj ponownie.\n");
						buffWrite.flush();
						continue;
					}
					
					String username = text.split("\\s+")[1];
					String password = text.split("\\s+")[2];
					if(!username.matches("\\w+")) // Sprawdzenie czy nazwa użytkownika zawiera znaki nie alfanumeryczne
					{
						buffWrite.write("Niepoprawne znaki w nazwie użytkownika! Nazwa użytkownika może się składać jedynie z liter (łacińskich) i liczb!\n");
						buffWrite.write("Spróbuj ponownie.\n");
						buffWrite.flush();
					}
					else
					{
						if(users.containsKey(username)) // Sprawdzenie czy nazwa użytkownika jest zajęta
						{
							buffWrite.write("Nazwa użytkownika jest zajęta. Spróbuj ponownie.\n");
							buffWrite.flush();
						}
						else
						{
							buffWrite.write("Zarejestrowano pomyślnie. Zalogowano do serwera.\n");
							buffWrite.write("Aby uzyskać pomoc, wpisz 'POMOC'.\n");
							buffWrite.flush();
							
							user = new User(username, password);
							users.put(username, user);
							appendUserToFile(user); // Dodanie nowego użytkownika do pliku "users.dat"
							user.setBuffRead(buffRead);
							user.setBuffWrite(buffWrite);
							
							System.out.println(user.getName() + " registered in...");
							user.setOnlineStatus(true);
							backupUsers(); // Zapisanie użytkowników
							success = true;
						}
					}
				}
				else if(text.matches("^LOGUJ\\s.*")) // Opcja logowania 
				{
					if(text.split("\\s+").length < 2) // Sprawdzenie formatu zmiennej text (wymagane: komenda + login + hasło)
					{
						buffWrite.write("Nie podano nazwy użytkownika oraz hasła. Spróbuj ponownie.\n");
						buffWrite.flush();
						continue;
					}
					else if(text.split("\\s+").length < 3)
					{
						buffWrite.write("Nie podano hasła. Spróbuj ponownie.\n");
						buffWrite.flush();
						continue;
					}
					
					String username = text.split("\\s+")[1];
					String password = text.split("\\s+")[2];
					if(users.containsKey(username)) // Sprawdzenie czy istnieje użytkownik o podanym loginie
					{
						if(users.get(username).getPassword().equals(password)) // Sprawdzenie czy hasło jest poprawne
						{
							if(!users.get(username).isOnline()) // Sprawdzenie czy użytkownik jest offline (nie można logować się na aktywnego użytkownika)
							{
								user = users.get(username);
								buffWrite.write("Zalogowano pomyślnie.\n");
								buffWrite.write("Aby uzyskać pomoc, wpisz 'POMOC'.\n");
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
				else if(text.matches("KONIEC")) // Zakończenie połączenia
				{
					buffWrite.write("Nastąpiło wylogowanie.\n");
					buffWrite.flush();
					buffWrite.close();
					buffRead.close();
					
					success = true;
				}
				else if(text.matches("POMOC")) // Wyświetlenie pomocy
				{
					String helpMsg = "Komendy dostępne przed zalogowaniem:\n"
							+ "* 'REJESTRUJ użytkownik hasło' - w celu rejestracji na serwer\n"
							+ "* 'LOGUJ użytkownik hasło' - w celu zalogowania na serwer\n"
							+ "* 'KONIEC' - w celu rozłączenia z serwerem\n\n"
							
							+ "Komendy dostępne po zalogowaniu:\n"
							+ "* 'Użytkownik: Treść wiadomości...' - w celu wysłania do danego użytkownika wiadomości\n"
							+ "* 'DODAJ użytkownik' - w celu dodania użytkownika na listę znajomych\n"
							+ "* 'KASUJ użytkownik' - w celu usunięcia użytkownika z listy znajomych\n"
							+ "* 'ZNAJOMI' - w celu wyświetlenia zalogowanych znajomych\n"
							+ "* 'WYREJESTRUJ' - w celu wyrejestrowania z serwera\n"
							+ "* 'KONIEC' - w celu rozłączenia z serwerem\n\n";
					
					buffWrite.write(helpMsg);
					buffWrite.flush();
				}
				else // Przypadek gdy nie rozpoznano komendy
				{
					buffWrite.write("Nie rozpoznano komendy. Spróbuj ponownie.\n");
					buffWrite.flush();
				}
			}
			
			return user;
		}
		catch (IOException e)
		{
			System.out.println("Error while receiving/sending message!");
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
				System.out.println("Thread was interrupted. Error occurred!");
			}
		}
	}
	
	private void writeData(Socket socket, User client) // Wysyłanie wiadomości do użytkownika
	{
		try
		{
			BufferedWriter buffWrite = client.getBuffWrite();
			Message message = client.getMessage();
			while(message.finalMsg() == false) // Jeśli nie jest to ostatnia wiadomość, to wysyłaj wiadomości w pętli
			{
				buffWrite.write(message.getContent());
				buffWrite.flush();
				
				message = client.getMessage();
			}
		} 
		catch (IOException | InterruptedException e)
		{
			System.out.println("Connection error. " + client.getName() + " has disconnected...");
		}
	}
	
	private void readData(Socket socket, User client) // Odbieranie wiadomości od użytkownika
	{
		try
		{
			BufferedReader buffRead = client.getBuffRead();
			String line = buffRead.readLine();
			boolean logout = false;
			
			while(!"KONIEC".equals(line) && !socket.isClosed()) // Jeśli wiadomość różna od 'KONIEC', to odczytuj dalej
			{
				logout = parseText(socket, client, line); // Przetwarzanie odebranej wiadomości
				
				if(logout) // Jeśli odebrano komendę 'WYREJESTRUJ' to rozłącz od razu
					break;
				
				line = buffRead.readLine();
			}
			
			client.sendMessage("#END", true); // Wysłanie wiadomości finalnej (kończy to wysyłanie wiadomości do użytkownika w funkcji writeData(...))
		} 
		catch (IOException | InterruptedException e)
		{
			System.out.println("Connection error. " + client.getName() + " has disconnected...");
		}
		
	}
	
	private void logout(Socket socket, User user) // Zakończenie połączenia po wylogowaniu użytkownika
	{
		try
		{
			user.getBuffRead().close();
			user.getBuffWrite().close();
			if(!socket.isClosed())
				socket.close();
			user.setOnlineStatus(false);
			System.out.println(user.getName() + " has logged out...");
		} 
		catch (IOException e)
		{
			System.out.println("Connection error. " + user.getName() + " has disconnected...");
		}
	}
	
	private void showFriendList(User user) throws InterruptedException // Wyświetlenie listy znajomych użytkownika wraz ze statusem aktywnosci znajomych
	{
		String msg = "Lista znajomych:\n";
		for(String username : user.getFriends())
		{
			msg += "* " + username + " - status: ";
			if(users.get(username).isOnline())
				msg += "online\n";
			else
				msg += "offline\n";
		}
		
		user.sendMessage(msg);
	}
	
	private void addToFriends(User user, String text) throws InterruptedException // Dodanie użytkownika do listy znajomych
	{
		String username = text.split("DODAJ\\s+")[1];
		if(users.containsKey(username) && !user.getFriends().contains(username)) // Sprawdzenie czy użytkownik istnieje oraz czy nie ma go jeszcze na liście znajomych
		{
			user.addFriend(username);
			user.sendMessage("Pomyślnie dodano użytkownika " + username + " do znajomych\n");
			System.out.println("User " + user.getName() + " added " + username + " to friends...");
			backupUsers(); // Zapisanie użytkowników
		}
		else if(!users.containsKey(username))
		{
			user.sendMessage("Podany użytkownik nie istnieje\n");
		}
		else if(user.getFriends().contains(username))
		{
			user.sendMessage("Masz już użytkownika " + username + " w znajomych\n");
		}
	}
	
	private void removeFromServer(User user) throws InterruptedException // Usunięcie konta z serwera (wyrejestrowanie)
	{
		String username = user.getName();
		
		user.sendMessage("Wyrejestrowano z serwera.\n");
		user.sendMessage("#END", true);

		users.remove(username);
		for(Map.Entry<String, User> entry : users.entrySet()) // Usunięcie użytkownika z serwera i list znajomych wszystkich innych użytkowników
			entry.getValue().deleteFriend(username);
		backupUsers(); // Zapisanie użytkowników
		System.out.println(username + " has been removed from server...");
	}
	
	private void sendMessage(User user, String text) throws InterruptedException // Wysyłanie wiadomości do innego użytkownika
	{
		String username = text.split(":")[0];
		String msg = text.split("^\\w+:\\s+")[1];

		/*
		   Wiadomość zostanie wysłana jeśli:
		   1) Adresat istnieje (jest zarejestrowany)
		   2) Użytkownicy wzajemnie posiadają się na listach znajomych
		   3) Adresat jest online
		*/
		if(user.getFriends().contains(username) && users.get(username).getFriends().contains(user.getName()) && users.get(username).isOnline())
		{
			System.out.println(user.getName() + " is sending message to " + username);
			users.get(username).sendMessage("Wiadomość od " + user.getName() + ": " + msg + '\n');
		}
		else if(!user.getFriends().contains(username))
			user.sendMessage("Nie posiadasz " + username + " na lisćie znajomych!\n");
		else if(!users.get(username).getFriends().contains(user.getName()))
			user.sendMessage("Użytkownik " + username + " nie posiada Cię na liście znajomych!\n");
		else if(!users.get(username).isOnline())
			user.sendMessage("Użytkownik " + username + " jest offline.\n");
	}
	
	private void removeFromFriends(User user, String text) throws InterruptedException // Usunięcie użytkownika z listy znajomych
	{
		String username = text.split("KASUJ\\s+")[1];
		
		if(user.getFriends().contains(username)) // Sprawdzenie czy użytkownik istnieje na liście znajomych
		{
			user.deleteFriend(username);
			user.sendMessage("Użytkownik " + username + " został usunięty z listy znajomych.\n");
			System.out.println("User " + user.getName() + " deleted " + username + " from friends...");
			backupUsers(); // Zapisanie użytkowników
		}
		else
		{
			user.sendMessage("Nie posiadasz " + username + " na liście znajomych.\n");
		}
	}
	
	private void sendHelp(User user) throws InterruptedException // Wysłanie pomocy użytkownikowi
	{
		String text = "Komendy dostępne przed zalogowaniem:\n"
				+ "* 'REJESTRUJ użytkownik hasło' - w celu rejestracji na serwer\n"
				+ "* 'LOGUJ użytkownik hasło' - w celu zalogowania na serwer\n"
				+ "* 'KONIEC' - w celu rozłączenia z serwerem\n\n"
				
				+ "Komendy dostępne po zalogowaniu:\n"
				+ "* 'Użytkownik: Treść wiadomości...' - w celu wysłania do danego użytkownika wiadomości\n"
				+ "* 'DODAJ użytkownik' - w celu dodania użytkownika na listę znajomych\n"
				+ "* 'KASUJ użytkownik' - w celu usunięcia użytkownika z listy znajomych\n"
				+ "* 'ZNAJOMI' - w celu wyświetlenia zalogowanych znajomych\n"
				+ "* 'WYREJESTRUJ' - w celu wyrejestrowania z serwera\n"
				+ "* 'KONIEC' - w celu rozłączenia z serwerem\n\n";
		
		user.sendMessage(text);
	}
	
	private boolean parseText(Socket socket, User user, String text) // Przetwarzanie wiadomości - rozpoznanie komend
	{
		try
		{
			if(text == null) // Sprawdzenie czy text zawiera dane
			{
				return true;
			}
			else if(text.equals("ZNAJOMI"))
			{
				showFriendList(user);
			}
			else if(text.matches("DODAJ\\s\\w+"))
			{
				addToFriends(user, text);
			}
			else if(text.equals("WYREJESTRUJ"))
			{
				removeFromServer(user);
				return true;
			} 
			else if(text.matches("^\\w+:\\s+.*"))
			{
				sendMessage(user, text);
			}
			else if(text.matches("^KASUJ\\s\\w+"))
			{	
				removeFromFriends(user, text);
			}
			else if(text.matches("POMOC"))
			{
				sendHelp(user);
			}
			else
			{
				user.sendMessage("Nie rozpoznano komendy lub nie podano wymaganej nazwy użytkownika. Wpisz 'POMOC' aby wyświetlić dostępne komendy.\n");
			}
		}
		catch (InterruptedException e)
		{
			System.out.println("Connection error while parsing " + user.getName() + " message...");
		}
		
		return false;
	}
	
	private void backupUsers() // Zapisanie użytkowników do pliku "users_backup.dat", a następie nadpisanie "users.dat"
	{
		try
		{
			FileWriter writer = new FileWriter(usersFileBackup);
			
			for(Map.Entry<String, User> entry : users.entrySet())
			{
				writer.write(entry.getValue().getName() + " " + entry.getValue().getPassword() + ' ');
				
				for(String friend : entry.getValue().getFriends())
					writer.write(friend + ' ');
				
				writer.write('\n');
				writer.flush();
			}
			
			writer.close();
			
			usersFileBackup.renameTo(usersFile); // Nadpisanie pliku "users.dat"
		} 
		catch (IOException e)
		{
			System.out.println("Error while saving users to file...");
		}
		
	}	
	

	public static void main(String[] args)
	{
		if(args.length != 2) // Sprawdzenie poprawności liczby podanych argumentów
		{
			System.out.println("Nieporawna liczba argumentów!");
			System.out.println("Należy podać port jako pierwszy i liczbę jednoczesnych połączeń jako drugi argument.");
			System.exit(1);
		}
		try
		{
			int PORT = Integer.parseInt(args[0]); // Inicjalizacja zmiennej PORT ( typ int)
			int nConnections = Integer.parseInt(args[1]); // Inicjalizacja zmiennej nConnections (typ int)
			
			new Server(PORT, nConnections); // Uruchomienie serwera
		} 
		catch (IOException e)
		{
			System.out.println("Nastąpił błąd - prawdopodobnie PORT jest już w użyciu. Zakończono program.");
			System.exit(1);
		}
		catch (NumberFormatException e)
		{
			System.out.println("Podano niepoprawny numer portu lub liczbę połączeń!");
			System.out.println("PORT oraz liczba połączeń muszą być całkowitymi, dodatnimi liczbami!");
			System.exit(1);
		}
		
	}

}
