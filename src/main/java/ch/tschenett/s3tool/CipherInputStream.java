package ch.tschenett.s3tool;

import java.io.IOException;
import java.io.InputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

public class CipherInputStream extends InputStream {
	private final Cipher cipher;
	private final InputStream target;
	private byte[] data;
	private int dataOff;
	private boolean closed;
	
	public CipherInputStream(Cipher cipher, InputStream target) {
		this.cipher = cipher;
		this.target = target;
	}

	@Override
	public int read() throws IOException {
		if (data != null) {
			int result = data[dataOff++];
			if (dataOff == data.length) {
				data = null;
			}
			return result & 0xff;
		}
		else if (closed) {
			return -1;
		}
		else {
			byte[] data2 = new byte[4096];
			int data2Len = target.read(data2);
			
			if (data2Len == -1) {
				closed = true;
				
				try {
					data = cipher.doFinal();
				} catch (IllegalBlockSizeException e) {
					throw new IOException("crypto: IllegalBlockSize", e);
				} catch (BadPaddingException e) {
					throw new IOException("crypto: BadPadding", e);
				}
			}
			else {
				data = cipher.update(data2, 0, data2Len);
			}
			
			if (data != null) {
				dataOff = 0;
				
				if (data.length == 0) {
					data = null;
				}
			}
			
			return read();
		}
	}
}
