package Server;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private final ServerSocket serverSocket;
    private final ObjectMapper mapper;
    private final Map<String, PrintWriter> users;

    public Server() throws IOException {
        this.serverSocket = new ServerSocket(1337);
        this.mapper = new ObjectMapper();
        this.users = new HashMap<>();
    }

    public static void main(String[] args) throws IOException {
        new Server().run();
    }

    public void run() throws IOException {
        FileTransferSetupThread fileTransferSetupThread = new FileTransferSetupThread();
        fileTransferSetupThread.start();
        while (true) {
            Socket socket = serverSocket.accept();
            InputStream inputStream = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            OutputStream outputStream = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outputStream);
            PingInfo pingInfo = new PingInfo();

            MessageThread messageThread = new MessageThread(socket, reader, writer, mapper, pingInfo, this);


            messageThread.start();
        }
    }

    public Map<String, PrintWriter> getUsers() {
        return users;
    }

    public void addUser(String username, PrintWriter writer) {
        users.put(username.toLowerCase(), writer);
    }

    public PrintWriter getWriter(String username) {
        return users.get(username.toLowerCase());
    }

    public void removeUser(String username) {
        users.remove(username.toLowerCase());
    }

    public boolean containsUser(String username) {
        return users.containsKey(username.toLowerCase());
    }
}
