package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

import static Messages.MessageSender.sendLine;

public class PingThread extends Thread {
    private final Socket socket;
    private final PrintWriter writer;
    private final PingInfo pingInfo;

    public PingThread(Socket socket, PrintWriter writer, PingInfo pingInfo) {
        this.socket = socket;
        this.writer = writer;
        this.pingInfo = pingInfo;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(10000);
            while (pingInfo.isConnected()) {
                pingInfo.startPing();
                sendLine("PING", writer);
                Thread.sleep(3000);
                if (pingInfo.isAwaitingPing()) {
                    pingInfo.disconnect();
                    sendLine("DSCN {\"code\":\"3000\"", writer);
                } else {
                    Thread.sleep(7000);
                }
            }
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
