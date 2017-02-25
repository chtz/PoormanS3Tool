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

@Service
public class Crypto {
	public String genKey() {
		return Base64.encodeBase64String(AES.createKeyData());
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
