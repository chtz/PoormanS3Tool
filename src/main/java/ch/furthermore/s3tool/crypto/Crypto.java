package ch.furthermore.s3tool.crypto;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.crypto.aes.AES;
import ch.furthermore.s3tool.crypto.aes.AESKeyGenerator;
import ch.furthermore.s3tool.crypto.rsa.RSA;
import ch.furthermore.s3tool.crypto.rsa.RSAKeyGenerator;

@Service
public class Crypto {
	public KeyPair genKeyPair() {
		java.security.KeyPair keyPair = RSAKeyGenerator.createKeyPair();
		
		return new KeyPair(Base64.encodeBase64String(keyPair.getPublic().getEncoded()), Base64.encodeBase64String(keyPair.getPrivate().getEncoded()));
	}
	
	public String encodeKey(String publicKeyBase64, String aesKeyBase64) {
		RSA rsa = new RSA(null, Base64.decodeBase64(publicKeyBase64));
		byte[] encoded = rsa.encrypt(Base64.decodeBase64(aesKeyBase64));
		return Base64.encodeBase64String(encoded);
	}
	
	public String decodeKey(String privateKeyBase64, String encodedAesKeyBase64) {
		RSA rsa = new RSA(Base64.decodeBase64(privateKeyBase64), null);
		byte[] decoded = rsa.decrypt(Base64.decodeBase64(encodedAesKeyBase64));
		return Base64.encodeBase64String(decoded);
	}
	
	public String sign(String privateKeyBase64, File file) throws IOException {
		RSA rsa = new RSA(Base64.decodeBase64(privateKeyBase64), null);
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
		try {
			byte[] signature = rsa.sign(in);
			return Base64.encodeBase64String(signature);
		}
		finally {
			in.close();
		}
	}
	
	public boolean verify(String publicKeyBase64, String signatureBase64, File file) throws IOException {
		RSA rsa = new RSA(null, Base64.decodeBase64(publicKeyBase64));
		byte[] signature = Base64.decodeBase64(signatureBase64);
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
		try {
			return rsa.verify(signature, in);
		}
		finally {
			in.close();
		}
	}
	
	public String genKey() {
		return Base64.encodeBase64String(AESKeyGenerator.createKeyData());
	}
	
	private AES aesKey(String aesKeyBase64) {
		return new AES(Base64.decodeBase64(aesKeyBase64));
	}
	
	public void decodeToFileClosing(String aesKeyBase64, InputStream in, File file) throws IOException {
		copyToFileClosing(aesKey(aesKeyBase64).decodingInputStream(in), file);
	}

	public void encodeFile(String aesKeyBase64, File plainFile, File encodedFile) throws IOException {
		encodeToFileClosing(aesKeyBase64, new BufferedInputStream(new FileInputStream(plainFile)), encodedFile);
	}
	
	private void encodeToFileClosing(String aesKeyBase64, InputStream in, File encodedFile) throws IOException {
		copyToFileClosing(aesKey(aesKeyBase64).encodingInputStream(in), encodedFile);
	}

	private void copyToFileClosing(InputStream in, File file) throws IOException {
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			try {
				copyToOutputStream(in, out);
			}
			finally {
				out.close();
			}
		}
		finally {
			in.close();
		}
	}
	
	private void copyToOutputStream(InputStream in, OutputStream out) throws IOException {
		final byte[] buf = new byte[4096];
		for (int l = in.read(buf); l != -1; l = in.read(buf)) {
			out.write(buf, 0, l);
		}
	}
}
