package server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Testy
{

	public static void main(String[] args)
	{
		User user = new User("imie", "nazwisko");
		User nullUser = null;
		
		if(user == null)
			System.out.println("User to null");
		else
			System.out.println("User to nie jest null");
		
		if(nullUser == null)
			System.out.println("NullUser to null");
		else
			System.out.println("NullUser to nie jest null");
	}

}
