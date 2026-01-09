package server;

import java.io.PrintWriter;
import java.net.Socket;

public class PingInfo {
    private final Socket socket;
    private final PrintWriter writer;
    private final int PING_DELAY_MS = 10_000;
    private final String PING = "PING";
    private boolean awaitingPong = true;

    PingInfo(Socket socket, PrintWriter writer) {
        this.socket = socket;
        this.writer = writer;
    }

    public void ping() {
        writer.println(PING);
        awaitingPong = true;
    }

    protected void setAwaitingPong(boolean awaiting) {
        awaitingPong = awaiting;
    }

    public Socket getSocket() {
        return socket;
    }

    public PrintWriter getWriter() {
        return writer;
    }

    public int getPingDelayMs() {
        return PING_DELAY_MS;
    }

    public String getPing() {
        return PING;
    }

    public boolean isAwaitingPong() {
        return awaitingPong;
    }
}
