package server;

import java.io.PrintWriter;
import java.net.Socket;

public class PingInfo {
    private final Socket socket;
    private final PrintWriter writer;
    private final int PING_DELAY_MS = 10_000;
    private final String PING = "PING";
    private boolean awaitingPong = true;
    private boolean connectionAlive = true;

    PingInfo(Socket socket, PrintWriter writer) {
        this.socket = socket;
        this.writer = writer;
    }

    public void ping() {
        writer.println(PING);
        awaitingPong = true;
    }

    protected void killConnection() {
        connectionAlive = false;
    }

    protected boolean isConnectionAlive() {
        return connectionAlive;
    }

    protected void receivedPong() {
        awaitingPong = false;
    }

    public int getPingDelayMs() {
        return PING_DELAY_MS;
    }

    public boolean isAwaitingPong() {
        return awaitingPong;
    }
}
