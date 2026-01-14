package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.*;
import shared.utils.PromptUser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class ServerInputThread extends Thread {
    private final PrintWriter writer;
    private final BufferedReader reader;
    private final ObjectMapper mapper;
    private final String BROADCAST_RESP = "BROADCAST_RESP";
    private final String BROADCAST = "BROADCAST";
    private final String PARSE_ERROR = "PARSE_ERROR";
    private final String PING = "PING";
    private final String PONG = "PONG";
    private final String PONG_ERROR = "PONG_ERROR";
    private final String DSCN = "DSCN";
    private final String OK = "OK";
    private final String BYE_RESP = "BYE_RESP";
    private final String LEFT = "LEFT";
    private final String JOINED = "JOINED";
    private final String LIST_USERS_RESP = "LIST_USERS_RESP";
    private final String PRIVATE_MESSAGE_RESP = "PRIVATE_MESSAGE_RESP";
    private final String PRIVATE_MESSAGE = "PRIVATE_MESSAGE";
    private final String FILE_TRANSFER_REQ = "FILE_TRANSFER_REQ";
    private final String UNKNOWN_COMMAND = "UNKNOWN_COMMAND";
    private final ArrayList<FileTransferRequest> fileTransferRequests = new ArrayList<>();

    ServerInputThread(PrintWriter writer, BufferedReader reader, ObjectMapper mapper) {
        this.writer = writer;
        this.reader = reader;
        this.mapper = mapper;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] lineParts = line.split(" ", 2);
                    switch (lineParts[0]) {
                        case BROADCAST_RESP -> handleBroadcastResponse(lineParts[1]);
                        case BROADCAST -> handleBroadcast(lineParts[1]);
                        case PARSE_ERROR -> handleParseError();
                        case PING -> handlePing();
                        case PONG_ERROR -> handlePongError(lineParts[1]);
                        case DSCN -> handleDisconnect(lineParts[1]);
                        case BYE_RESP -> handleByeResp(lineParts[1]);
                        case LEFT -> handleLeft(lineParts[1]);
                        case JOINED -> handleJoined(lineParts[1]);
                        case LIST_USERS_RESP -> handleListUsersResponse(lineParts[1]);
                        case PRIVATE_MESSAGE_RESP -> handlePrivateMessageResp(lineParts[1]);
                        case PRIVATE_MESSAGE -> handlePrivateMessage(lineParts[1]);
                        case FILE_TRANSFER_REQ -> handleFileTransferReq(lineParts[1]);
                        case UNKNOWN_COMMAND -> handleUnknownCommand();
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private void handleFileTransferReq(String jsonString) throws JsonProcessingException {
        try {
            FileTransferRequest message = mapper.readValue(jsonString, FileTransferRequest.class);

            int index = existingFileTransferRequest(message.username());
            // TODO: this should be a set I think
            if (index != -1) {
                // Replace existing request with new request
                fileTransferRequests.remove(index);
            }
            fileTransferRequests.add(message);

            System.out.printf("Incoming file transfer request\nUsername: %s\nFile: %s\nFile size: %d\n0 to reject or 1 to accept: ", message.username(), message.fileName(), message.fileSize());
            int input = PromptUser.getIntBetweenBounds(0, 1);

            // Recipient -> S: FILE_TRANSFER_RESP {"username":"<username>","code":0/1}
            String fileTransferRespJson = mapper.writeValueAsString(new FileTransferResponse(message.username(), input));
            writer.println("FILE_TRANSFER_RESP " + fileTransferRespJson);

            // TODO: File transfer request was accepted so connect to server through a new socket
            if (input == 1) {

            }
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
        }
    }

    private int existingFileTransferRequest(String sender) {
        int size = fileTransferRequests.size();
        if (size == 0) return -1;

        for (int i = 0; i < fileTransferRequests.size(); i++) {
            if (sender.equals(fileTransferRequests.get(i).username())) {
                return i;
            }
        }

        return -1;
    }

    private void handlePrivateMessage(String jsonString) throws JsonProcessingException {
        try {
            PrivateMessage message = mapper.readValue(jsonString, PrivateMessage.class);

            System.out.printf("Private message from %s: %s\n", message.username(), message.message());
        } catch (JsonProcessingException e) {
            // TODO: how else could this exception be handled?
            System.err.println(e.getMessage());
        }
    }

    private void handlePrivateMessageResp(String jsonString) throws JsonProcessingException {
        try {
            ResponseMessage message = mapper.readValue(jsonString, ResponseMessage.class);

            if (OK.equals(message.status())) {
                System.out.println("Private message sent.");
            } else {
                MessageCodePrinter.printMessageFromCode(message.code());
            }
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
        }
    }

    private void handleListUsersResponse(String jsonString) throws JsonProcessingException {
        try {
            UserListMessage message = mapper.readValue(jsonString, UserListMessage.class);

            if (!OK.equals(message.status())) {
                MessageCodePrinter.printMessageFromCode(2000);
            } else {
                String[] users = message.users();
                // If array length is 0 of course print that there are no other users online
                if (users.length == 0) {
                    System.out.println("No online users currently, you're all alone.");
                } else {
                    for (String user : users) {
                        System.out.println("-" + user);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            MessageCodePrinter.printMessageFromCode(9000);
        }
    }

    private void handleParseError() {
        MessageCodePrinter.printMessageFromCode(9000);
    }

    private void handleJoined(String jsonString) throws JsonProcessingException {
        UsernameMessage message = mapper.readValue(jsonString, UsernameMessage.class);
        System.out.println(message.username() + " joined.");
    }

    private void handleUnknownCommand() {
        MessageCodePrinter.printMessageFromCode(9001);
    }

    private void handleLeft(String jsonString) throws JsonProcessingException {
        UsernameMessage message = mapper.readValue(jsonString, UsernameMessage.class);
        System.out.println(message.username() + " disconnected.");
    }

    private void handleByeResp(String jsonString) throws JsonProcessingException {
        StatusMessage message = mapper.readValue(jsonString, StatusMessage.class);
        if (OK.equals(message.status())) {
            System.out.println("Bye");
            // TODO: how to actually terminate the client application?
        } else {
            // No error messages available for this path, hence unknown error
            MessageCodePrinter.printMessageFromCode(0);
        }
    }

    private void handleBroadcastResponse(String jsonString) throws JsonProcessingException {
        GenericMessage message = mapper.readValue(jsonString, GenericMessage.class);

        if (OK.equals(message.status())) {
            System.out.println("Message sent.");
        } else {
            MessageCodePrinter.printMessageFromCode(message.code());
        }
    }

    private void handleBroadcast(String jsonString) throws JsonProcessingException {
        BroadcastMessage message = mapper.readValue(jsonString, BroadcastMessage.class);
        System.out.printf("%s: %s\n", message.username(), message.message());
    }

    private void handlePing() {
        writer.println(PONG);
    }

    private void handlePongError(String jsonString) throws JsonProcessingException {
        CodeMessage message = mapper.readValue(jsonString, CodeMessage.class);
        MessageCodePrinter.printMessageFromCode(message.code());
    }

    private void handleDisconnect(String jsonString) throws JsonProcessingException {
        DisconnectMessage message = mapper.readValue(jsonString, DisconnectMessage.class);
        MessageCodePrinter.printMessageFromCode(message.reason());
    }
}
