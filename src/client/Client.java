package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.UsernameMessage;
import shared.messages.GenericMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader reader;
    private Scanner scanner;
    private ObjectMapper objectMapper;
    private static final int PORT = 1337;
    private static final String IP = "127.0.0.1";

    public static void main(String[] args) {
        try {
            new Client().run();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(0);
        }
    }

    public void run() throws IOException {
        scanner = new Scanner(System.in);
        objectMapper = new ObjectMapper();
        startConnection(IP, PORT);

        awaitWelcomeMessage();
        logIn();

        stopConnection();
    }

    private void awaitWelcomeMessage() throws IOException {
        String receivedLine;
        while ((receivedLine = reader.readLine()) != null) {
            String[] lineParts = receivedLine.split(" ", 2);
            if ("WELCOME".equals(lineParts[0])) {
                GenericMessage message = objectMapper.readValue(lineParts[1], GenericMessage.class);
                System.out.println(message.msg());
                return;
            }
        }
    }

    private void logIn() throws IOException {
        boolean loggedIn = false;
        while (!loggedIn) {
            System.out.print("Enter your name: ");
            String userInput = scanner.nextLine();

            if (!usernameIsValid(userInput)) {
                MessageCodePrinter.printMessageFromCode(5001);
            } else {
                out.println("LOGIN " + objectMapper.writeValueAsString(new UsernameMessage(userInput)));
                loggedIn = awaitLoginResponse();
            }
        }
    }

    private boolean usernameIsValid(String username) {
        if (username == null || username.isBlank()) return false;

        // Length must be between 3 and 14 characters
        int length = username.length();
        if (length < 3 || length > 14) return false;

        // Username may only consist of characters, numbers, and underscores
        return username.matches("^[a-zA-Z0-9_]+$");
    }

    private boolean awaitLoginResponse() throws IOException {
        String received;
        while ((received = reader.readLine()) != null) {
            String[] lineParts = received.split(" ", 2);
            if ("LOGIN_RESP".equals(lineParts[0])) {
                GenericMessage loginResp = objectMapper.readValue(lineParts[1], GenericMessage.class);

                if (!loginResp.status().equals("OK")) {
                    MessageCodePrinter.printMessageFromCode(loginResp.code());
                    return false;
                } else {
                    System.out.println("Logged in successfully.");
                    return true;
                }
            }
        }

        return false;
    }

    public void startConnection(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public void stopConnection() throws IOException {
        reader.close();
        out.close();
        clientSocket.close();
    }
}