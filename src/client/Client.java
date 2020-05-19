package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.Scanner;

public class Client {
	
	private InetAddress ADDRESS = null;
	private int PORT = 40123;
	private Socket socket = null;
	
	private BufferedReader buffRead = null;
	private BufferedWriter buffWrite = null;
	
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
		
		printGreeting();
		
		try 
		{
			socket = new Socket(ADDRESS, PORT);
		}
		catch (IOException e)
		{
			System.out.println("Nie udało się połączyć z serwerem.");
			System.exit(1);
		}
		
		initBuffers();

		Runnable read = () -> { readData(); };
		Runnable write = () -> { writeData(); };
		
		Thread readThread = new Thread(read);
		Thread writeThread = new Thread(write);
		
		readThread.start();
		writeThread.start();
		
		keepAlive(readThread, writeThread);
		closeConnection();
	}
	
	private void printGreeting()
	{
		String text = "Witaj! Następuje próba połączenia z " + ADDRESS + "\n"
					+ "W każdym momencie możesz wpisać 'POMOC', aby wyświetlić dostępne komendy.\n";
		
		System.out.println(text);
	}
	
	private void readData()
	{
		try
		{
			String text;
			
			while((text = buffRead.readLine()) != null)
				System.out.println(text);
			
		} 
		catch (IOException e)
		{
			System.out.println("Błąd połączenia. Rozłączono z serwerem.");
			System.exit(1);
		}
	}
	
	private void writeData()
	{
		try
		{
			Scanner scanner = new Scanner(System.in);
			
			String text = scanner.nextLine();
			while(!socket.isClosed())
			{
				buffWrite.write(text);
				buffWrite.newLine();
				buffWrite.flush();
				
				if("KONIEC".equals(text))
					break;
				
				text = scanner.nextLine();
			}
			
			scanner.close();
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
			while(thread1.isAlive() || thread2.isAlive())
				Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
			System.out.println("Wystąpił błąd w wątku głównym!");
		}
	}
	
	private void closeConnection()
	{
		try
		{
			socket.close();
		} 
		catch (IOException e)
		{
			System.out.println("Nastąpił błąd podczas zamykania połączenia!");
		}
	}
	
	private void initBuffers()
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
		if(args.length != 2)
		{
			System.out.println("Niepoprawna liczba argumentów!");
			System.out.println("Należy podać adres serwera jako pierwszy oraz port jako drugi argument.");
			System.exit(1);
		}
		
		
		String ADDRESS = args[0];
		try 
		{
			int PORT = Integer.parseInt(args[1]);	
			new Client(ADDRESS, PORT);
		}
		catch (NumberFormatException e)
		{
			System.out.println("Numer portu musi być całkowitą liczbą dodatnią!");
			System.exit(1);
		}
		
		
		
	}

}
