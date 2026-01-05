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
                    case "LOGIN_RESP" -> handleLoginResponse(lineParts[1]);
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

    private void login() {
        try {
            System.out.print("Enter your name: ");

            String userInput = scanner.nextLine();
            while (userInput == null || userInput.isBlank() || userInput.isEmpty()) {
                System.out.print("Invalid username. Enter your name: ");
                userInput = scanner.nextLine();
            }
            username = userInput;

            out.println("LOGIN " + objectMapper.writeValueAsString(new LoginMessage(userInput)));
        } catch (Exception e) {
            System.err.println("Error logging in");
        }
    }

    private void handleLoginResponse(String jsonString) {
        assert username != null : "Username is still null after logging in";

        try {
            LoginResponseMessage loginResp = objectMapper.readValue(jsonString, LoginResponseMessage.class);

            if (!loginResp.status().equals("OK")) {
                throw new Exception("Error logging in");
            }

            System.out.printf("Successfully logged in. Welcome %s!", username);
        } catch (Exception e) {
            System.err.println("Error logging in");
        }
    }
}