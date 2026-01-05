package client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import messages.LoginMessage;
import messages.LoginResponseMessage;
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
    private String username;

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
        assert (Objects.equals(splits[0], "WELCOME")) : "Error connecting to the server";

        Message message = parseMessage(splits[1]);
        System.out.println(message);

        System.out.print("Enter your name: ");
        String userInput = scanner.nextLine();
        // TODO: what if the name is invalid?

        LoginMessage loginMsg = new LoginMessage(userInput);
        // C -> S: LOGON {"username":"<username>"}
        out.println("LOGIN " + objectMapper.writeValueAsString(loginMsg));

        inputLine = in.readLine();
        splits = inputLine.split(" ", 2);
        assert (Objects.equals(splits[0], "LOGIN_RESP")) : "Error logging in";

        LoginResponseMessage loginResp = objectMapper.readValue(splits[1], LoginResponseMessage.class);
        // S -> C: LOGON_RESP {"status":"OK"}
        if (!Objects.equals(loginResp.status(), "OK")) {
            throw new RuntimeException("Error logging in");
        }

        // Set username after successful login
        username = userInput;
        assert username != null : "Username is still null after logging in";
        System.out.printf("Successfully logged in. Welcome %s!", username);
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
