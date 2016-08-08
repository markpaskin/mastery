package us.paskin.mastery;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Computes very-likely unique long IDs for different native types.
 */
public class Fingerprint {
    private static final MessageDigest MESSAGE_DIGEST;
    private static final int LONG_BYTES = 8;
    private static ByteBuffer longBuffer = ByteBuffer.allocate(LONG_BYTES);

    static {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException err) {
            throw new IllegalStateException();
        }
        MESSAGE_DIGEST = md;
    }

    private static byte[] md5(String s) {
        return MESSAGE_DIGEST.digest(s.getBytes());
    }

    public static synchronized long forString(String s) {
        byte[] bytes = md5(s);
        longBuffer.put(bytes, 0, LONG_BYTES);
        long result = longBuffer.getLong(0);
        longBuffer.clear();
        return result;
    }
}
