package Server;

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

    @Override
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

    public void addReader(String id, OutputStream outputStream) {
        fileReaders.put(id, outputStream);
    }

    public OutputStream removeReader(String id) {
        return fileReaders.remove(id);
    }

    public boolean containsReader(String id) {
        return fileReaders.containsKey(id);
    }

    public void addWriter(String id, InputStream inputStream) {
        fileWriters.put(id, inputStream);
    }

    public InputStream removeWriter(String id) {
        return fileWriters.remove(id);
    }

    public boolean containsWriter(String id) {
        return fileWriters.containsKey(id);
    }
}