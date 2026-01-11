package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static shared.utils.Utils.usernameIsValid;

public class Server {
    private final static int PORT = 3000;
    private ServerSocket serverSocket;
    private final HashMap<String, PrintWriter> users = new HashMap<>();

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);

        while (true) {
            // Wait for an incoming client-connection request (blocking)
            new ServerThread(serverSocket.accept(), this).start();
        }
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        while (true) {
            server.start(PORT);
            server.serverSocket.close();
        }
    }

    public boolean removeUser(String username) {
        PrintWriter writer = users.remove(username);
        return writer != null;
    }

    public void addUser(String username, PrintWriter writer) {
        if (!usernameIsValid(username)) throw new IllegalArgumentException("Username is invalid.");
        if (writer == null) throw new IllegalArgumentException("Invalid PrintWriter instance provided.");

        users.put(username, writer);
    }

    public ArrayList<String> getUsernames() {
        ArrayList<String> result = new ArrayList<>();

        users.forEach((username, writer) -> {
            result.add(username);
        });

        return result;
    }

    public Collection<PrintWriter> getPrintWriters() {
        return users.values();
    }

    public HashMap<String, PrintWriter> getUsers() {
        return users;
    }
}
