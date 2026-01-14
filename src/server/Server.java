package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static shared.utils.UsernameValidation.usernameIsValid;

public class Server {
    private ServerSocket serverSocket;
    private FileTransferSetupThread fileTransferSetupThread;
    private final HashMap<String, PrintWriter> users = new HashMap<>();

    public void start() throws IOException {
        serverSocket = new ServerSocket(1337);
        fileTransferSetupThread = new FileTransferSetupThread(1338);

        while (true) {
            // Wait for an incoming client-connection request (blocking)
            new ServerThread(serverSocket.accept(), this).start();
        }
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        while (true) {
            server.start();
            server.serverSocket.close();
        }
    }

    public void removeUser(String username) {
        PrintWriter writer = users.remove(username);
        assert !users.containsKey(username) : "Error removing user from hashmap.";
    }

    public void addUser(String username, PrintWriter writer) {
        if (!usernameIsValid(username)) throw new IllegalArgumentException("Username is invalid.");
        if (writer == null) throw new IllegalArgumentException("Invalid PrintWriter instance provided.");

        users.put(username, writer);
    }

    public PrintWriter getUser(String username) {
        return users.get(username);
    }

    public String[] getUsernames() {
        String[] result = new String[users.size()];

        int count = 0;
        for (Map.Entry<String, PrintWriter> user : users.entrySet()) {
            result[count] = user.getKey();
            count++;
        }

        return result;
    }

    public Collection<PrintWriter> getPrintWriters() {
        return users.values();
    }

    public HashMap<String, PrintWriter> getUsers() {
        return users;
    }
}
