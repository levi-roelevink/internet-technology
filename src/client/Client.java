package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.ResponseMessage;
import shared.messages.UsernameMessage;
import shared.messages.GenericMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import static shared.utils.Utils.usernameIsValid;

public class Client {
    private Socket clientSocket;
    private PrintWriter writer;
    private BufferedReader reader;
    private Scanner scanner;
    private ObjectMapper mapper;
    private static final int PORT = 3000;
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
        mapper = new ObjectMapper();
        startConnection(IP, PORT);

        awaitWelcomeMessage();
        logIn();

        ClientInputThread clientInputThread = new ClientInputThread(writer, mapper);
        ServerInputThread serverInputThread = new ServerInputThread(writer, reader, mapper);
        clientInputThread.start();
        serverInputThread.start();
    }

    private void awaitWelcomeMessage() throws IOException {
        String receivedLine;
        while ((receivedLine = reader.readLine()) != null) {
            String[] lineParts = receivedLine.split(" ", 2);
            if ("WELCOME".equals(lineParts[0])) {
                GenericMessage message = mapper.readValue(lineParts[1], GenericMessage.class);
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
                writer.println("LOGIN " + mapper.writeValueAsString(new UsernameMessage(userInput)));
                loggedIn = awaitLoginResponse();
            }
        }
    }

    private boolean awaitLoginResponse() throws IOException {
        String received;
        while ((received = reader.readLine()) != null) {
            String[] lineParts = received.split(" ", 2);
            if ("LOGIN_RESP".equals(lineParts[0])) {

                ResponseMessage loginResp = mapper.readValue(lineParts[1], ResponseMessage.class);

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
        writer = new PrintWriter(clientSocket.getOutputStream(), true);
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public void stopConnection() throws IOException {
        reader.close();
        writer.close();
        clientSocket.close();
    }
}