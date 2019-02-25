package info.nightscout.androidaps.plugins.PumpOmnipod;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class OmnipyApiToken {

    private byte[] _token;
    private OmnipyApiSecret _secret;
    private byte[] _iv;
    private byte[] _auth;

    public OmnipyApiToken(byte[] token, OmnipyApiSecret secret) {
        _token = token;
        _secret = secret;
        try {
            Cipher aes = Cipher.getInstance("AES/CBC/NoPadding");
            aes.init(Cipher.ENCRYPT_MODE, _secret.getKey());
            _iv = aes.getIV();
            _auth = aes.update(_token);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public byte[] getAuthenticationToken() {
        return _auth;
    }

    public byte[] getIV() {
        return _iv;
    }
}
