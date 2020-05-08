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
		if("Rafalł".matches("\\w+"))
			System.out.println("Udało się");
		else
			System.out.println("Nie udało się");
	}

}
