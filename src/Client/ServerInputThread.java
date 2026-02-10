package Client;

import Client.FileTransferManaging.FileSenderThread;
import Client.FileTransferManaging.FileTransferManager;
import Messages.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.security.*;
import java.security.spec.*;

import static Messages.MessageSender.sendLine;

public class ServerInputThread extends Thread {
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final ObjectMapper mapper;
    private final FileTransferManager fileTransferManager;
    private final Client client;

    ServerInputThread(BufferedReader reader, PrintWriter writer, ObjectMapper mapper, FileTransferManager fileTransferManager, Client client) {
        this.reader = reader;
        this.writer = writer;
        this.mapper = mapper;
        this.fileTransferManager = fileTransferManager;
        this.client = client;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String line = reader.readLine();
                if (line == null) { //When server is unexpectedly shut down line will be null
                    handleDSCN("{\"code\":0}");
                } else {
                    String[] lineParts = line.split(" ", 2);
                    switch (lineParts[0]) {
                        case "JOINED" -> handleJoinedMessage(lineParts[1]);
                        case "BROADCAST_RESP" -> handleBroadcastResponse(lineParts[1]);
                        case "BROADCAST" -> handleBroadcast(lineParts[1]);
                        case "PING" -> sendLine("PONG", writer);
                        case "DSCN" -> handleDSCN(lineParts[1]);
                        case "PONG_ERROR" -> handlePongError(lineParts[1]);
                        case "BYE_RESP" -> handleByeResponse(lineParts[1]);
                        case "LEFT" -> handleLeftMessage(lineParts[1]);
                        case "LIST_USERS_RESP" -> handleListUsersResponse(lineParts[1]);
                        case "PRIVATE_MESSAGE_RESP" -> handlePrivateMessageResponse(lineParts[1]);
                        case "PRIVATE_MESSAGE" -> handlePrivateMessage(lineParts[1]);
                        case "NUMBER_SETUP_RESP" -> handleNumberSetupResponse(lineParts[1]);
                        case "NUMBER_SETUP" -> handleNumberSetupMessage(lineParts[1]);
                        case "NUMBER_JOIN_RESP" -> handleNumberJoinResponse(lineParts[1]);
                        case "NUMBER_START" ->
                                System.out.println("The number guessing game has started! Use the 'number_guess <number>' command to guess a number");
                        case "NUMBER_CANCEL" ->
                                System.out.println("The number guessing game has been cancelled due to lack of participants");
                        case "NUMBER_GUESS_RESP" -> handleGuessResponse(lineParts[1]);
                        case "NUMBER_RESULT" -> handleResult(lineParts[1]);
                        case "FILE_TRANSFER_REQ" -> handleFileTransferRequest(lineParts[1]);
                        case "FILE_TRANSFER_RESP" -> handleFileTransferResponse(lineParts[1]);
                        case "PUBLIC_KEY" -> handlePublicKey(lineParts[1]);
                        case "SESSION_KEY" -> handleSessionKey(lineParts[1]);
                        case "ENCRYPTED_MESSAGE" -> handleEncryptedPrivateMessage(lineParts[1]);
                        case "KEY_RESP" -> handleKeyResponse(lineParts[1]);
                        case "ENCRYPTED_MESSAGE_RESP" -> handleEncryptedPrivateMessageResponse(lineParts[1]);
                        case "UNKNOWN_COMMAND" -> {
                            System.out.println("This is an invalid message");
                            MessageCodePrinter.printHelpMessage();
                        }
                        case "PARSE_ERROR" -> System.err.println("Message couldn't be parsed");
                    }
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
                System.exit(0);
            }
        }
    }

    private void handleJoinedMessage(String message) throws Exception {
        GenericMessage joinedMessage = mapper.readValue(message, GenericMessage.class);

        System.out.println(joinedMessage.username() + " has joined");
    }

    private void handleBroadcastResponse(String message) throws Exception {
        GenericMessage broadcastResponseMessage = mapper.readValue(message, GenericMessage.class);

        if (broadcastResponseMessage.status().equals("OK")) {
            System.out.println("Message has been sent successfully");
        } else {
            MessageCodePrinter.printMessageFromCode(broadcastResponseMessage.code());
        }
    }

    private void handleBroadcast(String message) throws Exception {
        GenericMessage broadcastMessage = mapper.readValue(message, GenericMessage.class);

        System.out.println(broadcastMessage.username() + ": " + broadcastMessage.message());
    }

    private void handleDSCN(String message) throws Exception {
        GenericMessage disconnectMessage = mapper.readValue(message, GenericMessage.class);

        MessageCodePrinter.printMessageFromCode(disconnectMessage.code());

        System.exit(0);
    }

    private void handlePongError(String message) throws Exception {
        GenericMessage pongErrorMessage = mapper.readValue(message, GenericMessage.class);

        MessageCodePrinter.printMessageFromCode(pongErrorMessage.code());
    }

    private void handleByeResponse(String message) throws Exception {
        GenericMessage byeResponseMessage = mapper.readValue(message, GenericMessage.class);

        if (byeResponseMessage.status().equals("OK")) {
            System.out.println("Bye bye");
        } else {
            MessageCodePrinter.printMessageFromCode(byeResponseMessage.code());
        }

        System.exit(0);
    }

    private void handleLeftMessage(String message) throws Exception {
        GenericMessage leftMessage = mapper.readValue(message, GenericMessage.class);

        System.out.println(leftMessage.username() + " has left");
        fileTransferManager.removePendingFileSendRequest(leftMessage.username());
        fileTransferManager.removePendingFileReceiveRequest(leftMessage.username());
        client.removeSessionKey(leftMessage.username());
        client.removeStoredEncryptedMessage(leftMessage.username());
    }

    private void handleNumberSetupResponse(String message) throws JsonProcessingException {
        GenericMessage setupResponseMessage = mapper.readValue(message, GenericMessage.class);

        if (setupResponseMessage.status().equals("OK")) {
            System.out.println("Number guessing game set up successfully");
        } else {
            MessageCodePrinter.printMessageFromCode(setupResponseMessage.code());
        }
    }

    private void handleListUsersResponse(String message) throws JsonProcessingException {
        UserListMessage handleListUsersResponseMessage = mapper.readValue(message, UserListMessage.class);

        for (String user : handleListUsersResponseMessage.userList()) {
            System.out.println(user);
        }
    }

    private void handlePrivateMessageResponse(String message) throws JsonProcessingException {
        GenericMessage handlePrivateMessageResponseMessage = mapper.readValue(message, GenericMessage.class);

        if (handlePrivateMessageResponseMessage.status().equals("OK")) {
            System.out.println("Message has been sent successfully");
        } else {
            MessageCodePrinter.printMessageFromCode(handlePrivateMessageResponseMessage.code());
        }
    }

    private void handlePrivateMessage(String message) throws JsonProcessingException {
        GenericMessage handlePrivateMessageMessage = mapper.readValue(message, GenericMessage.class);

        System.out.println("Whisper from " + handlePrivateMessageMessage.username() + ": " + handlePrivateMessageMessage.message());
    }

    private void handleNumberSetupMessage(String message) throws JsonProcessingException {
        GenericMessage setupMessage = mapper.readValue(message, GenericMessage.class);

        System.out.println(setupMessage.username() + " has started a number guessing game! You have 10 seconds to use the 'number_join' command to join");
    }

    private void handleNumberJoinResponse(String message) throws JsonProcessingException {
        GenericMessage joinResponseMessage = mapper.readValue(message, GenericMessage.class);

        if (joinResponseMessage.status().equals("OK")) {
            System.out.println("Number guessing game joined successfully");
        } else {
            MessageCodePrinter.printMessageFromCode(joinResponseMessage.code());
        }
    }

    private void handleGuessResponse(String message) throws JsonProcessingException {
        GenericMessage guessResponseMessage = mapper.readValue(message, GenericMessage.class);

        if (!guessResponseMessage.status().equals("ERROR")) {
            switch (guessResponseMessage.code()) {
                case -1 -> System.out.println("Your guess was too low.");
                case 0 -> System.out.println("Your guess was correct!");
                case 1 -> System.out.println("Your guess was too high.");
                default -> throw new RuntimeException("Unexpected code");
            }
        } else {
            MessageCodePrinter.printMessageFromCode(guessResponseMessage.code());
        }
    }

    private void handleResult(String message) throws JsonProcessingException {
        NumberResult resultMessage = mapper.readValue(message, NumberResult.class);

        for (int i = 0; i < resultMessage.results().size(); i++) {
            System.out.println((i + 1) + " " + resultMessage.results().get(i));
        }
    }

    private void handleFileTransferRequest(String message) throws JsonProcessingException {
        FileTransferRequestMessage requestMessage = mapper.readValue(message, FileTransferRequestMessage.class);
        System.out.println("RECEIVED " + requestMessage);

        fileTransferManager.addPendingFileReceiveRequest(requestMessage.username(), requestMessage.id(), requestMessage.checksum(), requestMessage.filename());
        System.out.println(requestMessage.username() + " would like to send you a " + requestMessage.filesize() + " kb file named " + requestMessage.filename());
        System.out.println("Use file_accept " + requestMessage.username() + " or file_decline " + requestMessage.username() + " to accept or decline.");
    }

    private void handleFileTransferResponse(String message) throws JsonProcessingException {
        FileTransferResponseMessage responseMessage = mapper.readValue(message, FileTransferResponseMessage.class);

        if (!responseMessage.status().equals("ERROR")) {
            switch (responseMessage.code()) {
                case 0:
                    fileTransferManager.removePendingFileSendRequest(responseMessage.username());
                    System.out.println(responseMessage.username() + " declined your file transfer request.");
                    break;
                case 1:
                    System.out.println(responseMessage.username() + " accepted your file transfer request.");
                    new FileSenderThread(fileTransferManager, responseMessage.username()).start();
                    break;
                default:
                    throw new RuntimeException("Unexpected code");
            }
        } else {
            MessageCodePrinter.printMessageFromCode(responseMessage.code());
        }
    }

    private void handlePublicKey(String message) throws JsonProcessingException, InvalidKeySpecException {
        KeyMessage publicKeyMessage = mapper.readValue(message, KeyMessage.class);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyMessage.key());
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            SecretKey sessionKey = keyGenerator.generateKey();
            byte[] encodedKey = sessionKey.getEncoded();

            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedEncodedSessionKey = cipher.doFinal(encodedKey);

            KeyMessage sessionKeyMessage = new KeyMessage(publicKeyMessage.username(), encryptedEncodedSessionKey);
            sendLine("SESSION_KEY " + mapper.writeValueAsString(sessionKeyMessage), writer);
            client.addSessionKey(publicKeyMessage.username(), sessionKey);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleSessionKey(String message) throws JsonProcessingException, InvalidKeySpecException {
        KeyMessage sessionKeyMessage = mapper.readValue(message, KeyMessage.class);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(client.getPrivateKey());
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            Cipher decryptCipher = Cipher.getInstance("RSA");
            decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] decryptedSessionKey = decryptCipher.doFinal(sessionKeyMessage.key());

            SecretKey sessionKey = new SecretKeySpec(decryptedSessionKey, "AES");
            client.addSessionKey(sessionKeyMessage.username(), sessionKey);

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, client.getSessionKey(sessionKeyMessage.username()));
            byte[] encryptedMessage = cipher.doFinal(client.getStoredEncryptedMessage(sessionKeyMessage.username()).getBytes());
            sendLine("ENCRYPTED_MESSAGE_REQ " + mapper.writeValueAsString(new EncryptedPrivateMessage(sessionKeyMessage.username(), encryptedMessage)), writer);
            client.removeStoredEncryptedMessage(sessionKeyMessage.username());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleEncryptedPrivateMessage(String message) throws JsonProcessingException {
        EncryptedPrivateMessage handlePrivateMessageMessage = mapper.readValue(message, EncryptedPrivateMessage.class);

        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, client.getSessionKey(handlePrivateMessageMessage.username()));

            byte[] decryptedBytes = cipher.doFinal(handlePrivateMessageMessage.message());
            String decryptedMessage = new String(decryptedBytes);

            System.out.println("Encrypted whisper from " + handlePrivateMessageMessage.username() + ": " + decryptedMessage);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleKeyResponse(String message) throws JsonProcessingException {
        ErrorResponseMessage keyResponse = mapper.readValue(message, ErrorResponseMessage.class);
        MessageCodePrinter.printMessageFromCode(keyResponse.code());
    }

    private void handleEncryptedPrivateMessageResponse(String message) throws JsonProcessingException {
        ErrorResponseMessage encryptedPrivateMessageResponse = mapper.readValue(message, ErrorResponseMessage.class);

        if (encryptedPrivateMessageResponse.status().equals("OK")) {
            System.out.println("Encrypted private message sent successfully");
        } else {
            MessageCodePrinter.printMessageFromCode(encryptedPrivateMessageResponse.code());
        }
    }
}
