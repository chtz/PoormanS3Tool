package ch.tschenett.s3tool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.annotation.PostConstruct;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

@SpringBootApplication
public class S3ToolApp {
	public static void main(String[] args) {
		try {
			SpringApplication.run(S3ToolApp.class, args).getBean(S3ToolApp.class).run();
			
			System.exit(0);
		}
		catch (Exception e) {
			syserr("error: " + e.getMessage());
			
			System.exit(1);
		}
	}

	private static void sysout(String s) {
		System.out.println(s);
	}
	
	private static void syserr(String s) {
		System.err.println(s);
	}
	
	@Value(value="${accessKey:}")
	private String accessKey;
	
	@Value(value="${secretKey:}")
	private String secretKey;
	
	@Value(value="${bucketName:}")
	private String bucketName;
	
	@Value(value="${endpoint:}")
	private String endpoint;

	@Value(value="${key:}")
	private String key;
	
	@Value(value="${keyPublic:false}")
	private String keyPublic;
	
	@Value(value="${file:}")
	private String filename;
	
	@Value(value="${file2:}")
	private String filename2;
	
	@Value(value="${url:}")
	private String url;
	
	@Value(value="${contentType:}")
	private String contentType;
	
	@Value(value="${command:}")
	private String command;
	
	@Value(value="${aesKey:}")
	private String aesKeyBase64;
	
	private AmazonS3 s3;
	private AESKey aesKey;
	
	@PostConstruct
	public void init() {
		s3 = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
		if (!"".equals(endpoint)) {
			s3.setEndpoint(endpoint);
		}
		aesKey = "".equals(aesKeyBase64) ? null : new AESKey(Base64.decodeBase64(aesKeyBase64));
	}
	
	public void run() throws Exception {
		if ("genKey".equals(command)) {
			genKey();
		}
		else if ("encode".equals(command)) {
			encode();
		}
		else if ("decode".equals(command)) {
			decode();
		}
		else if ("putObject".equals(command)) {
			putObject();
		}
		else if ("getObject".equals(command)) {
			getObject();
		}
		else if ("download".equals(command)) {
			download();
		}
	 	else throw new RuntimeException("unknown command: '"  + command + "'");
	}

	private void download() throws IOException, MalformedURLException, InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, FileNotFoundException {
		File file = new File(filename);
		
		BufferedInputStream in2 = new BufferedInputStream(new URL(url).openStream());
		InputStream in = aesKey != null
				? aesKey.decodingInputStream(in2)
				: in2;
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			try {
				copyInToOut(in, out);
				
				syserr("downloaded " + url + " to " + filename);
			}
			finally {
				out.close();
			}
		}
		finally {
			in.close();
		}
	}

	private void getObject() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			FileNotFoundException, IOException {
		File file = new File(filename);
		
		BufferedInputStream in2 = new BufferedInputStream(s3.getObject(new GetObjectRequest(bucketName, key)).getObjectContent());
		InputStream in = aesKey != null 
				? aesKey.decodingInputStream(in2) 
				: in2;
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
			try {
				copyInToOut(in, out);
				
				syserr("downloaded " + key + " to " + filename);
			}
			finally {
				out.close();
			}
		}
		finally {
			in.close();
		}
	}

	private void putObject() throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			FileNotFoundException {
		File plainFile = new File(filename);
		File encFile = null;
		try {
			if (aesKey != null) {
				encFile = File.createTempFile("enc", ".tmp");
				encodeFile(plainFile, encFile);
			}
			else {
				encFile = plainFile;
			}
			
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(encFile.length());
			metadata.setContentType(contentType);
			
			InputStream input = new BufferedInputStream(new FileInputStream(encFile));
			try {
				PutObjectRequest request = new PutObjectRequest(bucketName, key, input, metadata);
				
				if ("true".equals(keyPublic)) {
					request.withCannedAcl(CannedAccessControlList.PublicRead);
				}
				
				s3.putObject(request);
				
				syserr("uploaded " + plainFile + " to " + key);
			}
			finally {
				input.close();
			}
		}
		finally {
			if (encFile != plainFile) {
				encFile.delete();
			}
		}
	}

	private void decode() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			FileNotFoundException, IOException {
		File encFile = new File(filename);
		File plainFile = new File(filename2);
		
		decodeFile(encFile, plainFile);
		
		syserr("decoded " + encFile + " to " + plainFile);
	}

	private void encode() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
			FileNotFoundException, IOException {
		File plainFile = new File(filename);
		File encFile = new File(filename2);
		
		encodeFile(plainFile, encFile);
		
		syserr("encoded " + plainFile + " to " + encFile);
	}

	private void genKey() {
		sysout(Base64.encodeBase64String(AESKey.createKeyData()));
	}

	private void encodeFile(File plainFile, File encFile) throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, FileNotFoundException, IOException {
		InputStream in = aesKey.encodingInputStream(new BufferedInputStream(new FileInputStream(plainFile)));
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(encFile));
			try {
				copyInToOut(in, out);
			}
			finally {
				out.close();
			}
		}
		finally {
			in.close();
		}
	}
	
	private void decodeFile(File encFile, File plainFile) throws InvalidKeyException, NoSuchAlgorithmException,
			NoSuchPaddingException, FileNotFoundException, IOException {
		InputStream in = aesKey.decodingInputStream(new BufferedInputStream(new FileInputStream(encFile)));
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(plainFile));
			try {
				copyInToOut(in, out);
			}
			finally {
				out.close();
			}
		}
		finally {
			in.close();
		}
	}

	private void copyInToOut(InputStream in, BufferedOutputStream out) throws IOException {
		byte[] buf = new byte[4096];
		for (int l = in.read(buf); l != -1; l = in.read(buf)) {
			out.write(buf, 0, l);
		}
	}
}
