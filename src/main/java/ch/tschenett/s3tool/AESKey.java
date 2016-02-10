package ch.tschenett.s3tool;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class AESKey {
    private static final String AES = "AES";
    
	private SecretKeySpec key;
    
    public AESKey(byte[] keyData) {
    	try {
			key = new SecretKeySpec(keyData, AES);
		}
        catch (Exception e) {
            throw new RuntimeException("crypto: invalid keyData", e);
        }
    }
    
    public static byte[] createKeyData() {
		try {
    		return KeyGenerator.getInstance(AES).generateKey().getEncoded();
    	}
        catch (Exception e) {
            throw new RuntimeException("crypto: cannot create key", e);
        }
    }
    
    public OutputStream encodingOutputStream(OutputStream target) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
    	return new CipherOutputStream(encodingCipher(), target);
    }
    
    public InputStream encodingInputStream(InputStream target) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
    	return new CipherInputStream(encodingCipher(), target);
    }
    
    public byte[] encode(byte[] plain) {
    	try {
	        return encodingCipher().doFinal(plain);
    	}
    	catch (Exception e) {
    		throw new RuntimeException("crypto: encode failed", e);
    	}
    }

	private Cipher encodingCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		Cipher cipher = Cipher.getInstance(AES);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher;
	}
    
	public InputStream decodingInputStream(InputStream target) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		return new CipherInputStream(decodingCipher(), target);
	}
	
	public OutputStream decodingOutputStream(OutputStream target) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException {
		return new CipherOutputStream(decodingCipher(), target);
	}
	
    public byte[] decode(byte[] encoded) {
    	try {
	        return decodingCipher().doFinal(encoded);
    	}
    	catch (Exception e) {
    		throw new RuntimeException("crypto: decode failed", e);
    	}
    }

	private Cipher decodingCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		Cipher cipher2 = Cipher.getInstance(AES);
		cipher2.init(Cipher.DECRYPT_MODE, key);
		return cipher2;
	}
}
