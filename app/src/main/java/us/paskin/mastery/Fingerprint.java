package us.paskin.mastery;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 */
public class Fingerprint {
    private static final MessageDigest MESSAGE_DIGEST;
    private static ByteBuffer longBuffer = ByteBuffer.allocate(Long.BYTES);

    static {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException err) {
            throw new IllegalStateException();
        }
        MESSAGE_DIGEST = md;
    }

    private byte[] md5(String s) {
        return MESSAGE_DIGEST.digest(s.getBytes());
    }

    public synchronized long forString(String s) {
        byte[] bytes = md5(s);
        longBuffer.put(bytes, 0, Long.BYTES);
        return longBuffer.getLong();
    }
}
