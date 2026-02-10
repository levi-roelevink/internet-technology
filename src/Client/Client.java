package Client;

import Client.FileTransferManaging.FileTransferManager;
import Messages.GenericMessage;
import Messages.MessageCodePrinter;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static Messages.MessageSender.sendLine;

public class Client {
    private final PrintWriter writer;
    private final BufferedReader reader;
    private final ObjectMapper mapper;
    private final KeyPairGeneration keyPairGeneration;
    private final FileTransferManager fileTransferManager;
    private final Map<String, SecretKey> sessionKeys;
    private final Map<String, String> storedEncryptedMessages;

    public Client() throws IOException, NoSuchAlgorithmException {
        keyPairGeneration = new KeyPairGeneration();
        Socket socket = new Socket("127.0.0.1", 1337);
        InputStream inputStream = socket.getInputStream();
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
        OutputStream outputStream = socket.getOutputStream();
        this.writer = new PrintWriter(outputStream);
        this.mapper = new ObjectMapper();
        this.fileTransferManager = new FileTransferManager();
        this.sessionKeys = new HashMap<>();
        this.storedEncryptedMessages = new HashMap<>();
    }

    public static void main(String[] args) {
        try {
            new Client().run();
        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println(e.getMessage());
            System.exit(0);
        }
    }

    public void run() throws IOException, NoSuchAlgorithmException {
        awaitWelcomeMessage();

        logIn();

        ServerInputThread serverInputThread = new ServerInputThread(reader, writer, mapper, fileTransferManager, this);
        ClientInputThread clientInputThread = new ClientInputThread(writer, mapper, fileTransferManager, this);
        serverInputThread.start();
        clientInputThread.start();
        //keyPairGeneration = new KeyPairGeneration();
    }

    private void awaitWelcomeMessage() throws IOException {
        while (true) {
            String line = reader.readLine();
            String[] lineParts = line.split(" ", 2);
            if (lineParts[0].equals("WELCOME")) {
                GenericMessage message = mapper.readValue(lineParts[1], GenericMessage.class);
                System.out.println(message.message());
                return;
            }
        }
    }

    private void logIn() throws IOException {
        boolean loggedInSuccessfully = false;
        while (!loggedInSuccessfully) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Please enter your username");
            String name = scanner.nextLine();
            sendLine("LOGIN {\"username\":\"" + name + "\"}", writer);

            loggedInSuccessfully = awaitLoginResponse();
        }
    }

    private boolean awaitLoginResponse() throws IOException {
        while (true) {
            String line = reader.readLine();
            if (line.startsWith("LOGIN_RESP ")) {
                GenericMessage message = mapper.readValue(line.replaceFirst("LOGIN_RESP ", ""), GenericMessage.class);
                if (message.status().equals("OK")) {
                    System.out.println("Logged in successfully.");
                    System.out.println("Use the command help to see available commands.");
                    return true;
                } else {
                    MessageCodePrinter.printMessageFromCode(message.code());
                    return false;
                }
            }
        }
    }

    public byte[] getPublicKey() {return keyPairGeneration.getPublicKey();}

    public byte[] getPrivateKey() {return keyPairGeneration.getPrivateKey();}

    public void addSessionKey(String username, SecretKey secretKey) {
        sessionKeys.put(username.toLowerCase(), secretKey);
    }

    public SecretKey getSessionKey(String username) {
        return sessionKeys.get(username.toLowerCase());
    }

    public void removeSessionKey(String username) {
        sessionKeys.remove(username.toLowerCase());
    }

    public boolean containsSessionKey(String username) {
        return sessionKeys.containsKey(username.toLowerCase());
    }

    public void addStoredEncryptedMessage(String username, String message) {
        storedEncryptedMessages.put(username.toLowerCase(), message);
    }

    public String getStoredEncryptedMessage(String username) {
        return storedEncryptedMessages.get(username.toLowerCase());
    }

    public void removeStoredEncryptedMessage(String username) {
        storedEncryptedMessages.remove(username.toLowerCase());
    }
}
