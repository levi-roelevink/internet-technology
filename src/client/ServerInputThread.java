package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                        case BROADCAST -> handleBroadcast();
                        case PARSE_ERROR -> MessageCodePrinter.printMessageFromCode(9000);
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private void handleBroadcastResponse(String jsonString) throws JsonProcessingException {
        GenericMessage message = mapper.readValue(jsonString, GenericMessage.class);

        if ("OK".equals(message.status())) {
            System.out.println("Message sent.");
        } else {
            MessageCodePrinter.printMessageFromCode(message.code());
        }
    }

    private void handleBroadcast() {
//        try {
//
//        }
        // Parse the message
        // Just sout?
    }

//    private void temp() {
//        String line;
//
//        while ((line = reader.readLine()) != null) {
//            try {
//                String[] lineParts = line.split(" ", 2);
//                switch (lineParts[0]) {
//                    case "LOGIN_RESP" -> handleLoginResponse(lineParts[1]);
//                }
//            } catch (Exception e) {
//                System.err.println(e);
//            }
//        }
//    }
}
