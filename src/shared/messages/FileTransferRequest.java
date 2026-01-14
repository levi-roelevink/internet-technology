package shared.messages;

import client.File;

public record FileTransferRequest(String username, File file) {
}
