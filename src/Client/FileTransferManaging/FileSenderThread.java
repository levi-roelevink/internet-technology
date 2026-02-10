package Client.FileTransferManaging;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class FileSenderThread extends Thread {
    private final FileTransferManager fileTransferManager;
    private final String receiver;

    public FileSenderThread(FileTransferManager fileTransferManager, String receiver) {
        this.fileTransferManager = fileTransferManager;
        this.receiver = receiver;
    }

    @Override
    public void run() {
        try {
            FileSendData data = fileTransferManager.getFileSendData(receiver);
            fileTransferManager.removePendingFileSendRequest(receiver);
            FileInputStream fileInputStream = new FileInputStream(data.file());
            Socket socket = new Socket("127.0.0.1", 1338);
            OutputStream outputStream = socket.getOutputStream();

            outputStream.write(("w" + data.id()).getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            System.out.println("Started transferring file.");
            fileInputStream.transferTo(outputStream);

            fileInputStream.close();
            socket.close();
            System.out.println("Finished transferring file.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
