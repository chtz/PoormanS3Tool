package ch.tschenett.s3tool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.annotation.PostConstruct;

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

	private void download() throws IOException {
		BufferedInputStream urlIn = new BufferedInputStream(new URL(url).openStream());
		File file = new File(filename);
		
		decodeIfRequiredAndCopyToFileClosing(urlIn, file);
		
		syserr("downloaded " + url + " to " + file);
	}

	private void getObject() throws IOException {
		BufferedInputStream s3In = new BufferedInputStream(s3.getObject(new GetObjectRequest(bucketName, key)).getObjectContent());
		File file = new File(filename);
		
		decodeIfRequiredAndCopyToFileClosing(s3In, file);
		
		syserr("downloaded " + key + " to " + file);
	}

	private void decodeIfRequiredAndCopyToFileClosing(InputStream in, File file) throws IOException {
		copyInToFileClosing(aesKey != null ? aesKey.decodingInputStream(in) : in, file);
	}

	private void putObject() throws IOException {
		File plainFile = new File(filename);
		File uploadFile = null;
		try {
			if (aesKey != null) {
				uploadFile = File.createTempFile("enc", ".tmp");
				
				encodeFile(plainFile, uploadFile);
			}
			else {
				uploadFile = plainFile;
			}
			
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(uploadFile.length());
			metadata.setContentType(contentType);
			
			InputStream uploadFileIn = new BufferedInputStream(new FileInputStream(uploadFile));
			try {
				PutObjectRequest request = new PutObjectRequest(bucketName, key, uploadFileIn, metadata);
				
				if ("true".equals(keyPublic)) {
					request.withCannedAcl(CannedAccessControlList.PublicRead);
				}
				
				s3.putObject(request);
			}
			finally {
				uploadFileIn.close();
			}
		}
		finally {
			if (uploadFile != plainFile) {
				uploadFile.delete();
			}
		}
		
		syserr("uploaded " + plainFile + " to " + key);
	}

	private void decode() throws IOException {
		File encodedFile = new File(filename);
		File plainFile = new File(filename2);
		
		decodeFile(encodedFile, plainFile);
		
		syserr("decoded " + encodedFile + " to " + plainFile);
	}

	private void encode() throws IOException {
		File plainFile = new File(filename);
		File encodedFile = new File(filename2);
		
		encodeFile(plainFile, encodedFile);
		
		syserr("encoded " + plainFile + " to " + encodedFile);
	}

	private void genKey() {
		sysout(Base64.encodeBase64String(AESKey.createKeyData()));
	}

	private void encodeFile(File plainFile, File encodedFile) throws IOException {
		copyInToFileClosing(aesKey.encodingInputStream(new BufferedInputStream(new FileInputStream(plainFile))), encodedFile);
	}
	
	private void decodeFile(File encodedFile, File plainFile) throws IOException {
		copyInToFileClosing(aesKey.decodingInputStream(new BufferedInputStream(new FileInputStream(encodedFile))), plainFile);
	}

	private void copyInToFileClosing(InputStream in, File file) throws IOException {
		try {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
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
