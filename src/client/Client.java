package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private Scanner scanner;
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

    public void login() {
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();

        // TODO: what if the name is invalid?

        System.out.printf("Name provided %s\n", name);
        // TODO: how do we specifically inform the server that this is the name we want to login with?

        String message = "LOGIN " + name;
        out.println(message);
    }

    public void awaitWelcome() throws IOException {
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            String[] splits = inputLine.split(" ", 2);

            String command = splits[0];
            System.out.println("Command: " + command);

            // TODO: deserialize JSON
            String message = splits[1];
            System.out.println("Message: " + message);
        }
    }

//    private String[] parseMessage() {
//
//    }

    public void run() throws IOException {
        scanner = new Scanner(System.in);
        startConnection(IP, PORT);

        awaitWelcome();

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
