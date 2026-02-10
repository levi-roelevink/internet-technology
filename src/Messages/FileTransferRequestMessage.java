package Messages;

public record FileTransferRequestMessage(String username, String filename, int filesize, String id, String checksum) {
}
