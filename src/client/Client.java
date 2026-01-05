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

    public void run() throws IOException {
        startConnection("127.0.0.1", 3000);

        Scanner scanner = new Scanner(System.in);

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
