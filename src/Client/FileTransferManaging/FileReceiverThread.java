package Client.FileTransferManaging;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static Client.FileTransferManaging.ChecksumGenerator.generateChecksum;

public class FileReceiverThread extends Thread {
    private final FileTransferManager fileTransferManager;
    private final String sender;

    public FileReceiverThread(FileTransferManager fileTransferManager, String sender) {
        this.fileTransferManager = fileTransferManager;
        this.sender = sender;
    }

    @Override
    public void run() {
        try {
            FileReceiveData data = fileTransferManager.getFileReceiveData(sender);
            fileTransferManager.removePendingFileReceiveRequest(sender);
            FileOutputStream fileOutputStream = new FileOutputStream(data.filename());
            Socket socket = new Socket("127.0.0.1", 1338);
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();

            outputStream.write(("r" + data.id()).getBytes(StandardCharsets.UTF_8));
            outputStream.flush();

            System.out.println("Started receiving file.");
            inputStream.transferTo(fileOutputStream);

            fileOutputStream.close();
            socket.close();

            System.out.println("File received, checking checksum.");
            File file = new File(data.filename());
            String checksum = generateChecksum(file);
            if (!checksum.equals(data.checksum())) {
                file.delete();
                System.err.println("File checksums do not match, deleting file.");
            } else {
                System.out.println("Checksums match, file successfully received.");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
