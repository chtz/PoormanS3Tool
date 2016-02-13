package ch.tschenett.s3tool;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@SpringBootApplication
public class S3ToolApp {
	private static final String USER_META_LAST_MODIFIED = "lastmodified";

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
	
	@Value(value="${prefix:}")
	private String prefix;
	
	@Value(value="${keyPublic:false}")
	private String keyPublic;
	
	@Value(value="${file:}")
	private String filename;
	
	@Value(value="${file2:}")
	private String filename2;
	
	@Value(value="${directory:}")
	private String directoryname;
	
	@Value(value="${filenamePattern:}")
	private String filenamePatternString;
	
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
		else if ("upSync".equals(command)) {
			upSync();
		}
		else if ("downSync".equals(command)) {
			downSync();
		}
	 	else throw new RuntimeException("unknown command: '"  + command + "'");
	}

	private void downSync() throws FileNotFoundException, IOException {
		final Map<String,Long> lastModifiedByShortKey = listShortKeysWithLastModifiedMeta(bucketName, prefix);
		
		File directory = new File(directoryname);
		for (Map.Entry<String, Long> lastModifiedShortKey : lastModifiedByShortKey.entrySet()) {
			String key = prefix + lastModifiedShortKey.getKey();
			File file = new File(directory, lastModifiedShortKey.getKey());
			
			if (file.exists()) {
				if (file.lastModified() < lastModifiedShortKey.getValue()) {
					getObject(key, file);
					file.setLastModified(lastModifiedShortKey.getValue());
					
					syserr("downloaded newer " + key + " to " + file);
				}
				else {
					syserr("ignored older " + key);
				}
			}
			else {
				getObject(key, file);
				file.setLastModified(lastModifiedShortKey.getValue());
				
				syserr("downloaded new " + key + " to " + file);
			}
		}
	}
	
	private void upSync() throws FileNotFoundException, IOException {
		final Map<String,Long> lastModifiedByShortKey = listShortKeysWithLastModifiedMeta(bucketName, prefix);
		
		File directory = new File(directoryname);
		for (File file : directory.listFiles(new FilenameFilter() {
			private Pattern filenamePattern = Pattern.compile(filenamePatternString);
			
			@Override
			public boolean accept(File dir, String name) {
				return filenamePattern.matcher(name).matches();
			}
		})) {
			String shortKey = file.getName();
			String key = prefix + shortKey;
			
			if (lastModifiedByShortKey.containsKey(shortKey)) {
				long lastModifiedS3 = lastModifiedByShortKey.get(shortKey);
				
				if (file.lastModified() > lastModifiedS3) {
					putObject(file, key);
					
					syserr("uploaded newer " + file + " to " + key);
				}
				else {
					syserr("ignored older " + file);
				}
			}
			else {
				putObject(file, key);
				
				syserr("uploaded new " + file + " to " + key);
			}
		}
	}

	private Map<String,Long> listShortKeysWithLastModifiedMeta(String bucketName, String prefix) {
		Map<String,Long> lastModifiedByShortKey = new HashMap<String, Long>();
		ListObjectsRequest request = new ListObjectsRequest()
				.withBucketName(bucketName)
				.withPrefix(prefix);
		ObjectListing listing = s3.listObjects(request);
		for (;;) {
			for (S3ObjectSummary  summary : listing.getObjectSummaries()) {
				ObjectMetadata meta = s3.getObjectMetadata(new GetObjectMetadataRequest(summary.getBucketName(), summary.getKey()));
				if (meta.getUserMetadata().get(USER_META_LAST_MODIFIED) != null) {
					lastModifiedByShortKey.put(summary.getKey().substring(prefix.length()), 
							new Long(meta.getUserMetadata().get(USER_META_LAST_MODIFIED)));
				}
			}
			
			if (listing.isTruncated()) {
				listing = s3.listNextBatchOfObjects(listing);
			}
			else {
				break;
			}
		}
		return lastModifiedByShortKey;
	}

	private void download() throws IOException {
		BufferedInputStream urlIn = new BufferedInputStream(new URL(url).openStream());
		File file = new File(filename);
		
		decodeIfRequiredAndCopyToFileClosing(urlIn, file);
		
		syserr("downloaded " + url + " to " + file);
	}

	private void getObject() throws IOException {
		String downloadKey = key;
		File file = new File(filename);
		getObject(downloadKey, file);
		
		syserr("downloaded " + key + " to " + file);
	}

	private void getObject(String downloadKey, File file) throws IOException {
		BufferedInputStream s3In = new BufferedInputStream(s3.getObject(new GetObjectRequest(bucketName, downloadKey)).getObjectContent());
		
		decodeIfRequiredAndCopyToFileClosing(s3In, file);
	}

	private void decodeIfRequiredAndCopyToFileClosing(InputStream in, File file) throws IOException {
		copyInToFileClosing(aesKey != null ? aesKey.decodingInputStream(in) : in, file);
	}

	private void putObject() throws IOException {
		File plainFile = new File(filename);
		String uploadKey = key;
		putObject(plainFile, uploadKey);
		
		syserr("uploaded " + plainFile + " to " + key);
	}

	private void putObject(File plainFile, String uploadKey) throws IOException, FileNotFoundException {
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
			
			Map<String, String> userMetadata = new HashMap<String, String>();
			userMetadata.put(USER_META_LAST_MODIFIED, Long.toString(plainFile.lastModified()));
			metadata.setUserMetadata(userMetadata);
			
			InputStream uploadFileIn = new BufferedInputStream(new FileInputStream(uploadFile));
			try {
				PutObjectRequest request = new PutObjectRequest(bucketName, uploadKey, uploadFileIn, metadata);
				
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
