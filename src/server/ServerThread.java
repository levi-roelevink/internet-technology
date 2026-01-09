package server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import shared.messages.WelcomeMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerThread extends Thread {
    private final Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    private ObjectMapper mapper;
    private final String PONG = "PONG";
    private final String BYE = "BYE";

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            mapper = new ObjectMapper();

            welcomeClient();

            PingThread pingThread = new PingThread(socket, writer);
            pingThread.start();

            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                System.out.printf("Received \"%s\"\n", inputLine);
                String[] lineParts = inputLine.split(" ", 2);

                switch (lineParts[0]) {
                    case PONG -> pingThread.setAwaitingPong(false);
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

    private void welcomeClient() throws JsonProcessingException {
        try {
            WelcomeMessage wm = new WelcomeMessage("Welcome to the server.");
            String jsonString = mapper.writeValueAsString(wm);

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
