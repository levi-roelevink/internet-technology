package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.BroadcastRequestMessage;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Scanner;

public class ClientInputThread extends Thread {
    private final PrintWriter writer;
    private final ObjectMapper mapper;
    private final Scanner scanner;

    private final int QUIT = 0;
    private final int BROADCAST_REQ = 1;

    ClientInputThread(PrintWriter writer, ObjectMapper objectMapper) {
        this.writer = writer;
        this.mapper = objectMapper;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        while (true) {
            int userInput = getIntBetweenBounds(0, 5);

            switch (userInput) {
                case QUIT -> quit();
                case BROADCAST_REQ -> handleBroadcastRequest();
            }
        }
    }

    private void handleBroadcastRequest() {
        try {
            String message = null;
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
