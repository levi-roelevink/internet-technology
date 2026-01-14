package client;

import java.util.HashMap;
import java.util.Map;

public class FileTransferManager {
    private final Map<String, FileSendData> pendingFileSendRequests = new HashMap<>();
    private final Map<String, FileReceiveData> pendingFileReceiveRequests = new HashMap<>();

    // TODO: get, add, remove, contains
}
