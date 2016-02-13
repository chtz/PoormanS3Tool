package ch.tschenett.s3tool;

import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class AESKey {
    private static final String AES = "AES";
    
	private final SecretKeySpec key;
    
    public AESKey(byte[] keyData) {
    	try {
			key = new SecretKeySpec(keyData, AES);
		}
        catch (Exception e) {
            throw new RuntimeException("crypto: invalid key", e);
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
    
    public InputStream encodingInputStream(InputStream target) {
    	try {
			return new CipherInputStream(encodingCipher(), target);
		} catch (InvalidKeyException e) {
			throw new RuntimeException("crypto: invalid key", e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("crypto: no such algorithm", e);
		} catch (NoSuchPaddingException e) {
			throw new RuntimeException("crypto: no such padding", e);
		}
    }
    
	public InputStream decodingInputStream(InputStream target) {
		try {
			return new CipherInputStream(decodingCipher(), target);
		} catch (InvalidKeyException e) {
			throw new RuntimeException("crypto: invalid key", e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("crypto: no such algorithm", e);
		} catch (NoSuchPaddingException e) {
			throw new RuntimeException("crypto: no such padding", e);
		}
	}
	
	private Cipher encodingCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		Cipher cipher = Cipher.getInstance(AES);
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher;
	}
	
	private Cipher decodingCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		Cipher cipher2 = Cipher.getInstance(AES);
		cipher2.init(Cipher.DECRYPT_MODE, key);
		return cipher2;
	}
}
