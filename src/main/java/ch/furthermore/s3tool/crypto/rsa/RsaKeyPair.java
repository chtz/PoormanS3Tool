package ch.furthermore.s3tool.crypto.rsa;

public class RsaKeyPair {
	private final byte[] privateKeyData;
	private final byte[] publicKeyData;
	
	public RsaKeyPair(byte[] privateKeyData, byte[] publicKeyData) {
		this.privateKeyData = privateKeyData;
		this.publicKeyData = publicKeyData;
	}

	public byte[] getPrivateKeyData() {
		return privateKeyData;
	}

	public byte[] getPublicKeyData() {
		return publicKeyData;
	}
}
