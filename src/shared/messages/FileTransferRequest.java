package shared.messages;

public record FileTransferRequest(String username, String fileName, long fileSize, String id, String checksum) {
}
