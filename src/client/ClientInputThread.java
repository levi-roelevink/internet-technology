package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.BroadcastRequestMessage;
import shared.messages.FileTransferRequestMessage;
import shared.messages.FileTransferResponse;
import shared.messages.PrivateMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.UUID;

import static shared.utils.ChecksumGenerator.getFileChecksum;
import static shared.utils.PromptUser.*;

public class ClientInputThread extends Thread {
    private final Socket socket;
    private final PrintWriter writer;
    private final ObjectMapper mapper;
    private final Scanner scanner;
    private final String username;
    private final FileTransferManager fileTransferManager;

    private final String BYE = "BYE";
    private final String MENU = """
            1) Broadcast message
            2) List online users
            3) Private message
            4) File transfer
            5) Respond to file transfer requests
            0) Terminate connection
            Select an option:\s""";

    ClientInputThread(String username, Socket socket, PrintWriter writer, ObjectMapper objectMapper, FileTransferManager fileTransferManager) {
        this.username = username;
        this.socket = socket;
        this.writer = writer;
        this.mapper = objectMapper;
        this.scanner = new Scanner(System.in);
        this.fileTransferManager = fileTransferManager;
    }

    @Override
    public void run() {
        while (true) {
            System.out.print(MENU);
            int userInput = getIntBetweenBounds(0, 5);

            switch (userInput) {
                case 0 -> terminateConnection();
                case 1 -> broadcastRequest();
                case 2 -> listUsers();
                case 3 -> privateMessage();
                case 4 -> fileTransferRequest();
                case 5 -> respondToFileTransferRequest();
            }
        }
    }

    private void respondToFileTransferRequest() {
        // Prompt for username of sender
        String sender = promptForStringInput("Who's request would you like to respond to: ");

        System.out.printf("0 to reject or 1 to accept %s's request: ", sender);
        int input = getIntBetweenBounds(0, 1);

        try {
            // Recipient -> S: FILE_TRANSFER_RESP {"username":"<username>","code":0/1}
            String fileTransferRespJson = mapper.writeValueAsString(new FileTransferResponse(sender, input));
            writer.println("FILE_TRANSFER_RESP " + fileTransferRespJson);
            System.out.println("Recipient -> S: FILE_TRANSFER_RESP");
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
        }

        if (input == 1) {
            // TODO: initiate FileReceiverThread
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
                writer.println("FILE_TRANSFER_REQ " + reqMsg);
                System.out.println("File transfer request sent.");

                fileTransferManager.addPendingFileSendRequest(recipient, uuidString, file);
            } else {
                System.err.println("Couldn't find this file.");
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("Invalid message provided.");
        }
    }

    // TODO: this should probably happen on a new thread so that other chat functionality remains available
    // Unless this happens quick
    private void fileTransfer() {
        try {
            FileInputStream fileInputStream = new FileInputStream("docs/test.txt");
            long bytesTransferred = fileInputStream.transferTo(socket.getOutputStream());

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void terminateConnection() {
        writer.println(BYE);
    }

    private void privateMessage() {
        try {
            String user = promptForStringInput("User to message: ");
            String message = promptForStringInput("Enter your message: ");

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

    private void broadcastRequest() {
        try {
            String message = promptForStringInput("Enter your message: ");
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
}
