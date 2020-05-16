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
			//e.printStackTrace();
			System.out.println("Niepoprawny adres serwera.");
			System.exit(1);
		}
		this.PORT = port;
		
		printGreeting();
		
		try {
			socket = new Socket(ADDRESS, PORT);
		}
		catch (IOException e)
		{
			//e.printStackTrace();
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
					+ "W każdym momencie możesz wpisać 'POMOC' aby wyświetlić dostępne komendy.\n";
		
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
			e.printStackTrace();
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
				if("POMOC".equals(text))
				{
					printHelp();
					text = scanner.nextLine();
					continue;
				}
				
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
			e.printStackTrace();
		}
	}
	
	private void printHelp()
	{
		String text = "Komendy dostępne przed zalogowaniem:\n"
					+ "* 'LOGUJ użytkownik hasło' - w celu zalogowania na serwer\n"
					+ "* 'REJESTRUJ użytkownik hasło' - w celu rejestracji na serwer\n"
					+ "* 'KONIEC' - w celu rozłączenia z serwerem\n\n"
					+ "Komendy dostępne po zalogowaniu:\n"
					+ "* 'ZNAJOMI' - w celu wyświetlenia zalogowanych znajomych\n"
					+ "* 'DODAJ użytkownik hasło' - w celu dodania użytkownika na listę znajomych\n"
					+ "* 'WYREJESTRUJ' - w celu wyrejestrowania z serwera\n"
					+ "* 'Użytkownik: Treść wiadomości' - w celu wysłania do danego użytkownika wiadomości\n"
					+ "* 'KONIEC' - w celu rozłączenia z serwerem\n\n";
		
		System.out.println(text);
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
			e.printStackTrace();
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
			e.printStackTrace();
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
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		if(args.length != 2)
		{
			System.out.println("Niepoprawna liczba argumentów!");
			System.out.println("Należy podać adres serwera jako pierwszy oraz port jako drugi argument");
			System.exit(1);
		}
		
		
		String ADDRESS = args[0];
		int PORT = Integer.parseInt(args[1]);
		
		new Client(ADDRESS, PORT);
		
	}

}
