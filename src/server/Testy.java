package server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Testy
{

	public static void main(String[] args)
	{
		File f1, f2;
		f1 = new File("test1.txt");
		f2 = new File("test2.txt");
		
		try 
		{
			f1.createNewFile();
			f2.createNewFile();
			
			System.out.println(f2);
			f2.renameTo(f1);
			System.out.println(f2);
			
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

}
