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
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.DeleteBucketRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
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
	private static final String USER_META_ENCODED_KEY = "encodedkey";
	private static final String USER_META_SIGNATURE = "signature";

	@Value(value="${region:eu-west-1}")
	private String region;
	
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
		
		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder
			.standard()
			.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
			.withClientConfiguration(config);
		
		if (!"".equals(endpoint)) {
			builder.withEndpointConfiguration(new EndpointConfiguration(endpoint, region));
		}
		else {
			builder.withRegion(region);
		}
		
		s3 = builder.build();
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

	public enum GetObjectOutcome {
		SUCCESS,
		KEY_DECODING_FAILED,
		SIGNATURE_VERIFICATION_FAILED
	}
	
	public GetObjectOutcome getObject(String bucketName, String aesKeyBase64, String decryptPrivateKeyBase64, String verifyPublicKeyBase64, String downloadKey, File file) throws IOException {
		ObjectMetadata meta = s3.getObjectMetadata(new GetObjectMetadataRequest(bucketName, downloadKey));
		
		if ("".equals(aesKeyBase64) && !"".equals(decryptPrivateKeyBase64)) {
			if (meta.getUserMetadata().get(USER_META_ENCODED_KEY) != null) {
				String encodedAesKeyBase64 = meta.getUserMetadata().get(USER_META_ENCODED_KEY);

				try {
					aesKeyBase64 = crypto.decodeKey(decryptPrivateKeyBase64, encodedAesKeyBase64);
				}
				catch (Exception e) {
					return GetObjectOutcome.KEY_DECODING_FAILED;
				}
			}
		}
		
		InputStream s3In = getObject(bucketName, downloadKey);
		
		crypto.decodeToFileClosing(aesKeyBase64, s3In, file);
		
		if (!"".equals(verifyPublicKeyBase64) && meta.getUserMetadata().get(USER_META_SIGNATURE) != null) {
			if (!crypto.verify(verifyPublicKeyBase64, meta.getUserMetadata().get(USER_META_SIGNATURE), file)) {
				file.delete();
				
				return GetObjectOutcome.SIGNATURE_VERIFICATION_FAILED;
			}
		}
		
		return GetObjectOutcome.SUCCESS;
	}
	
	private InputStream getObject(String bucketName, String key) throws IOException {
		return new BufferedInputStream(s3.getObject(new GetObjectRequest(bucketName, key)).getObjectContent());
	}

	public void putObject(String bucketName, String aesKeyBase64, String encryptPublicKeyBase64, String signPrivateKeyBase64, String key, String contentType, File plainFile) throws IOException, FileNotFoundException {
		if ("".equals(aesKeyBase64)) {
			aesKeyBase64 = crypto.genKey();
		}
		
		String signature = null;
		if (!"".equals(signPrivateKeyBase64)) {
			signature = crypto.sign(signPrivateKeyBase64, plainFile);
		}
		
		File uploadFile = File.createTempFile("enc", ".tmp");
		try {
			crypto.encodeFile(aesKeyBase64, plainFile, uploadFile);
			
			uploadFile.setLastModified(plainFile.lastModified());
			
			putObjectInt(bucketName, aesKeyBase64, encryptPublicKeyBase64, signature, key, contentType, uploadFile);
		}
		finally {
			uploadFile.delete();
		}
	}
	
	private void putObjectInt(String bucketName, String aesKeyBase64, String encryptPublicKeyBase64, String signature, String key, String contentType, File encodedFile) throws IOException {
		Map<String, String> userMetadata = new HashMap<String, String>();
		userMetadata.put(USER_META_LAST_MODIFIED, Long.toString(encodedFile.lastModified()));
		
		if (!"".equals(encryptPublicKeyBase64)) {
			String encodedAesKeyBase64 = crypto.encodeKey(encryptPublicKeyBase64, aesKeyBase64);
			
			userMetadata.put(USER_META_ENCODED_KEY, encodedAesKeyBase64);
		}
		
		if (signature != null) {
			userMetadata.put(USER_META_SIGNATURE, signature);
		}
		
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setUserMetadata(userMetadata);
		metadata.setContentLength(encodedFile.length());
		metadata.setContentType(contentType);
			
		InputStream in = new BufferedInputStream(new FileInputStream(encodedFile));
		try {
			PutObjectRequest request = new PutObjectRequest(bucketName, key, in, metadata);
			
			s3.putObject(request);
		}
		finally {
			in.close();
		}
	}

	public void createBucket(String bucketName) {
		s3.createBucket(new CreateBucketRequest(bucketName, region));
	}

	public void deleteObject(String bucketName, String key) {
		s3.deleteObject(bucketName, key);
	}

	public void deleteBucket(String bucketName) {
		ListObjectsRequest request = new ListObjectsRequest().withBucketName(bucketName);
		ObjectListing listing = s3.listObjects(request);
		for (;;) {
			for (S3ObjectSummary  summary : listing.getObjectSummaries()) {
				s3.deleteObject(new DeleteObjectRequest(bucketName, summary.getKey()));
			}
			
			if (listing.isTruncated()) {
				listing = s3.listNextBatchOfObjects(listing);
			}
			else {
				break;
			}
		}
		
		s3.deleteBucket(new DeleteBucketRequest(bucketName));
	}
}
