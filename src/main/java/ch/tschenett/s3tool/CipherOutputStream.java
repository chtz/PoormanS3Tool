package ch.tschenett.s3tool;

import java.io.IOException;
import java.io.OutputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

public class CipherOutputStream extends OutputStream {
	private final Cipher cipher;
	private final OutputStream target;

	public CipherOutputStream(Cipher cipher, OutputStream target) {
		this.cipher = cipher;
		this.target = target; 
	}
	
	@Override
	public void write(int b) throws IOException {
		byte[] encoded = cipher.update(new byte[]{(byte)b});
		if (encoded != null) {
			target.write(encoded);
		}
	}

	@Override
	public void close() throws IOException {
		try {
			byte[] encoded = cipher.doFinal();
			if (encoded != null) {
				target.write(encoded);
			}
		} catch (IllegalBlockSizeException e) {
			throw new IOException("crypto: IllegalBlockSize", e);
		} catch (BadPaddingException e) {
			throw new IOException("crypto: BadPadding", e);
		}
		
		target.close();
	}
}
