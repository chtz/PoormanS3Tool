package ch.furthermore.s3tool.crypto;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
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
	public  static final int RSA_KEY_SIZE = 4096;
	private static final String SHA512WITH_RSA = "SHA512withRSA";
	private static final String SHA1PRNG = "SHA1PRNG";
	private static final String RSA = "RSA";
	private static final String RSA_ECB_PKCS1PADDING = "RSA/ECB/PKCS1Padding";
	
	private byte[] privateKeyData;
	private byte[] publicKeyData;
	
	private transient PrivateKey privateKey = null;
	private transient PublicKey publicKey = null;
	
	public RSA() {
		this(null, null);
	}
	
	public RSA(byte[] privateKey, byte[] publicKey) {
		this.privateKeyData = privateKey;
		this.publicKeyData = publicKey;
	}
	
	public RSA(int rsaKeySize) {
		try {
			KeyPairGenerator keyPairGenerator = keyPairGenerator(RSA);
			
			keyPairGenerator.initialize(rsaKeySize, secureRandom(SHA1PRNG));

			KeyPair generatedKeyPair = keyPairGenerator.generateKeyPair();
	
			privateKeyData = (privateKey = generatedKeyPair.getPrivate()).getEncoded();
			publicKeyData = (publicKey = generatedKeyPair.getPublic()).getEncoded();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private SecureRandom secureRandom(String secRandomAlgorithm) throws NoSuchAlgorithmException {
		return SecureRandom.getInstance(secRandomAlgorithm);
	}

	private KeyPairGenerator keyPairGenerator(String keyAlgorithm) throws NoSuchAlgorithmException {
		return KeyPairGenerator.getInstance(keyAlgorithm);
	}
	
	private PrivateKey privateKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
		if (privateKey != null) return privateKey;
		
		PKCS8EncodedKeySpec secretKeySpec =  new PKCS8EncodedKeySpec(privateKeyData);
		
		return privateKey = keyFactory().generatePrivate(secretKeySpec);
	}

	private KeyFactory keyFactory() throws NoSuchAlgorithmException {
		return KeyFactory.getInstance(RSA);
	}
	
	private PublicKey publicKey() throws InvalidKeySpecException, NoSuchAlgorithmException {
		if (publicKey != null) return publicKey;
		
		X509EncodedKeySpec publicKeySpec =  new X509EncodedKeySpec(publicKeyData);
		
		return publicKey = keyFactory().generatePublic(publicKeySpec);
	}
	
	public Signed signed(byte[] data) {
		Signed signed = new Signed();
		signed.setData(data);
		signed.setSignature(sign(data));
		return signed;
	}
	
	public byte[] sign(byte[] data) {
		try {
			return sign(SHA512WITH_RSA, data);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private byte[] sign(String signatureAlgorithm, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, SignatureException {
		Signature signatureGenerator = signature(signatureAlgorithm);
		signatureGenerator.initSign(privateKey());
		signatureGenerator.update(data);
		return signatureGenerator.sign();
	}

	private Signature signature(String signatureAlgorithm) throws NoSuchAlgorithmException {
		return Signature.getInstance(signatureAlgorithm);
	}
	
	public boolean verify(Signed signed) {
		return verify(signed.getSignature(), signed.getData());
	}
	
	public boolean verify(byte[] signature, byte[] data) {
		try {
			return verify(SHA512WITH_RSA, signature, data);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private boolean verify(String signatureAlgorithm, byte[] signature, byte[] data) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, SignatureException {
		Signature signatureVerifier = signature(signatureAlgorithm);
		signatureVerifier.initVerify(publicKey());
		signatureVerifier.update(data);
		return signatureVerifier.verify(signature);
	}
	
	public byte[] encrypt(byte[] data) {
		try {
			return encrypt(RSA_ECB_PKCS1PADDING, data);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private byte[] encrypt(String cipherTransformation, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException {
		Cipher encryptCipher = cipher(cipherTransformation);
		encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey());
		return encryptCipher.doFinal(data);
	}

	private Cipher cipher(String cipherTransformation) throws NoSuchAlgorithmException, NoSuchPaddingException {
		return Cipher.getInstance(cipherTransformation);
	}
	
	public byte[] decrypt(byte[] data) {
		try {
			return decrypt(RSA_ECB_PKCS1PADDING, data);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private byte[] decrypt(String cipherTransformation, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException {
		Cipher decryptCipher = cipher(cipherTransformation);
		decryptCipher.init(Cipher.DECRYPT_MODE, privateKey());
		return decryptCipher.doFinal(data);
	}

	public void setPrivateKey(byte[] data) {
		privateKeyData = data;
	}

	public void setPublicKey(byte[] data) {
		publicKeyData = data;
	}
	
	public byte[] getPrivateKey() {
		return privateKeyData;
	}

	public byte[] getPublicKey() {
		return publicKeyData;
	}
	
	public static class Signed {
		private byte[] data;
		private byte[] signature;

		public byte[] getData() {
			return data;
		}

		public void setData(byte[] data) {
			this.data = data;
		}

		public byte[] getSignature() {
			return signature;
		}

		public void setSignature(byte[] signature) {
			this.signature = signature;
		}
	}
}
