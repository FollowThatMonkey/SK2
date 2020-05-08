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
		String text = "Ała";
		
		if(text.contains("\\W"))
			System.out.println(text.matches("\\W"));
		else
			System.out.println("Nie prawda");
		
	}

}
