package client;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileTransferManager {
    private final Map<String, FileSendData> pendingFileSendRequests = new HashMap<>();

    public void addPendingFileSendRequest(String username, String id, File file) {
        FileSendData data = new FileSendData(id, file);
        pendingFileSendRequests.put(username.toLowerCase(), data);
    }
}
