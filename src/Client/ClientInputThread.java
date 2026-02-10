package Client;

import Client.FileTransferManaging.FileReceiverThread;
import Client.FileTransferManaging.FileTransferManager;
import Messages.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
import java.util.UUID;

import static Client.FileTransferManaging.ChecksumGenerator.generateChecksum;
import static Messages.MessageSender.sendLine;

public class ClientInputThread extends Thread {
    private final PrintWriter writer;
    private final ObjectMapper mapper;
    private final FileTransferManager fileTransferManager;
    private final Client client;

    ClientInputThread(PrintWriter writer, ObjectMapper mapper, FileTransferManager fileTransferManager, Client client) {
        this.writer = writer;
        this.mapper = mapper;
        this.fileTransferManager = fileTransferManager;
        this.client = client;
    }

    @Override
    public void run() {
        while (true) {
            Scanner scanner = new Scanner(System.in);
            String message = scanner.nextLine();
            if (message.equals("help")) {
                MessageCodePrinter.printHelpMessage();
            } else if (message.equals("logout")) {
                sendLine("BYE", writer);
                break;
            } else if (message.startsWith("broadcast ")) {
                requestBroadcast(message.replaceFirst("broadcast ", ""));
            } else if (message.equals("list_users")) {
                sendLine("LIST_USERS_REQ", writer);
            } else if (message.startsWith("private_message ")) {
                requestPrivateMessage(message.replaceFirst("private_message ", ""));
            } else if (message.equals("number_setup")) {
                sendLine("NUMBER_SETUP_REQ", writer);
            } else if (message.equals("number_join")) {
                sendLine("NUMBER_JOIN_REQ", writer);
            } else if (message.startsWith("number_guess ")) {
                guessNumber(message.replaceFirst("number_guess ", ""));
            } else if (message.startsWith("file_transfer ")) {
                requestFileTransfer(message.replaceFirst("file_transfer ", ""));
            } else if (message.startsWith("file_accept ")) {
                acceptFileTransfer(message.replaceFirst("file_accept ", ""));
            } else if (message.startsWith("file_decline ")) {
                declineFileTransfer(message.replaceFirst("file_decline ", ""));
            } else if (message.startsWith("encrypted_private_message ")) {
                requestEncryptedPrivateMessage(message.replaceFirst("encrypted_private_message ", ""));
            } else {
                System.out.println("Unknown command");
            }
        }
    }

    private void requestBroadcast(String message) {
        try {
            if (!message.isEmpty()) {
                sendLine("BROADCAST_REQ " + mapper.writeValueAsString(new BroadcastRequestMessage(message)), writer);
            } else {
                System.err.println("Message is empty");
            }
        } catch (JsonProcessingException e) {
            sendLine("PARSE_ERROR", writer);
        }
    }

    private void requestPrivateMessage(String messageAndUsername) {
        try {
            if (!messageAndUsername.isEmpty()) {
                String[] parsedRequest = messageAndUsername.split(" ", 2);
                String username = parsedRequest[0];
                String messageToSend = parsedRequest[1];
                sendLine("PRIVATE_MESSAGE_REQ " + mapper.writeValueAsString(new BroadcastMessage(username, messageToSend)), writer);
            } else {
                System.err.println("Invalid message provided");
            }
        } catch (ArrayIndexOutOfBoundsException | JsonProcessingException e) {
            System.err.println("Invalid message provided");
        }
    }

    private void guessNumber(String message) {
        if (!message.isEmpty()) {
            sendLine("NUMBER_GUESS_REQ {\"number\":\"" + message + "\"}", writer);
        } else {
            System.err.println("No number given");
        }
    }

    private void requestFileTransfer(String message) {
        try {
            if (!message.isEmpty()) {
                String[] parsedRequest = message.split(" ", 2);
                String username = parsedRequest[0];
                String path = parsedRequest[1];
                File file = new File(path);
                if (file.exists()) {
                    System.out.println("Processing file...");
                    String filename = file.getName();
                    long filesize = file.length() / 10;
                    int kb = (int) filesize;
                    String id = UUID.randomUUID().toString();
                    String checksum = generateChecksum(file);

                    fileTransferManager.addPendingFileSendRequest(username, id, file);

                    FileTransferRequestMessage requestMessage = new FileTransferRequestMessage(username, filename, kb, id, checksum);
                    sendLine("FILE_TRANSFER_REQ " + mapper.writeValueAsString(requestMessage), writer);
                    System.out.println("File transfer request sent successfully.");

                    FileTransferRequestMessage testMessage = new FileTransferRequestMessage("testMessage", filename, kb, id, checksum);
                    sendLine("FILE_TRANSFER_REQ " + mapper.writeValueAsString(testMessage), writer);
                    System.out.println("Sent test message.");
                } else {
                    System.err.println("Couldn't find this file");
                }
            } else {
                System.err.println("No username or path provided");
            }
        } catch (ArrayIndexOutOfBoundsException | IOException e) {
            System.err.println("Invalid message provided");
        }
    }

    private void acceptFileTransfer(String message) {
        try {
            if (fileTransferManager.receiveRequestExists(message)) {
                sendLine("FILE_TRANSFER_RESP " + mapper.writeValueAsString(new RecipientFileTransferResponseMessage(message, 1)), writer);
                new FileReceiverThread(fileTransferManager, message).start();
                System.out.println("File transfer request accepted");
            } else {
                System.err.println("No pending file transfer request from this user or this user is not connected");
            }
        } catch (JsonProcessingException e) {
            sendLine("PARSE_ERROR", writer);
        }
    }

    private void declineFileTransfer(String message) {
        try {
            if (fileTransferManager.receiveRequestExists(message)) {
                sendLine("FILE_TRANSFER_RESP " + mapper.writeValueAsString(new RecipientFileTransferResponseMessage(message, 0)), writer);
                System.out.println("File transfer request declined.");
            } else {
                System.err.println("No pending file transfer request from this user or this user is not connected");
            }
        } catch (JsonProcessingException e) {
            sendLine("PARSE_ERROR", writer);
        }
    }

    private void requestEncryptedPrivateMessage(String messageAndUsername) {
        try {
            if (!messageAndUsername.isEmpty()) {
                String[] parsedRequest = messageAndUsername.split(" ", 2);
                String username = parsedRequest[0];
                String messageToSend = parsedRequest[1];
                if (client.containsSessionKey(username)) {
                    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                    cipher.init(Cipher.ENCRYPT_MODE, client.getSessionKey(username));
                    byte[] encryptedMessage = cipher.doFinal(messageToSend.getBytes());
                    sendLine("ENCRYPTED_MESSAGE_REQ " + mapper.writeValueAsString(new EncryptedPrivateMessage(username, encryptedMessage)), writer);
                } else {
                    client.addStoredEncryptedMessage(username, messageToSend);
                    sendLine("PUBLIC_KEY " + mapper.writeValueAsString(new KeyMessage(username, client.getPublicKey())), writer);
                }
            } else {
                System.err.println("Invalid message provided");
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Invalid message provided");
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException |
                 JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
