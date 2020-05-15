package client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.Scanner;

public class Client {
	
	InetAddress ADDRESS;
	int PORT;
	
	
	Client(String address, int port)
	{
		try
		{
			this.ADDRESS = InetAddress.getByName(address);
		} 
		catch (UnknownHostException e)
		{
			e.printStackTrace();
			System.out.println("Niepoprawny adres serwera.");
			System.exit(1);
		}
		this.PORT = port;
	}
	
	public static void main(String[] args) {
		
		String ADDRESS = args[0];
		int PORT = Integer.parseInt(args[1]);
		
		new Client(ADDRESS, PORT);
		
	}

}
