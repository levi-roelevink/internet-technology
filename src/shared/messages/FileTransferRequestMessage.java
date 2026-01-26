package shared.messages;

public record FileTransferRequestMessage(String username, String filename, long filesize, String id, String checksum) {
}
