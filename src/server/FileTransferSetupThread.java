package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class FileTransferSetupThread extends Thread {
    private final Map<String, OutputStream> fileReaders = new HashMap<>();
    private final Map<String, InputStream> fileWriters = new HashMap<>();
    private final ServerSocket serverSocket;

    public FileTransferSetupThread(int port) {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void run() {
        try {
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
