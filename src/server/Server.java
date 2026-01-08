package server;

import java.io.IOException;
import java.net.ServerSocket;

public class Server {
    private final static int PORT = 3000;
    private ServerSocket serverSocket;

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);

        while (true) {
            // Wait for an incoming client-connection request (blocking)
            new ServerThread(serverSocket.accept()).start();
        }
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        while (true) {
            server.start(PORT);
            server.serverSocket.close();
        }
    }
}
