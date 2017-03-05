package ch.furthermore.s3tool.crypto.rsa;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSA {
	private static final String SHA512WITH_RSA = "SHA512withRSA";
	private static final String RSA = "RSA";
	private static final String RSA_ECB_PKCS1PADDING = "RSA/ECB/PKCS1Padding";
	
	private final PrivateKey privateKey;
	private final PublicKey publicKey;
	
	public RSA(byte[] privateKey, byte[] publicKey) {
		try {
			if (privateKey != null) {
				PKCS8EncodedKeySpec secretKeySpec =  new PKCS8EncodedKeySpec(privateKey);
				this.privateKey = keyFactory().generatePrivate(secretKeySpec);
			}
			else {
				this.privateKey = null;
			}
			
			if (publicKey != null) {
				X509EncodedKeySpec publicKeySpec =  new X509EncodedKeySpec(publicKey);
				this.publicKey = keyFactory().generatePublic(publicKeySpec);
			}
			else {
				this.publicKey = null;
			}
		}
		catch (Exception e) {
            throw new RuntimeException("crypto: invalid key", e);
        }
	}
	
	private KeyFactory keyFactory() throws NoSuchAlgorithmException {
		return KeyFactory.getInstance(RSA);
	}
	
	public byte[] sign(InputStream in) {
		try {
			return sign(SHA512WITH_RSA, in);
		}
		catch (Exception e) {
            throw new RuntimeException("crypto: signature creation failed", e);
        }
	}
	
	private byte[] sign(String signatureAlgorithm, InputStream in) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, SignatureException, IOException {
		Signature signatureGenerator = signature(signatureAlgorithm);
		signatureGenerator.initSign(privateKey);
		
		byte[] buf = new byte[4096];
		for (int l = in.read(buf); l > 0; l = in.read(buf)) {
			signatureGenerator.update(buf, 0, l);
		}
		
		return signatureGenerator.sign();
	}

	private Signature signature(String signatureAlgorithm) throws NoSuchAlgorithmException {
		return Signature.getInstance(signatureAlgorithm);
	}
	
	public boolean verify(byte[] signature, InputStream in) {
		try {
			return verify(SHA512WITH_RSA, signature, in);
		}
		catch (Exception e) {
            throw new RuntimeException("crypto: signature verification failed", e);
        }
	}
	
	private boolean verify(String signatureAlgorithm, byte[] signature, InputStream in) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, SignatureException, IOException {
		Signature signatureVerifier = signature(signatureAlgorithm);
		signatureVerifier.initVerify(publicKey);
		
		byte[] buf = new byte[4096];
		for (int l = in.read(buf); l > 0; l = in.read(buf)) {
			signatureVerifier.update(buf, 0, l);
		}
		
		return signatureVerifier.verify(signature);
	}
	
	public byte[] encrypt(byte[] data) {
		try {
			return encrypt(RSA_ECB_PKCS1PADDING, data);
		}
		catch (Exception e) {
            throw new RuntimeException("crypto: encryption failed", e);
        }
	}
	
	private byte[] encrypt(String cipherTransformation, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException {
		Cipher encryptCipher = cipher(cipherTransformation);
		encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return encryptCipher.doFinal(data);
	}

	public byte[] decrypt(byte[] data) {
		try {
			return decrypt(RSA_ECB_PKCS1PADDING, data);
		}
		catch (Exception e) {
            throw new RuntimeException("crypto: decryption failed", e);
        }
	}
	
	private byte[] decrypt(String cipherTransformation, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException {
		Cipher decryptCipher = cipher(cipherTransformation);
		decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
		return decryptCipher.doFinal(data);
	}
	
	private Cipher cipher(String cipherTransformation) throws NoSuchAlgorithmException, NoSuchPaddingException {
		return Cipher.getInstance(cipherTransformation);
	}
}
