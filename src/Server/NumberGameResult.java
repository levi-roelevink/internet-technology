package Server;

public record NumberGameResult(String username, int ms) {
    @Override
    public String toString() {
        return username + " (" + ms + "ms)";
    }
}
