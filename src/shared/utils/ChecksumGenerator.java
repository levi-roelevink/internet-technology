package shared.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public interface ChecksumGenerator {

    static String getFileChecksum(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("MD5");

        // Read file in chunks
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int bytesRead = 0;

        while ((bytesRead = fis.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }

        byte[] bytes = digest.digest();

        // Convert bytes to Hex String
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            // "%02x"converts byte to 2-digit hex
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }
}
