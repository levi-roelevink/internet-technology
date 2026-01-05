package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import messages.LoginMessage;
import messages.LoginResponseMessage;
import messages.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

public class Client {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner scanner;
    private ObjectMapper objectMapper;
    private static final int PORT = 1337;
    private static final String IP = "127.0.0.1";
    private String username;

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.run();
    }

    public void startConnection(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public String sendMessage(String msg) throws IOException {
        out.println(msg);
        return in.readLine();
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }

    private Message parseMessage(String jsonString) throws JsonProcessingException {
        return objectMapper.readValue(jsonString, Message.class);
    }

    public void run() throws IOException {
        scanner = new Scanner(System.in);
        objectMapper = new ObjectMapper();
        startConnection(IP, PORT);

        String line;

        while ((line = in.readLine()) != null) {
            try {
                String[] lineParts = line.split(" ", 2);
                switch (lineParts[0]) {
                    case "WELCOME" -> handleWelcomeMessage(lineParts[1]);
                }
            } catch (Exception e) {
                System.err.println(e);
            }
        }

        stopConnection();
    }

    private void handleWelcomeMessage(String jsonString) throws IOException {
        Message message = objectMapper.readValue(jsonString, Message.class);
        System.out.println(message);

        assert username == null : "Unexpected welcome message from the server.";

        login();
    }

    public void login() throws IOException {
        System.out.print("Enter your name: ");
        String userInput = scanner.nextLine();
        // TODO: what if the name is invalid?

        LoginMessage loginMsg = new LoginMessage(userInput);
        // C -> S: LOGIN {"username":"<username>"}
        out.println("LOGIN " + objectMapper.writeValueAsString(loginMsg));

        String inputLine = in.readLine();
        String[] splits = inputLine.split(" ", 2);
        assert (Objects.equals(splits[0], "LOGIN_RESP")) : "Error logging in";

        LoginResponseMessage loginResp = objectMapper.readValue(splits[1], LoginResponseMessage.class);
        // S -> C: LOGIN_RESP {"status":"OK"}
        if (!Objects.equals(loginResp.status(), "OK")) {
            throw new RuntimeException("Error logging in");
        }

        // Set username after successful login
        username = userInput;
        assert username != null : "Username is still null after logging in";
        System.out.printf("Successfully logged in. Welcome %s!", username);
    }
}