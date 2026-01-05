package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import messages.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

public class Client {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner scanner;
    private ObjectMapper objectMapper;
    private static final int PORT = 1337;
    private static final String IP = "127.0.0.1";

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.run();
    }

    public void startConnection(String ip, int port) throws IOException {
        clientSocket = new Socket(ip, port);
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }

    public String sendMessage(String msg) throws IOException {
        out.println(msg);
        return in.readLine();
    }

    public void stopConnection() throws IOException {
        in.close();
        out.close();
        clientSocket.close();
    }

    public void login() throws IOException {
        String inputLine = in.readLine();
        String[] splits = inputLine.split(" ", 2);
        assert (Objects.equals(splits[0], "WELCOME")) : "Unexpected opening message from the server: \"" + splits[0] + "\"";

        Message message = parseMessage(splits[1]);
        System.out.println(message);

        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        // TODO: what if the name is invalid?

    }

    private Message parseMessage(String jsonString) throws JsonProcessingException {
        return objectMapper.readValue(jsonString, Message.class);
    }

    public void run() throws IOException {
        scanner = new Scanner(System.in);
        objectMapper = new ObjectMapper();
        startConnection(IP, PORT);

        login();

        while (true) {
            System.out.print("Enter a message: ");

            String input = scanner.nextLine();
            out.println(input);

            String response = in.readLine();
            System.out.println("SERVER: " + response);
            if ("Goodbye".equals(response)) {
                break;
            }
        }

        stopConnection();
    }
}
