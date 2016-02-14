package ch.tschenett.s3tool;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.NoSuchPaddingException;

import org.junit.Test;

public class AESKeyTest {
	@Test
	public void testKeyGenAndEncAndDecStream2() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException {
		testKeyGenAndEncAndDecStream2(10, 100);
		testKeyGenAndEncAndDecStream2(100, 10);
		testKeyGenAndEncAndDecStream2(100, 100);
		
		testKeyGenAndEncAndDecStream2(1000, 10000);
		testKeyGenAndEncAndDecStream2(10000, 1000);
		testKeyGenAndEncAndDecStream2(10000, 10000);
	}
	
	private void testKeyGenAndEncAndDecStream2(int plainSize, int bufSize)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException {
		byte[] plain = new byte[plainSize];
		new Random().nextBytes(plain);
		
		byte[] keyData = AES.createKeyData();
		
		AES key2 = new AES(keyData);
		InputStream decodingIn = key2.decodingInputStream(key2.encodingInputStream(new ByteArrayInputStream(plain)));
		ByteArrayOutputStream plainOut2 = new ByteArrayOutputStream();
		byte[] buf = new byte[bufSize];
		for (int l = decodingIn.read(buf); l != -1; l = decodingIn.read(buf)) {
			plainOut2.write(buf, 0, l);
		}
		
		assertArrayEquals(plain, plainOut2.toByteArray());
	}
}
