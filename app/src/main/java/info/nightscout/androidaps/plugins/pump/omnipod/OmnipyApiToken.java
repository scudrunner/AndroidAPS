package info.nightscout.androidaps.plugins.pump.omnipod;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

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
