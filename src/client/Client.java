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
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import static shared.messages.MessageSender.sendLine;
import static shared.utils.UsernameValidation.usernameIsValid;

public class Client {
    private PrintWriter writer;
    private BufferedReader reader;
    private Scanner scanner;
    private ObjectMapper mapper;
    private FileTransferManager fileTransferManager;

    public Client() throws IOException, NoSuchAlgorithmException {
        Socket socket = new Socket("127.0.0.1", 1337);
        this.writer = new PrintWriter(socket.getOutputStream());
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.mapper = new ObjectMapper();
        this.fileTransferManager = new FileTransferManager();
    }

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

        awaitWelcomeMessage();
        logIn();

        ClientInputThread clientInputThread = new ClientInputThread(writer, mapper, fileTransferManager, this);
        ServerInputThread serverInputThread = new ServerInputThread(reader, writer, mapper, fileTransferManager, this);
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
                sendLine("LOGIN " + mapper.writeValueAsString(new UsernameMessage(userInput)), writer);
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
}