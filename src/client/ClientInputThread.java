package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.BroadcastRequestMessage;
import shared.messages.PrivateMessage;

import java.io.PrintWriter;
import java.util.Scanner;

public class ClientInputThread extends Thread {
    private final PrintWriter writer;
    private final ObjectMapper mapper;
    private final Scanner scanner;

    private final String BYE = "BYE";
    private final String MENU = """
            1) Broadcast message
            2) List online users
            3) Private message
            0) Terminate connection
            Select an option:\s""";

    ClientInputThread(PrintWriter writer, ObjectMapper objectMapper) {
        this.writer = writer;
        this.mapper = objectMapper;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        while (true) {
            System.out.print(MENU);
            int userInput = getIntBetweenBounds(0, 5);
            scanner.nextLine(); // Consume leftover line

            switch (userInput) {
                case 0 -> terminateConnection();
                case 1 -> broadcastRequest();
                case 2 -> listUsers();
                case 3 -> privateMessage();
            }
        }
    }

    private void terminateConnection() {
        writer.println(BYE);
    }

    private void privateMessage() {
        try {
            String user = promptForInput("User to message: ");
            String message = promptForInput("Enter your message: ");

            String jsonString = mapper.writeValueAsString(new PrivateMessage(user, message));
            writer.println("PRIVATE_MESSAGE_REQ " + jsonString);
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
        }
    }

    private void listUsers() {
        // C -> S: LIST_USERS_REQ
        writer.println("LIST_USERS_REQ");
    }

    private String promptForInput(String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine();

        while (input == null || input.isBlank()) {
            System.out.print(prompt);
            input = scanner.nextLine();
        }

        return input;
    }

    private void broadcastRequest() {
        try {
            String message = promptForInput("Enter your message: ");
            writer.println("BROADCAST_REQ " + mapper.writeValueAsString(new BroadcastRequestMessage(message)));
        } catch (Exception e) {
            MessageCodePrinter.printMessageFromCode(0);
        }
    }

    private void quit() {
        // Send logout request to server
        writer.println(BYE);

        System.out.println("Bye.");
        System.exit(0);
    }

    private int getIntBetweenBounds(int lowerBound, int upperBound) {
        int result = scanner.nextInt();
        while (result < lowerBound || result > upperBound) {
            System.out.printf("Please enter a value between %d and %d: ", lowerBound, upperBound);
            result = scanner.nextInt();
        }

        return result;
    }
}
