package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private PingInfo pingInfo;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            mapper = new ObjectMapper();
            pingInfo = new PingInfo(socket, writer);

            welcomeClient();
            awaitLogin();
            System.out.println("Logged in " + username);

            PingThread pingThread = new PingThread(pingInfo);
            pingThread.start();

            String inputLine;

            while (pingInfo.isConnectionAlive()) {
                while ((inputLine = reader.readLine()) != null) {
                    System.out.printf("Received \"%s\"\n", inputLine);
                    String[] lineParts = inputLine.split(" ", 2);

                    switch (lineParts[0]) {
                        case PONG -> handlePong();
                        default -> System.out.println("Unknown command...");
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

    private void handlePong() {
        pingInfo.receivedPong();
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

    private class PingThread extends Thread {
        private final PingInfo pingInfo;

        PingThread(PingInfo pingInfo) {
            this.pingInfo = pingInfo;
        }

        public void run() {
            while (true) {
                try {
                    System.out.println("Going to sleep for 10s");
                    sleep(pingInfo.getPingDelayMs());

                    if (pingInfo.isAwaitingPong()) {
                        // No PONG response within 10_000 MS, hence the client has lost connection
                        pingInfo.killConnection();
                    }
                    System.out.println("Sending PING");
                    pingInfo.ping();
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }
}
