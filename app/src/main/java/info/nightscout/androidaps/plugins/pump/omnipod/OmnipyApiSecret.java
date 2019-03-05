package info.nightscout.androidaps.plugins.pump.omnipod;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.spec.SecretKeySpec;

public class OmnipyApiSecret {

    private byte[] _key;

    private OmnipyApiSecret(byte[] key){
        _key = key;
    }

    public static OmnipyApiSecret fromPassphrase(String passphrase) {
        try {
            byte[] passphraseBytes = passphrase.getBytes(StandardCharsets.UTF_8);
            byte[] passphraseSalt = "bythepowerofgrayskull".getBytes(StandardCharsets.UTF_8);
            byte[] input = new byte[passphraseBytes.length + passphraseSalt.length];
            System.arraycopy(passphraseBytes, 0, input, 0, passphraseBytes.length);
            System.arraycopy(passphraseSalt, 0, input, passphraseBytes.length,
                    passphraseSalt.length);
            byte[] key = MessageDigest.getInstance("SHA-256").digest(input);
            return new OmnipyApiSecret(key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public SecretKeySpec getKey()
    {
        return new SecretKeySpec(_key, "AES");
    }
}
