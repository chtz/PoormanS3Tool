package ch.furthermore.s3tool.crypto.rsa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;

import ch.furthermore.s3tool.crypto.aes.AESKeyGenerator;

public class RSATest {
	@Test
	public void test() {
		KeyPair keyPair = RSAKeyGenerator.createKeyPair();
		RSA publicKey = new RSA(null, keyPair.getPublic().getEncoded());
		RSA privateKey = new RSA(keyPair.getPrivate().getEncoded(), null);
		
		byte[] data = AESKeyGenerator.createKeyData();
		
		byte[] signature = privateKey.sign(new ByteArrayInputStream(data));
		byte[] encrypted = publicKey.encrypt(data);
		
		assertTrue(publicKey.verify(signature, new ByteArrayInputStream(data)));
		assertFalse(publicKey.verify(signature, new ByteArrayInputStream(encrypted)));
		
		byte[] decrypted = privateKey.decrypt(encrypted);
		
		assertEquals(Base64.encodeBase64String(data), Base64.encodeBase64String(decrypted));
	}
}
