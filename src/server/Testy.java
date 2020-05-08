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
		FileWriter file;
		try
		{
			file = new FileWriter("./testy.txt");

			file.write("Pierwsza linijka\n");
			file.write("Druga linijka\n");
			file.write("Trzecia linijka\n");
			
			file.close();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		
		
	}

}
