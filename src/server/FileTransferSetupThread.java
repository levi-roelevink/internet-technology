package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class FileTransferSetupThread extends Thread {
    private final HashMap<String, OutputStream> writer = new HashMap<>();
    private final HashMap<String, InputStream> reader = new HashMap<>();

    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(1338);

            while (true) {
                Socket socket = serverSocket.accept();

                new FileTransferThread(socket, this).start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: get, add, remove, contains K: id (uuid), V: the corresponding stream
}
