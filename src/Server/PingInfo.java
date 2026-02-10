package Server;

public class PingInfo {
    private boolean awaitingPing = false;
    private boolean connected = true;

    public void startPing() {
        awaitingPing = true;
    }

    public void stopPing() {
        awaitingPing = false;
    }

    public void disconnect() {
        connected = false;
    }

    public boolean isAwaitingPing() {
        return awaitingPing;
    }

    public boolean isConnected() {
        return connected;
    }
}
