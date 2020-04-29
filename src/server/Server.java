package server;

import java.io.IOException;
import java.net.ServerSocket;

public class Server {
	
	public final int PORT; // PORT na którym działa serwer
	public final int nConnections; // Liczba hostów którzy będą akceptowani na raz
	
	private ServerSocket serverSocket; // Socket serwera
	
	public Server(int PORT, int nConnections) throws IOException {
		this.PORT = PORT; // Przypisanie portu
		this.nConnections = nConnections; // Przypisanie max liczby połączeń
		
		InitSocket();
	}
	
	private void InitSocket() throws IOException {
		serverSocket = new ServerSocket(this.PORT);
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
