package Messages;

public record EncryptedPrivateMessage(String username, byte[] message) {
}
