package Client.FileTransferManaging;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileTransferManager {
    private final Map<String, FileSendData> pendingFileSendRequests = new HashMap<>();
    private final Map<String, FileReceiveData> pendingFileReceiveRequests = new HashMap<>();

    public void addPendingFileSendRequest(String username, String id, File file) {
        pendingFileSendRequests.put(username.toLowerCase(), new FileSendData(id, file));
    }

    public void removePendingFileSendRequest(String username) {
        pendingFileSendRequests.remove(username.toLowerCase());
    }

    public FileSendData getFileSendData(String username) {
        return pendingFileSendRequests.get(username.toLowerCase());
    }

    public void addPendingFileReceiveRequest(String username, String id, String checksum, String filename) {
        pendingFileReceiveRequests.put(username.toLowerCase(), new FileReceiveData(id, checksum, filename));
    }

    public void removePendingFileReceiveRequest(String username) {
        pendingFileReceiveRequests.remove(username.toLowerCase());
    }

    public FileReceiveData getFileReceiveData(String username) {
        return pendingFileReceiveRequests.get(username.toLowerCase());
    }

    public boolean receiveRequestExists(String username) {
        return pendingFileReceiveRequests.containsKey(username.toLowerCase());
    }
}
