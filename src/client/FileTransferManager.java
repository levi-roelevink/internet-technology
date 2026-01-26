package client;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FileTransferManager {
    // TODO: Key is "<sender_username>|<recipient_username>" to allow for multiple pending requests for each user
    private final Map<String, FileSendData> pendingFileSendRequests = new HashMap<>();
    private final Map<String, FileReceiveData> pendingFileReceiveRequests = new HashMap<>();

    public void addPendingFileSendRequest(String username, String id, File file) {
        FileSendData data = new FileSendData(id, file);
        pendingFileSendRequests.put(username.toLowerCase(), data);
    }

//    public FileSendData removePendingFileSendRequest(String senderName, String recipientName) {
//        String key = combineUsernames(senderName, recipientName);
//        return pendingFileSendRequests.remove(key);
//    }
//
//    public FileSendData getPendingFileSendRequest(String senderName, String recipientName) {
//        String key = combineUsernames(senderName, recipientName);
//        return pendingFileSendRequests.get(key);
//    }
}
