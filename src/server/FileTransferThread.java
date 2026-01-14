package server;

import java.net.Socket;

public class FileTransferThread extends Thread {
    private Socket socket;
    private FileTransferSetupThread setupThread;

    FileTransferThread(Socket socket, FileTransferSetupThread setupThread) {
        this.socket = socket;
        this.setupThread = setupThread;
    }

    public void run() {

    }
}
