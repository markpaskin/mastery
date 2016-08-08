package us.paskin.mastery;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class FingerprintUnitTest {
    @Test
    public void forString_works() throws Exception {
        assertNotEquals(Fingerprint.forString("a"), Fingerprint.forString("b"));
    }
}