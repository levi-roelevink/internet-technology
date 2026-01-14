package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

import static shared.utils.UsernameValidation.usernameIsValid;

public class ServerThread extends Thread {
    private final Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private ObjectMapper mapper;
    private Server server;
    private final String PING = "PING";
    private final String PONG = "PONG";
    private final String BYE = "BYE";
    private final String LOGIN = "LOGIN";
    private final String BROADCAST_REQ = "BROADCAST_REQ";
    private final String LIST_USERS_REQ = "LIST_USERS_REQ";
    private final String PRIVATE_MESSAGE_REQ = "PRIVATE_MESSAGE_REQ";
    private final String FILE_TRANSFER_REQ = "FILE_TRANSFER_REQ";
    private final String FILE_TRANSFER_RESP = "FILE_TRANSFER_RESP";
    private final String UNKNOWN_COMMAND = "UNKNOWN_COMMAND";
    private String username;
    private PingInfo pingInfo;

    public ServerThread(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    public void run() {
        assert server != null : "Server instance is null.";

        try {
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            mapper = new ObjectMapper();
            pingInfo = new PingInfo();

            welcomeClient();
            awaitLogin();
            informOthersOfJoin();

            PingThread pingThread = new PingThread(pingInfo);
            pingThread.start();

            String inputLine;

            while (pingInfo.isConnectionAlive()) {
                while ((inputLine = reader.readLine()) != null) {
                    String[] lineParts = inputLine.split(" ", 2);

                    switch (lineParts[0]) {
                        case PONG -> handlePong();
                        case BROADCAST_REQ -> handleBroadcastRequest(lineParts[1]);
                        case LIST_USERS_REQ -> handleUserListRequest();
                        case PRIVATE_MESSAGE_REQ -> handlePrivateMessageRequest(lineParts[1]);
                        case FILE_TRANSFER_REQ -> handleFileTransferRequest(lineParts[1]);
                        case FILE_TRANSFER_RESP -> handleFileTransferResponse(lineParts[1]);
                        case BYE -> handleBye();
                        default -> handleUnknownCommand();
                        // TODO: BYE case which also breaks the loop
                    }

                    writer.println(inputLine);
                }
            }

            reader.close();
            writer.close();
            socket.close();
            // TODO: disconnect / terminate
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void handleFileTransferResponse(String jsonString) throws JsonProcessingException {
        try {
            FileTransferResponse message = mapper.readValue(jsonString, FileTransferResponse.class);

            // Send to message.username()'s PrintWriter
            PrintWriter senderWriter = server.getUser(message.username());
            if (senderWriter == null) {
                // TODO: error code 2000
                return;
            }

            String fileTransferRespJson = mapper.writeValueAsString(new FileTransferResponse(username, message.code()));
            // S -> sender: FILE_TRANSFER_RESP {"status":"OK","username":"<username>","code":0/1}
            senderWriter.println("FILE_TRANSFER_RESP " + fileTransferRespJson);
        } catch (JsonProcessingException e) {
            writer.println("PARSE_ERROR");
        }
    }

    private void handleFileTransferRequest(String jsonString) throws JsonProcessingException {
        int errorCode = -1;

        try {
            FileTransferRequest request = mapper.readValue(jsonString, FileTransferRequest.class);

            PrintWriter recipient = server.getUser(request.username());
            if (username == null) {
                errorCode = 2000;
            } else if (recipient == null) {
                errorCode = 5000;
            }

            if (errorCode != -1) {
                // TODO: S -> C: FILE_TRANSFER_RESP {"status":"ERROR","code":<error code>}
            } else {
                String forwardReqJson = mapper.writeValueAsString(new FileTransferRequest(username, request.file()));
                recipient.println("FILE_TRANSFER_REQ " + forwardReqJson);
            }
        } catch (JsonProcessingException e) {
            writer.println("PARSE_ERROR");
        }
    }

    private void handlePrivateMessageRequest(String jsonString) throws JsonProcessingException {
        try {
            PrivateMessage message = mapper.readValue(jsonString, PrivateMessage.class);

            if (username == null) {
                String respMsgJson = mapper.writeValueAsString(new ResponseMessage("ERROR", 2000));
                writer.println("PRIVATE_MESSAGE_RESP " + respMsgJson);
                return;
            }

            HashMap<String, PrintWriter> users = server.getUsers();
            PrintWriter recipientWriter = users.get(message.username());
            if (recipientWriter == null) {
                String respMsgJson = mapper.writeValueAsString(new ResponseMessage("ERROR", 5000));
                writer.println("PRIVATE_MESSAGE_RESP " + respMsgJson);
                return;
            }

            // Route message to recipient
            String privateMessageJson = mapper.writeValueAsString(new PrivateMessage(username, message.message()));
            recipientWriter.println("PRIVATE_MESSAGE " + privateMessageJson);

            // Send confirmation to sender
            String respMsgJson = mapper.writeValueAsString(new ResponseMessage("OK"));
            writer.println("PRIVATE_MESSAGE_RESP " + respMsgJson);
        } catch (JsonProcessingException e) {
            writer.println("PARSE_ERROR");
        }
    }

    private void handleUserListRequest() throws JsonProcessingException {
        try {
            if (username == null) {
                String errorRespJson = mapper.writeValueAsString(new ResponseMessage("ERROR", 2000));
                // S -> C: LIST_USERS_RESP {"status":"ERROR","code":<error code>}
                writer.println("LIST_USERS_RESP " + errorRespJson);
                return;
            }

            String[] otherUsers = Arrays.stream(server.getUsernames()).filter(u -> !u.equals(username)).toArray(String[]::new);
            String listUserRespJson = mapper.writeValueAsString(new UserListMessage("OK", otherUsers));

            // S -> C: LIST_USERS_RESP {"status":"OK","users":["<username1>", "<username2>",...]}
            writer.println("LIST_USERS_RESP " + listUserRespJson);
        } catch (JsonProcessingException e) {
            writer.println("PARSE_ERROR");
        }
    }

    private void handleUnknownCommand() {
        // S -> C: UNKNOWN_COMMAND
        writer.println(UNKNOWN_COMMAND);
    }

    private void handleBye() throws JsonProcessingException {
        try {
            String leftJson = mapper.writeValueAsString(new UsernameMessage(username));
            writeToAllButMe("LEFT " + leftJson);

            String byeRespJson = mapper.writeValueAsString(new StatusMessage("OK"));
            writer.println("BYE_RESP " + byeRespJson);

            server.removeUser(username);
            // TODO: actually terminate the server thread
        } catch (JsonProcessingException e) {
            writer.println("PARSE_ERROR");
        }
    }

    private void handleBroadcastRequest(String jsonString) throws JsonProcessingException {
        if (username == null) {
            // User is not logged in but trying to send a message anyway
            // S -> C: BROADCAST_RESP {"status": "ERROR","code":<error code>}
            String errorJson = mapper.writeValueAsString(new ResponseMessage("ERROR", 2000));
            writer.println("BROADCAST_RESP " + errorJson);
            return;
        }

        try {
            BroadcastRequestMessage broadcastRequestMessage = mapper.readValue(jsonString, BroadcastRequestMessage.class);

            // S -> others: BROADCAST {"username":"<username>","message":"<message>"}
            String broadcastJson = mapper.writeValueAsString(new BroadcastMessage(broadcastRequestMessage.message(), username));
            writeToAllButMe("BROADCAST " + broadcastJson);

            // S -> C: BROADCAST_RESP {"status":"OK"}
            String broadcastRespJson = mapper.writeValueAsString(new ResponseMessage("OK"));
            writer.println("BROADCAST_RESP " + broadcastRespJson);
        } catch (JsonProcessingException e) {
            // S -> C: PARSE_ERROR
            writer.println("PARSE_ERROR");
        }
    }

    private void handlePong() throws JsonProcessingException {
        try {
            if (!pingInfo.isAwaitingPong()) {
                String jsonString = mapper.writeValueAsString(new CodeMessage(8000));
                writer.println("PONG_ERROR " + jsonString);
            } else {
                pingInfo.setAwaitingPong(false);
            }
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
        }
    }

    private void awaitLogin() throws IOException {
        boolean loggedIn = false;

        while (!loggedIn) {
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                String[] lineParts = inputLine.split(" ", 2);
                if (LOGIN.equals(lineParts[0])) {
                    try {
                        UsernameMessage message = mapper.readValue(lineParts[1], UsernameMessage.class);

                        // TODO: check for duplicate username
                        if (!usernameIsValid(message.username())) {
                            String jsonString = mapper.writeValueAsString(new ResponseMessage("ERROR", 5001));
                            writer.println("LOGIN_RESP " + jsonString);
                        } else {
                            username = message.username();
                            server.addUser(username, writer);
                            loggedIn = true;

                            String jsonString = mapper.writeValueAsString(new ResponseMessage("OK"));
                            writer.println("LOGIN_RESP " + jsonString);
                            break;
                        }
                    } catch (IOException e) {
                        // S -> C: PARSE_ERROR
                        writer.println("PARSE_ERROR");
                    }
                }
            }
        }

        assert usernameIsValid(username) : "Username set is invalid";
    }

    private void informOthersOfJoin() throws JsonProcessingException {
        try {
            String message = "JOINED " + mapper.writeValueAsString(new UsernameMessage(username));
            writeToAllButMe(message);
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
        }
    }

    // The message argument should be formatted like "JOINED {"username":"<username>"}"
    private void writeToAllButMe(String message) {
        HashMap<String, PrintWriter> users = server.getUsers();

        users.forEach((name, writer) -> {
            if (!name.equals(username)) {
                writer.println(message);
            }
        });
    }

    private void welcomeClient() throws JsonProcessingException {
        try {
            String jsonString = mapper.writeValueAsString(new WelcomeMessage("Welcome to the server."));
            writer.println("WELCOME " + jsonString);
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
        }
    }

    private class PingThread extends Thread {
        private final PingInfo pingInfo;

        PingThread(PingInfo pingInfo) {
            this.pingInfo = pingInfo;
        }

        public void run() {
            while (true) {
                try {
                    sleep(pingInfo.getPingDelayMs());

                    if (pingInfo.isAwaitingPong()) {
                        // No PONG response within 10_000 MS, hence the client has lost connection
                        pingInfo.killConnection();
                        server.removeUser(username);
                        System.out.println("Removed " + username);

                        String jsonString = mapper.writeValueAsString(new DisconnectMessage(7000));
                        writer.println("DSCN " + jsonString);
                        return;
                    }

                    pingInfo.setAwaitingPong(true);
                    writer.println(PING);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }
}
