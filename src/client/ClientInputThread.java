package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.BroadcastRequestMessage;
import shared.messages.FileTransferRequestMessage;
import shared.messages.FileTransferResponse;
import shared.messages.PrivateMessage;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static shared.messages.MessageSender.sendLine;
import static shared.utils.ChecksumGenerator.getFileChecksum;
import static shared.utils.PromptUser.*;

public class ClientInputThread extends Thread {
    private final PrintWriter writer;
    private final ObjectMapper mapper;
    private final FileTransferManager fileTransferManager;
    private final Client client;

    private final String BYE = "BYE";
    private final String MENU = """
            1) Broadcast message
            2) List online users
            3) Private message
            4) File transfer
            0) Terminate connection
            Select an option:\s""";

    ClientInputThread(PrintWriter writer, ObjectMapper objectMapper, FileTransferManager fileTransferManager, Client client) {
        this.writer = writer;
        this.mapper = objectMapper;
        this.fileTransferManager = fileTransferManager;
        this.client = client;
    }

    @Override
    public void run() {
        while (true) {
            System.out.print(MENU);
            int userInput = getIntBetweenBounds(0, 4);

            switch (userInput) {
                case 0 -> terminateConnection();
                case 1 -> broadcastRequest();
                case 2 -> listUsers();
                case 3 -> privateMessage();
                case 4 -> fileTransferRequest();
            }
        }
    }

    private void fileTransferRequest() {
        String recipient = promptForStringInput("User to transfer file to: ");
        String path = promptForStringInput("File path: ");

        try {
            File file = new File(path);
            if (file.exists()) {
                String fileName = file.getName();
                long fileSize = file.length() / 10;

                String uuidString = UUID.randomUUID().toString();
                String checksum = getFileChecksum(file);

                String reqMsg = mapper.writeValueAsString(new FileTransferRequestMessage(recipient, fileName, fileSize, uuidString, checksum));
                // I think that this line writes to the server but also to the client's own server input thread
                sendLine("FILE_TRANSFER_REQ " + reqMsg, writer);
                System.out.println("File transfer request sent.");

                String testMsg = mapper.writeValueAsString(new FileTransferRequestMessage("testMsg", fileName, fileSize, uuidString, checksum));
                sendLine("FILE_TRANSFER_REQ " + testMsg, writer);

                fileTransferManager.addPendingFileSendRequest(recipient, uuidString, file);
            } else {
                System.err.println("Couldn't find this file.");
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Invalid message provided.");
        }
    }

    private void terminateConnection() {
        sendLine(BYE, writer);
    }

    private void privateMessage() {
        try {
            String user = promptForStringInput("User to message: ");
            String message = promptForStringInput("Enter your message: ");

            String jsonString = mapper.writeValueAsString(new PrivateMessage(user, message));
            sendLine("PRIVATE_MESSAGE_REQ " + jsonString, writer);
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
        }
    }

    private void listUsers() {
        sendLine("LIST_USERS_REQ", writer);
    }

    private void broadcastRequest() {
        try {
            String message = promptForStringInput("Enter your message: ");
            sendLine("BROADCAST_REQ " + mapper.writeValueAsString(new BroadcastRequestMessage(message)), writer);
        } catch (Exception e) {
            MessageCodePrinter.printMessageFromCode(0);
        }
    }

    private void quit() {
        // Send logout request to server
        terminateConnection();

        System.out.println("Bye.");
        System.exit(0);
    }
}
