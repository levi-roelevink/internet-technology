package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import static shared.utils.Utils.usernameIsValid;

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
                        default -> System.out.println("Unknown command...");
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
                    UsernameMessage message = mapper.readValue(lineParts[1], UsernameMessage.class);

                    // TODO: check for duplicate username
                    if (!usernameIsValid(message.username())) {
                        String jsonString = mapper.writeValueAsString(new LoginResponseMessage("ERROR", 5001));
                        writer.println("LOGIN_RESP " + jsonString);
                    } else {
                        username = message.username();
                        server.addUser(username, writer);
                        loggedIn = true;

                        String jsonString = mapper.writeValueAsString(new LoginResponseMessage("OK"));
                        writer.println("LOGIN_RESP " + jsonString);
                        break;
                    }
                }
            }
        }

        assert usernameIsValid(username) : "Username set is invalid";
    }

    private void informOthersOfJoin() throws JsonProcessingException {
        try {
            HashMap<String, PrintWriter> users = server.getUsers();

            String jsonString = mapper.writeValueAsString(new UsernameMessage(username));

            users.forEach((name, writer) -> {
                if (!name.equals(username)) {
                    writer.println("JOINED " + jsonString);
                }
            });
        } catch (JsonProcessingException e) {
            System.err.println(e.getMessage());
        }
    }

    private void welcomeClient() throws JsonProcessingException {
        try {
            WelcomeMessage message = new WelcomeMessage("Welcome to the server.");
            String jsonString = mapper.writeValueAsString(message);

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
