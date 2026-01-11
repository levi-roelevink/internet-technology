package server;

import java.io.PrintWriter;
import java.net.Socket;

public class PingInfo {
    private final int PING_DELAY_MS = 10_000;
    private boolean awaitingPong = false;
    private boolean connectionAlive = true;

    PingInfo() {}

    protected void killConnection() {
        connectionAlive = false;
    }

    protected boolean isConnectionAlive() {
        return connectionAlive;
    }

    protected void setAwaitingPong(boolean awaiting) {
        awaitingPong = awaiting;
    }

    public int getPingDelayMs() {
        return PING_DELAY_MS;
    }

    public boolean isAwaitingPong() {
        return awaitingPong;
    }
}
