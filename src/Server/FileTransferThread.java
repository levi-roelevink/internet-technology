package Server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class FileTransferThread extends Thread {
    private final Socket socket;
    private final FileTransferSetupThread fileTransferSetupThread;

    public FileTransferThread(Socket socket, FileTransferSetupThread fileTransferSetupThread) {
        this.socket = socket;
        this.fileTransferSetupThread = fileTransferSetupThread;
    }

    public void run() {
        try {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            byte[] clientRole = inputStream.readNBytes(1);
            String uniqueConnectionId = new String(inputStream.readNBytes(36));

            if (clientRole[0] == 'r') {
                if (fileTransferSetupThread.containsWriter(uniqueConnectionId)) {
                    InputStream writerInputStream = fileTransferSetupThread.removeWriter(uniqueConnectionId);
                    writerInputStream.transferTo(outputStream);
                    socket.close();
                    writerInputStream.close();
                } else {
                    fileTransferSetupThread.addReader(uniqueConnectionId, outputStream);
                }
            } else if (clientRole[0] == 'w') {
                if (fileTransferSetupThread.containsReader(uniqueConnectionId)) {
                    OutputStream readerOutputStream = fileTransferSetupThread.removeReader(uniqueConnectionId);
                    inputStream.transferTo(readerOutputStream);
                    socket.close();
                    readerOutputStream.close();
                } else {
                    fileTransferSetupThread.addWriter(uniqueConnectionId, inputStream);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
