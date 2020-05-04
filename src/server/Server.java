package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
	
	public final int PORT; // PORT na którym działa serwer
	public final int nConnections; // Liczba hostów którzy będą akceptowani na raz
	
	private ServerSocket serverSocket; // Socket serwera
	
	public Server(int PORT, int nConnections) throws IOException {
		this.PORT = PORT; // Przypisanie portu
		this.nConnections = nConnections; // Przypisanie max liczby połączeń

		// należy dodać wczytywanie użytkowników z pliku (użytkowników i hasła)
		
		serverSocket = new ServerSocket(this.PORT);
		InitServerThreads();
	}
	
	private void InitServerThreads() throws IOException {
		ExecutorService threads = Executors.newFixedThreadPool(nConnections);
		
		while(true) {
			final Socket accConnect = serverSocket.accept();
			
			Runnable serverThread = () -> { // każdy połączony użytkownik ma dwa wątki -> czytanie/pisanie				
				Runnable read = () -> { ReadData(accConnect); };
				Runnable write = () -> { WriteData(accConnect); };

				Thread readThread = new Thread(read);// wystartowanie wątku odczytywania danych od konkretnego użytkownika
				Thread writeThread = new Thread(write); // wystartowanie wątku wysyłania danych do konkretnego użytkownika
			};
			
			threads.submit(serverThread); // wystartowanie wątku użytkownika
		}
	}
	
	private void ReadData(Socket socket) {
		// Sprawdzanie wiadomości - czy nie jest to komenda do serwera
		// Sprawdzanie do kogo jest dana wiadomość - czy taki użytkownik istnieje
		// Przesłanie wiadomości - to jeszcze przemyślę w jaki sposób zrobić
		//BufferedReader buffRead = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}
	
	private void WriteData(Socket socket) {
		// Sprawdzanie jakiegoś bufora czy nie ma wiadomości do wysłania temu użytkownikowi
		// Jeśli są, to wysłać wiadomość
		//BufferedWriter buffWrite = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
	}

	public static void main(String[] args) {
		try {
			Server server = new Server(40123, 2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
