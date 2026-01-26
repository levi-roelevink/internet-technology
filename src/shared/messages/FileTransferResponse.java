package shared.messages;

public record FileTransferResponse(String status, String username, int code) {
    public FileTransferResponse(String username, int code) {
        this(null, username, 0);
    }
}
