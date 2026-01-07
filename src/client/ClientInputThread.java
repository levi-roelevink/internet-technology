package client;

import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.BroadcastRequestMessage;

import java.io.PrintWriter;
import java.util.Scanner;

public class ClientInputThread extends Thread {
    private final PrintWriter writer;
    private final ObjectMapper mapper;
    private final Scanner scanner;

    private final String QUIT = "QUIT";
    private final String BROADCAST_REQ = "BROADCAST_REQ";
    private final String BYE = "BYE";

    ClientInputThread(PrintWriter writer, ObjectMapper objectMapper) {
        this.writer = writer;
        this.mapper = objectMapper;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        while (true) {
            // TODO: option menu
            System.out.println("");
            int userInput = getIntBetweenBounds(0, 5);
            scanner.nextLine(); // Consume leftover line

            switch (userInput) {
                case 0 -> quit();
                case 1 -> handleBroadcastRequest();
                case 2 -> terminateConnection();
            }
        }
    }

    private void terminateConnection() {
        writer.println(BYE);
    }

    private void handleBroadcastRequest() {
        try {
            System.out.print("Enter your message: ");
            String message = scanner.nextLine();

            while (message == null || message.isBlank()) {
                System.out.print("Enter your message: ");
                message = scanner.nextLine();
            }

            writer.println("BROADCAST_REQ " + mapper.writeValueAsString(new BroadcastRequestMessage(message)));
        } catch (Exception e) {
            MessageCodePrinter.printMessageFromCode(0);
        }
    }

    private void quit() {
        // TODO: send logout request to server
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
