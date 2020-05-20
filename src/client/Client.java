/*
 * Autorzy: Rafał Jarmakiewicz i Zuzanna Łaska
 * Data modyfikacji: 19.05.2020r.
 * 
 */

package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.Scanner;

public class Client {
	
	private InetAddress ADDRESS = null; // Adres serwera
	private int PORT = 40123; // Port serwera
	private Socket socket = null; // Socket łączący się z serwerem
	
	private BufferedReader buffRead = null; // Bufor wejścia (dane przychodzące z serwera)
	private BufferedWriter buffWrite = null; // Bufor wyjścia (dane wysyłane do serwera)
	
	Client(String address, int port)
	{
		try
		{
			this.ADDRESS = InetAddress.getByName(address);
		} 
		catch (UnknownHostException e)
		{
			System.out.println("Niepoprawny adres serwera.");
			System.exit(1);
		}
		this.PORT = port;
		
		printGreeting(); // Wyświetl powitanie klienta
		
		try 
		{
			socket = new Socket(ADDRESS, PORT); // Próba połączenia z serwerem
		}
		catch (IOException e)
		{
			System.out.println("Nie udało się połączyć z serwerem.");
			System.exit(1);
		}
		
		initBuffers(); // Inicjalizacja buforów wejścia/wyjścia

		Runnable read = () -> { readData(); }; // Inicjalizacja wątków odczytywania/zapisu danych
		Runnable write = () -> { writeData(); };
		
		Thread readThread = new Thread(read);
		Thread writeThread = new Thread(write);
		
		readThread.start(); // Uruchomienie wątków
		writeThread.start();
		
		keepAlive(readThread, writeThread); // Utrzymanie wątku głównego przy życiu, póki działają wątki odczytywania/zapisu 
		closeConnection(); // Zamknięcie połączenia
	}
	
	private void printGreeting() // Wyświetlenie powitania
	{
		String text = "Witaj! Następuje próba połączenia z " + ADDRESS + "\n"
					+ "W każdym momencie możesz wpisać 'POMOC', aby wyświetlić dostępne komendy.\n";
		
		System.out.println(text);
	}
	
	private void readData() // Odbieranie danych od serwera
	{
		try
		{
			String text;
			
			while((text = buffRead.readLine()) != null) // Póki wczytywane dane różne od null, to je wyświetl
				System.out.println(text);
			
		} 
		catch (IOException e)
		{
			System.out.println("Błąd połączenia. Rozłączono z serwerem.");
			System.exit(1);
		}
	}
	
	private void writeData() // Wysyłanie danych do serwera
	{
		try
		{
			Scanner scanner = new Scanner(System.in); // Obiekt wczytujący dane z klawiatury
			
			String text = scanner.nextLine(); // Wczytaj linijkę z klawiatury
			while(!socket.isClosed())
			{
				buffWrite.write(text); // Zbuforuj dane text
				buffWrite.newLine(); // Dodaj znak nowej linii
				buffWrite.flush(); // Wyślij dane
				
				if("KONIEC".equals(text) || "WYREJESTRUJ".equals(text)) // Jeśli wysłano wiadomość KONIEC to zakończ
					break;
				
				text = scanner.nextLine(); // Wczytaj nową linię
			}
			
			scanner.close(); // Zamknij wczytywanie danych z klawiatury
		}
		catch (IOException e)
		{
			System.out.println("Błąd połączenia. Rozłączono z serwerem.");
			System.exit(1);
		}
	}
	
	private void keepAlive(Thread thread1, Thread thread2)
	{
		try
		{
			while(thread1.isAlive() || thread2.isAlive()) // Póki wątki odczytywania/zapisu żyją, to uśpij na 1s
				Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
			System.out.println("Wystąpił błąd w wątku głównym!");
		}
	}
	
	private void closeConnection() // Zamknij połączenie z serwerem
	{
		try
		{
			socket.close();
		} 
		catch (IOException e)
		{
			System.out.println("Nastąpił błąd podczas zamykania połączenia!");
			System.exit(1);
		}
	}
	
	private void initBuffers() // Inicjalizuj bufory odczytywania/zapisu
	{
		try
		{
			buffRead = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			buffWrite = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		}
		catch (IOException e)
		{
			System.out.println("Wystąpił błąd inicjalizacji buforów!");
		}
	}
	
	public static void main(String[] args) {
		if(args.length != 2) // Sprawdzenie poprawnosci liczby argumentów
		{
			System.out.println("Niepoprawna liczba argumentów!");
			System.out.println("Należy podać adres serwera jako pierwszy oraz port jako drugi argument.");
			System.exit(1);
		}
		
		
		String ADDRESS = args[0]; // Przypisanie adresu serwera
		try 
		{
			int PORT = Integer.parseInt(args[1]); // Przypisanie portu jako int
			new Client(ADDRESS, PORT);  // Uruchomienie klienta
		}
		catch (NumberFormatException e)
		{
			System.out.println("Numer portu musi być całkowitą liczbą dodatnią!");
			System.exit(1);
		}
		
		
		
	}

}
