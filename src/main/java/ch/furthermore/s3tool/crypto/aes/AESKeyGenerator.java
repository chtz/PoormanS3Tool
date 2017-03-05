package ch.furthermore.s3tool.crypto.aes;

import javax.crypto.KeyGenerator;

public class AESKeyGenerator {
    private static final String AES = "AES";
    
    public static byte[] createKeyData() {
		try {
    		return KeyGenerator.getInstance(AES).generateKey().getEncoded();
    	}
        catch (Exception e) {
            throw new RuntimeException("crypto: cannot create key", e);
        }
    }
}
