package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

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
    private final String UNKNOWN_COMMAND = "UNKNOWN_COMMAND";

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
                        case UNKNOWN_COMMAND -> handleUnknownCommand();
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
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
