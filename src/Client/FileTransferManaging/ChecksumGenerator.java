package Client.FileTransferManaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public interface ChecksumGenerator {
    static String generateChecksum(File file) throws IOException {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];

            //Read bytes from the fileInputStream until none remain
            //When none remain the amount of bytes read will be -1, which means the loop should be stopped
            int n;
            while ((n = fileInputStream.read(buffer)) != -1) {
                messageDigest.update(buffer, 0, n);
            }
            byte[] digest = messageDigest.digest();

            //Convert the digest to a hexadecimal string
            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : digest) {
                stringBuilder.append(String.format("%02x", b & 0xff));
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
