package server;

import client.MessageCodePrinter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.CodeMessage;
import shared.messages.LoginResponseMessage;
import shared.messages.UsernameMessage;
import shared.messages.WelcomeMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static shared.utils.Utils.usernameIsValid;

public class ServerThread extends Thread {
    private final Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private ObjectMapper mapper;
    private final String PONG = "PONG";
    private final String BYE = "BYE";
    private final String LOGIN = "LOGIN";
    private String username;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            mapper = new ObjectMapper();

            welcomeClient();
            awaitLogin();

//            PingThread pingThread = new PingThread(socket, writer);
//            pingThread.start();

            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                System.out.printf("Received \"%s\"\n", inputLine);
                String[] lineParts = inputLine.split(" ", 2);

                switch (lineParts[0]) {
//                    case PONG -> pingThread.setAwaitingPong(false);
                    case BYE -> terminateConnection();
                    default -> System.out.println("Unknown command...");
                }

                writer.println(inputLine);
            }

            reader.close();
            writer.close();
            socket.close();
        } catch (IOException e) {
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
                    UsernameMessage message = mapper.readValue(lineParts[1], UsernameMessage.class);

                    LoginResponseMessage responseMessage;

                    if (!usernameIsValid(message.username())) {
                        responseMessage = new LoginResponseMessage("ERROR", 5001);
                    } else {
                        responseMessage = new LoginResponseMessage("OK");
                        username = message.username();
                        loggedIn = true;
                    }

                    String jsonString = mapper.writeValueAsString(responseMessage);
                    writer.println("LOGIN_RESP " + jsonString);
                }
            }
        }

        assert usernameIsValid(username) : "Username set is invalid";
    }

    private void welcomeClient() throws JsonProcessingException {
        try {
            WelcomeMessage message = new WelcomeMessage("Welcome to the server.");
            String jsonString = mapper.writeValueAsString(message);

            writer.println("WELCOME " + jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void terminateConnection() {
        System.out.println("BEEP BOOP CAT POOP");
    }

    private class PingThread extends Thread {
        private final Socket socket;
        private final PrintWriter writer;
        private final int PING_DELAY_MS = 10_000;
        private final String PING = "PING";
        private boolean awaitingPong = true;

        PingThread(Socket socket, PrintWriter writer) {
            this.socket = socket;
            this.writer = writer;
        }

        public void run() {
            while (true) {
                try {
                    sleep(PING_DELAY_MS);

                    if (awaitingPong) {
                        // TODO: no PONG response within 10_000 MS, hence the client has lost connection
                        socket.close();
                        writer.close();
                        throw new Exception("No pong received.");
                    }
                    writer.println(PING);
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
        }

        protected boolean getAwaitingPong() {
            return awaitingPong;
        }

        protected void setAwaitingPong(boolean awaiting) {
            awaitingPong = awaiting;
        }
    }
}
