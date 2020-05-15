package client;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Testy
{

	public static void main(String[] args)
	{
		try
		{
			InetAddress ADDRESS = InetAddress.getByName("127.0.0.1.1");
			System.out.println(ADDRESS);
		} catch (UnknownHostException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
