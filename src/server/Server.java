package server;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static shared.utils.UsernameValidation.usernameIsValid;

public class Server {
    private final ServerSocket serverSocket;
    private final HashMap<String, PrintWriter> users;

    public Server() throws IOException {
        this.serverSocket = new ServerSocket(1337);
        this.users = new HashMap<>();
    }

    public static void main(String[] args) throws IOException {
        new Server().run();
    }

    public void run() throws IOException {
        while (true) {
            // Wait for an incoming client-connection request (blocking)
            Socket socket = serverSocket.accept();
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            ObjectMapper mapper = new ObjectMapper();
            PingInfo pingInfo = new PingInfo();
            new ServerThread(socket, this, writer, reader, mapper, pingInfo).start();
        }
    }

    public PrintWriter removeUser(String username) {
        PrintWriter writer = users.remove(username);
        assert !users.containsKey(username) : "Error removing user from hashmap.";
        return writer;
    }

    public void addUser(String username, PrintWriter writer) {
        if (!usernameIsValid(username)) throw new IllegalArgumentException("Username is invalid.");
        if (writer == null) throw new IllegalArgumentException("Invalid PrintWriter instance provided.");

        users.put(username.toLowerCase(), writer);
    }

    public PrintWriter getWriter(String username) {
        return users.get(username.toLowerCase());
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

    public HashMap<String, PrintWriter> getUsers() {
        return users;
    }
}
