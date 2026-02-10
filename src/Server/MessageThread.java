package Server;

import Messages.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;

import static Messages.MessageSender.sendLine;

public class MessageThread extends Thread {
    private static final NumberGame NUMBER_GAME = new NumberGame();
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final ObjectMapper mapper;
    private final PingInfo pingInfo;
    private final Server server;
    private String username;

    public MessageThread(Socket socket, BufferedReader reader, PrintWriter writer, ObjectMapper mapper, PingInfo pingInfo, Server server) {
        this.socket = socket;
        this.reader = reader;
        this.writer = writer;
        this.mapper = mapper;
        this.pingInfo = pingInfo;
        this.server = server;
    }

    @Override
    public void run() {
        sendLine("WELCOME {\"message\":\"Welcome to the java server\"}", writer);

        while (pingInfo.isConnected()) {
            try {
                String line = reader.readLine();
                if (line == null) {
                    handleBye();
                } else {
                    String[] lineParts = line.split(" ", 2);
                    switch (lineParts[0]) {
                        case "LOGIN" -> handleLogin(lineParts[1]);
                        case "BROADCAST_REQ" -> handleBroadcastRequest(lineParts[1]);
                        case "PONG" -> handlePong();
                        case "BYE" -> handleBye();
                        case "LIST_USERS_REQ" -> handleListUsersRequest();
                        case "PRIVATE_MESSAGE_REQ" -> handlePrivateMessageRequest(lineParts[1]);
                        case "NUMBER_SETUP_REQ" -> handleNumberSetupRequest();
                        case "NUMBER_JOIN_REQ" -> handleNumberJoinRequest();
                        case "NUMBER_GUESS_REQ" -> handleNumberGuessRequest(lineParts[1]);
                        case "FILE_TRANSFER_REQ" -> handleFileTransferRequest(lineParts[1]);
                        case "FILE_TRANSFER_RESP" -> handleFileTransferResponse(lineParts[1]);
                        case "PUBLIC_KEY" -> handlePublicKey(lineParts[1]);
                        case "SESSION_KEY" -> handleSessionKey(lineParts[1]);
                        case "ENCRYPTED_MESSAGE_REQ" -> handleEncryptedMessageRequest(lineParts[1]);
                        default -> sendLine("UNKNOWN_COMMAND", writer);
                    }
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
                pingInfo.disconnect();
            }
        }
        if (username != null) {
            server.removeUser(username);
            for (PrintWriter entryWriter : server.getUsers().values()) {
                sendLine("LEFT {\"username\":\"" + username + "\"}", entryWriter);
            }
        }
    }

    private void handleLogin(String message) {
        try {
            if (message.isEmpty()) {
                sendLine("LOGIN_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", 1001)), writer);
            } else {
                GenericMessage loginMessage = mapper.readValue(message, GenericMessage.class);
                boolean validLogin = true;
                int loginErrorCode = -1;

                if (!loginMessage.username().toUpperCase().matches("[A-Z0-9_]{3,14}")) { //If username is invalid
                    loginErrorCode = 1001;
                    validLogin = false;
                } else if (username != null) { //If user is already logged in
                    loginErrorCode = 1002;
                    validLogin = false;
                } else { //If user with username is already logged in
                    if (server.containsUser(loginMessage.username())) {
                        loginErrorCode = 1000;
                        validLogin = false;
                    }
                }

                if (!validLogin) {
                    sendLine("LOGIN_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", loginErrorCode)), writer);
                } else {
                    //Add new valid user to HashMap of users
                    username = loginMessage.username();
                    server.addUser(loginMessage.username(), writer);
                    for (Map.Entry<String, PrintWriter> entry : server.getUsers().entrySet()) {
                        String entryUsername = entry.getKey();
                        PrintWriter entryWriter = entry.getValue();
                        if (entryUsername.equalsIgnoreCase(loginMessage.username())) {
                            sendLine("LOGIN_RESP " + mapper.writeValueAsString(new OkResponseMessage("OK")), writer);
                        } else {
                            sendLine("JOINED " + mapper.writeValueAsString(new UsernameMessage(username)), entryWriter); //Sends JOINED message to all users except the one that just joined
                        }
                    }
                    PingThread pingThread = new PingThread(socket, writer, pingInfo);
                    pingThread.start();
                }
            }
        } catch (JsonProcessingException e) {
            sendLine("PARSE_ERROR", writer);
        }
    }

    private void handleBroadcastRequest(String message) throws JsonProcessingException {
        if (username == null) {
            sendLine("BROADCAST_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", 2000)), writer);
        } else {
            try {
                GenericMessage broadcastRequestMessage = mapper.readValue(message, GenericMessage.class);
                BroadcastMessage broadcast = new BroadcastMessage(username, broadcastRequestMessage.message());
                for (Map.Entry<String, PrintWriter> entry : server.getUsers().entrySet()) {
                    String entryUsername = entry.getKey();
                    PrintWriter entryWriter = entry.getValue();
                    if (username.toLowerCase().equals(entryUsername)) {
                        sendLine("BROADCAST_RESP " + mapper.writeValueAsString(new OkResponseMessage("OK")), entryWriter);
                    } else {
                        sendLine("BROADCAST " + mapper.writeValueAsString(broadcast), entryWriter);
                    }
                }
            } catch (JsonProcessingException e) {
                sendLine("PARSE_ERROR", writer);
            }
        }
    }

    private void handlePong() throws JsonProcessingException {
        if (pingInfo.isAwaitingPing()) {
            pingInfo.stopPing();
        } else {
            sendLine("PONG_ERROR " + mapper.writeValueAsString(new CodeMessage(4000)), writer);
        }
    }

    private void handleBye() throws JsonProcessingException {
        sendLine("BYE_RESP " + mapper.writeValueAsString(new OkResponseMessage("OK")), writer);
        if (username != null) {
            server.removeUser(username);
            for (PrintWriter entryWriter : server.getUsers().values()) {
                sendLine("LEFT " + mapper.writeValueAsString(new UsernameMessage(username)), entryWriter);
            }
        }
        pingInfo.disconnect();
    }

    private void handleListUsersRequest() throws JsonProcessingException {
        if (username == null) {
            sendLine("LIST_USERS_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", 2000)), writer);
        } else {
            UserListMessage userListMessage = new UserListMessage(server.getUsers().keySet(), "OK");

            sendLine("LIST_USERS_RESP " + mapper.writeValueAsString(userListMessage), writer);
        }
    }

    private void handlePrivateMessageRequest(String message) throws JsonProcessingException {
        GenericMessage privateMessageRequestMessage = mapper.readValue(message, GenericMessage.class);
        boolean validRecipient = false;

        if (username == null) {
            sendLine("PRIVATE_MESSAGE_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", 2000)), writer);
        } else {
            if (server.containsUser(privateMessageRequestMessage.username())) {
                validRecipient = true;
                //Sends username of the sender and the message to the recipient
                sendLine("PRIVATE_MESSAGE " + mapper.writeValueAsString(new BroadcastMessage(username, privateMessageRequestMessage.message())), server.getWriter(privateMessageRequestMessage.username()));
            }

            if (validRecipient) {
                sendLine("PRIVATE_MESSAGE_RESP " + mapper.writeValueAsString(new OkResponseMessage("OK")), writer);
            } else {
                sendLine("PRIVATE_MESSAGE_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", 5000)), writer);
            }
        }
    }

    private void handleNumberSetupRequest() throws JsonProcessingException {
        int errorCode = 0;
        if (username == null) {
            errorCode = 2000;
        } else if (!NUMBER_GAME.isIdle()) {
            errorCode = 2000;
        }
        if (errorCode == 0) {
            NUMBER_GAME.setupGame(username, writer);
            for (Map.Entry<String, PrintWriter> entry : server.getUsers().entrySet()) {
                String entryUsername = entry.getKey();
                PrintWriter entryWriter = entry.getValue();
                if (username.toLowerCase().equals(entryUsername)) {
                    sendLine("NUMBER_SETUP_RESP " + mapper.writeValueAsString(new OkResponseMessage("OK")), entryWriter);
                } else {
                    sendLine("NUMBER_SETUP " + mapper.writeValueAsString(new UsernameMessage(username)), entryWriter);
                }
            }
        } else {
            sendLine("NUMBER_SETUP_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", errorCode)), writer);
        }
    }

    private void handleNumberJoinRequest() throws JsonProcessingException {
        int errorCode = 0;
        if (username == null) {
            errorCode = 2000;
        } else if (NUMBER_GAME.isRunning()) {
            errorCode = 6001;
        } else if (NUMBER_GAME.isIdle()) {
            errorCode = 6002;
        } else if (NUMBER_GAME.userHasJoined(username)) {
            errorCode = 6003;
        }
        if (errorCode == 0) {
            NUMBER_GAME.joinGame(username, writer);
            sendLine("NUMBER_JOIN_RESP " + mapper.writeValueAsString(new OkResponseMessage("OK")), writer);
        } else {
            sendLine("NUMBER_JOIN_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", errorCode)), writer);
        }
    }

    private void handleNumberGuessRequest(String message) throws JsonProcessingException {
        int errorCode = 0;
        if (username == null) {
            errorCode = 2000;
        } else if (!NUMBER_GAME.isRunning()) {
            errorCode = 6002;
        } else if (!NUMBER_GAME.userHasJoined(username)) {
            errorCode = 6004;
        } else if (NUMBER_GAME.userHasGuessedNumber(username)) {
            errorCode = 6006;
        }
        if (errorCode == 0) {
            try {
                NumberGuess guessMessage = mapper.readValue(message, NumberGuess.class);
                int guess = guessMessage.number();
                int number = NUMBER_GAME.getNumber();
                if (guess == number) {
                    NUMBER_GAME.addResult(username);
                    sendLine("NUMBER_GUESS_RESP " + mapper.writeValueAsString(new NumberGuessResponseMessage("OK", 0)), writer);
                } else if (guess < number) {
                    sendLine("NUMBER_GUESS_RESP " + mapper.writeValueAsString(new NumberGuessResponseMessage("OK", -1)), writer);
                } else {
                    sendLine("NUMBER_GUESS_RESP " + mapper.writeValueAsString(new NumberGuessResponseMessage("OK", 1)), writer);
                }
            } catch (InvalidFormatException e) {
                sendLine("NUMBER_GUESS_RESP " + mapper.writeValueAsString(new NumberGuessResponseMessage("ERROR", 6005)), writer);
            } catch (JsonProcessingException e) {
                sendLine("PARSE_ERROR", writer);
            }
        } else {
            sendLine("NUMBER_GUESS_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", errorCode)), writer);
        }
    }

    private void handleFileTransferRequest(String message) throws JsonProcessingException {
        if (username == null) {
            sendLine("FILE_TRANSFER_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", 2000)), writer);
        } else {
            FileTransferRequestMessage fileTransferRequestMessage = mapper.readValue(message, FileTransferRequestMessage.class);

            if (server.containsUser(fileTransferRequestMessage.username())) {
                sendLine("FILE_TRANSFER_REQ " + mapper.writeValueAsString(new FileTransferRequestMessage(username, fileTransferRequestMessage.filename(), fileTransferRequestMessage.filesize(), fileTransferRequestMessage.id(), fileTransferRequestMessage.checksum())), server.getWriter(fileTransferRequestMessage.username()));
            } else {
                sendLine("FILE_TRANSFER_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", 5000)), writer);
            }
        }
    }

    private void handleFileTransferResponse(String message) throws JsonProcessingException {
        FileTransferResponseMessage fileTransferResponseMessage = mapper.readValue(message, FileTransferResponseMessage.class);

        if (server.containsUser(fileTransferResponseMessage.username())) {
            sendLine("FILE_TRANSFER_RESP " + mapper.writeValueAsString(new FileTransferResponseMessage("OK", username, fileTransferResponseMessage.code())), server.getWriter(fileTransferResponseMessage.username()));
        } else {
            sendLine("FILE_TRANSFER_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", 7000)), writer);
        }
    }

    private void handlePublicKey(String message) throws JsonProcessingException {
        if (username == null) {
            sendLine("KEY_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", 2000)), writer);
        } else {
            KeyMessage publicKeyMessage = mapper.readValue(message, KeyMessage.class);

            if (server.containsUser(publicKeyMessage.username())) {
                sendLine("PUBLIC_KEY " + mapper.writeValueAsString(new KeyMessage(username, publicKeyMessage.key())), server.getWriter(publicKeyMessage.username()));
            } else {
                sendLine("KEY_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", 5000)), writer);
            }
        }
    }

    private void handleSessionKey(String message) throws JsonProcessingException {
        if (username == null) {
            sendLine("KEY_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", 2000)), writer);
        } else {
            KeyMessage sessionKeyMessage = mapper.readValue(message, KeyMessage.class);

            if (server.containsUser(sessionKeyMessage.username())) {
                sendLine("SESSION_KEY " + mapper.writeValueAsString(new KeyMessage(username, sessionKeyMessage.key())), server.getWriter(sessionKeyMessage.username()));
            } else {
                sendLine("KEY_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", 5000)), writer);
            }
        }
    }

    private void handleEncryptedMessageRequest(String message) throws JsonProcessingException {
        EncryptedPrivateMessage privateMessageRequestMessage = mapper.readValue(message, EncryptedPrivateMessage.class);
        boolean validRecipient = false;

        if (username == null) {
            sendLine("ENCRYPTED_MESSAGE_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", 2000)), writer);
        } else {
            if (server.containsUser(privateMessageRequestMessage.username())) {
                validRecipient = true;
                //Sends username of the sender and the message to the recipient
                sendLine("ENCRYPTED_MESSAGE " + mapper.writeValueAsString(new EncryptedPrivateMessage(username, privateMessageRequestMessage.message())), server.getWriter(privateMessageRequestMessage.username()));
            }

            if (validRecipient) {
                sendLine("ENCRYPTED_MESSAGE_RESP " + mapper.writeValueAsString(new OkResponseMessage("OK")), writer);
            } else {
                sendLine("ENCRYPTED_MESSAGE_RESP " + mapper.writeValueAsString(new ErrorResponseMessage("ERROR", 5000)), writer);
            }
        }
    }
}