package ch.tschenett.s3tool;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.NoSuchPaddingException;

import org.junit.Test;

public class AESKeyTest {
	@Test
	public void testKeyGenAndEncAndDec() {
		byte[] keyData = AESKey.createKeyData();
		AESKey key1 = new AESKey(keyData);
		byte[] encoded = key1.encode("hallo".getBytes());
		AESKey key2 = new AESKey(keyData);
		assertEquals("hallo", new String(key2.decode(encoded)));
	}
	
	@Test
	public void testKeyGenAndEncAndDecStream() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException {
		testKeyGenAndEncAndDecStream(10, 100);
		testKeyGenAndEncAndDecStream(100, 10);
		testKeyGenAndEncAndDecStream(100, 100);
		
		testKeyGenAndEncAndDecStream(1000, 10000);
		testKeyGenAndEncAndDecStream(10000, 1000);
		testKeyGenAndEncAndDecStream(10000, 10000);
	}

	private void testKeyGenAndEncAndDecStream(int plainSize, int bufSize)
			throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IOException {
		byte[] plain = new byte[plainSize];
		new Random().nextBytes(plain);
		
		byte[] keyData = AESKey.createKeyData();
		AESKey key1 = new AESKey(keyData);
		ByteArrayOutputStream encodedOut = new ByteArrayOutputStream();
		OutputStream encodingOut = key1.encodingOutputStream(encodedOut);
		encodingOut.write(plain);
		encodingOut.close();
		byte[] encoded = encodedOut.toByteArray();
		
		assertArrayEquals(key1.encode(plain), encoded);
		
		AESKey key2 = new AESKey(keyData);
		ByteArrayInputStream encodedIn = new ByteArrayInputStream(encoded);
		InputStream decodingIn = key2.decodingInputStream(encodedIn);
		ByteArrayOutputStream plainOut2 = new ByteArrayOutputStream();
		byte[] buf = new byte[bufSize];
		for (int l = decodingIn.read(buf); l != -1; l = decodingIn.read(buf)) {
			plainOut2.write(buf, 0, l);
		}
		
		assertArrayEquals(plain, plainOut2.toByteArray());
	}
	
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
		
		byte[] keyData = AESKey.createKeyData();
		
		AESKey key2 = new AESKey(keyData);
		InputStream decodingIn = key2.decodingInputStream(key2.encodingInputStream(new ByteArrayInputStream(plain)));
		ByteArrayOutputStream plainOut2 = new ByteArrayOutputStream();
		byte[] buf = new byte[bufSize];
		for (int l = decodingIn.read(buf); l != -1; l = decodingIn.read(buf)) {
			plainOut2.write(buf, 0, l);
		}
		
		assertArrayEquals(plain, plainOut2.toByteArray());
	}
}
