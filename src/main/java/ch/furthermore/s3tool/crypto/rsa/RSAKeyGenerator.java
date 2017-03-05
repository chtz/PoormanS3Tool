package ch.furthermore.s3tool.crypto.rsa;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class RSAKeyGenerator {
	private static final int RSA_KEY_SIZE = 4096;
	private static final String SHA1PRNG = "SHA1PRNG";
	private static final String RSA = "RSA";
	
	public static RsaKeyPair createKeyPair() {
		try {
			KeyPairGenerator keyPairGenerator = keyPairGenerator(RSA);
			keyPairGenerator.initialize(RSA_KEY_SIZE, secureRandom(SHA1PRNG));

			KeyPair generatedKeyPair = keyPairGenerator.generateKeyPair();
	
			return new RsaKeyPair(generatedKeyPair.getPrivate().getEncoded(), generatedKeyPair.getPublic().getEncoded());
		}
		catch (Exception e) {
            throw new RuntimeException("crypto: cannot create key", e);
        }
	}
	
	private static KeyPairGenerator keyPairGenerator(String keyAlgorithm) throws NoSuchAlgorithmException {
		return KeyPairGenerator.getInstance(keyAlgorithm);
	}
	
	private static SecureRandom secureRandom(String secRandomAlgorithm) throws NoSuchAlgorithmException {
		return SecureRandom.getInstance(secRandomAlgorithm);
	}
}
