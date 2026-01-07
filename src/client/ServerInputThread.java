package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.BroadcastMessage;
import shared.messages.CodeMessage;
import shared.messages.DisconnectMessage;
import shared.messages.GenericMessage;

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
                        case PARSE_ERROR -> MessageCodePrinter.printMessageFromCode(9000);
                        case PING -> handlePing();
                        case PONG_ERROR -> handlePongError(lineParts[1]);
                        case DSCN ->  handleDisconnect(lineParts[1]);
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
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
        System.out.printf("%s: %s", message.username(), message.message());
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
        MessageCodePrinter.printMessageFromCode(Integer.parseInt(message.reason()));
    }
}
