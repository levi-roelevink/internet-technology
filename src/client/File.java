package client;

public class File {
    private final String id;
    private final String name;
    private final long size;
    private final String checksum;

    File(String id, String name, long size, String checksum) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.checksum = checksum;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public String getChecksum() {
        return checksum;
    }
}
