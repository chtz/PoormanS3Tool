package ch.furthermore.s3tool.s3;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import ch.furthermore.s3tool.crypto.Crypto;

@Service
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class S3 {
	private static final String USER_META_LAST_MODIFIED = "lastmodified";

	@Value(value="${accessKey}")
	private String accessKey;
	
	@Value(value="${secretKey}")
	private String secretKey;
	
	@Value(value="${endpoint:}")
	private String endpoint;

	@Value(value="${proxyHost:}")
	private String proxyHost;
	
	@Value(value="${proxyPort:-1}")
	private int proxyPort;
	
	@Autowired
	private Crypto crypto;
	
	private AmazonS3 s3;

	@PostConstruct
	public void init() {
		ClientConfiguration config = new ClientConfiguration();
		
		if (!"".equals(proxyHost)) {
			config.setProxyHost(proxyHost);
			config.setProxyPort(proxyPort);
		}
		
		s3 = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey), config);
		
		if (!"".equals(endpoint)) {
			s3.setEndpoint(endpoint);
		}
	}
	
	public Map<String,Long> listKeysWithLastModifiedMeta(String bucketName) {
		Map<String,Long> lastModifiedByKey = new HashMap<String, Long>();
		
		ListObjectsRequest request = new ListObjectsRequest().withBucketName(bucketName);
		ObjectListing listing = s3.listObjects(request);
		for (;;) {
			for (S3ObjectSummary  summary : listing.getObjectSummaries()) {
				ObjectMetadata meta = s3.getObjectMetadata(new GetObjectMetadataRequest(summary.getBucketName(), summary.getKey()));
				
				if (meta.getUserMetadata().get(USER_META_LAST_MODIFIED) != null) {
					lastModifiedByKey.put(summary.getKey(),  new Long(meta.getUserMetadata().get(USER_META_LAST_MODIFIED)));
				}
			}
			
			if (listing.isTruncated()) {
				listing = s3.listNextBatchOfObjects(listing);
			}
			else {
				break;
			}
		}
		
		return lastModifiedByKey;
	}

	public void getObject(String bucketName, String aesKeyBase64, String downloadKey, File file) throws IOException {
		InputStream s3In = getObject(bucketName, downloadKey);
		
		crypto.decodeToFileClosing(aesKeyBase64, s3In, file);
	}
	
	private InputStream getObject(String bucketName, String key) throws IOException {
		return new BufferedInputStream(s3.getObject(new GetObjectRequest(bucketName, key)).getObjectContent());
	}

	public void putObject(String bucketName, String aesKeyBase64, String key, String contentType, File plainFile) throws IOException, FileNotFoundException {
		File uploadFile = File.createTempFile("enc", ".tmp");
		try {
			crypto.encodeFile(aesKeyBase64, plainFile, uploadFile);
			
			uploadFile.setLastModified(plainFile.lastModified());
			
			putObject(bucketName, key, contentType, uploadFile);
		}
		finally {
			uploadFile.delete();
		}
	}
	
	private void putObject(String bucketName, String key, String contentType, File file) throws IOException {
		Map<String, String> userMetadata = new HashMap<String, String>();
		userMetadata.put(USER_META_LAST_MODIFIED, Long.toString(file.lastModified()));
		
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setUserMetadata(userMetadata);
		metadata.setContentLength(file.length());
		metadata.setContentType(contentType);
			
		InputStream in = new BufferedInputStream(new FileInputStream(file));
		try {
			PutObjectRequest request = new PutObjectRequest(bucketName, key, in, metadata);
			
			s3.putObject(request);
		}
		finally {
			in.close();
		}
	}

	public void createBucket(String bucketName, String region) {
		s3.createBucket(new CreateBucketRequest(bucketName, region));
	}

	public void deleteObject(String bucketName, String key) {
		s3.deleteObject(bucketName, key);
	}
}
