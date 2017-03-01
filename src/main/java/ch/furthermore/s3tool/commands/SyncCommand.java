package ch.furthermore.s3tool.commands;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.s3.S3;
import ch.furthermore.s3tool.s3.S3.GetObjectOutcome;

/**
 * FIXME (Design-) bug: deleted items in s3 cause deletion of "newer" local files 
 */
@Service("sync" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SyncCommand extends Command { //FIXME experimental //FIXME avoid heavy code duplication
	private static final String CONTENT_TYPE = "application/octet-stream";
	
	@Value(value="${bucketName}")
	private String bucketName;
	
	@Value(value="${directory}")
	private String directoryname;
	
	@Value(value="${aesKey:}")
	private String aesKeyBase64;
	
	@Value(value="${encryptPublicKey:}")
	private String encryptPublicKeyBase64;
	
	@Value(value="${signPrivateKey:}")
	private String signPrivateKeyBase64;
	
	@Value(value="${decryptPrivateKey:}")
	private String decryptPrivateKeyBase64;
	
	@Value(value="${verifyPublicKey:}")
	private String verifyPublicKeyBase64;
	
	@Autowired
	private S3 s3;
	
	@Override
	public void execute() throws IOException {
		final Map<String,Long> lastModifiedByKey = s3.listKeysWithLastModifiedMeta(bucketName);
		
		File directory = new File(directoryname);
		
		File remoteCacheDir = new File(directory, ".remote");
		remoteCacheDir.mkdir();
		
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) continue;
			
			String key = file.getName();
			
			File remoteCacheFile = new File(remoteCacheDir, key);
			
			if (lastModifiedByKey.containsKey(key)) {
				//local file is already in bucket
				
				long lastModifiedS3 = lastModifiedByKey.remove(key);
				
				if (((long)(file.lastModified() / 1000)) > ((long)(lastModifiedS3 / 1000))) {
					//local file is newer than file in bucket
					
					putObject(file, key);
					
					remoteCacheFile.createNewFile();
					remoteCacheFile.setLastModified(file.lastModified());
					
					syserr("uploaded newer " + file + " to s3://" + bucketName + "/" + key);
				}
				else if (((long)(file.lastModified() / 1000)) == ((long)(lastModifiedS3 / 1000))) {
					//local file and file in bucket have same age - do nothing
					
					syserr(file + " is up to date");
				}
				else {
					//local file is older than file in bucket
					
					switch (getObject(key, file)) {
					case KEY_DECODING_FAILED:
						file.delete();
						remoteCacheDir.delete();
						
						syserr("NOT downloaded newer s3://" + bucketName + "/" + key + " (key decode failed)");
						break;
					case SIGNATURE_VERIFICATION_FAILED:
						file.delete();
						remoteCacheDir.delete();
						
						syserr("downloaded newer s3://" + bucketName + "/" + key + ", but signature verification failed. Removed " + file);
						break;
					case SUCCESS:
						file.setLastModified(lastModifiedS3);
						
						remoteCacheFile.createNewFile();
						remoteCacheFile.setLastModified(file.lastModified());
						
						syserr("downloaded newer s3://" + bucketName + "/" + key + " to " + file);
						break;
					}
				}
			}
			else {
				//local file is not in bucket
				
				if (!remoteCacheFile.exists()) {
					//local file was not recently uploaded to s3 by this client -> uploading
					
					putObject(file, key);
					
					remoteCacheFile.createNewFile();
					remoteCacheFile.setLastModified(file.lastModified());
				
					syserr("uploaded new " + file + " to s3://" + bucketName + "/" + key);
				}
				else {
					//local file was recently uploaded to s3 by this client -> deleted from s3 by another client -> removing local file
					
					file.delete();
					
					remoteCacheFile.delete();
					
					syserr("deleted " + file);
				}
			}
		}

		for (Map.Entry<String, Long> lastModifiedKey : lastModifiedByKey.entrySet()) {
			//bucket file is not in local directory
			
			String key = lastModifiedKey.getKey();
			
			File file = new File(directory, key);
			
			File remoteCacheFile = new File(remoteCacheDir, key);
			
			if (remoteCacheFile.exists() && ((long)(remoteCacheFile.lastModified() / 1000)) == ((long)(lastModifiedKey.getValue() / 1000))) {
				//(no longer existing) local file was recently uploaded to s3 by this client -> delete from s3 
				
				deleteObject(key);
			
				remoteCacheFile.delete();
				
				syserr("deleted s3://" + bucketName + "/" + key);
			}
			else {
				//bucket file was not recently downloaded by this client -> downloading
				
				switch (getObject(key, file)) {
				case KEY_DECODING_FAILED:
					syserr("NOT downloaded new s3://" + bucketName + "/" + key + " (key decode failed)");
					break;
				case SIGNATURE_VERIFICATION_FAILED:
					syserr("downloaded new s3://" + bucketName + "/" + key + ", but signature verification failed. Removed " + file);
					break;
				case SUCCESS:
					file.setLastModified(lastModifiedKey.getValue());
					
					remoteCacheFile.createNewFile();
					remoteCacheFile.setLastModified(file.lastModified());
					
					syserr("downloaded new s3://" + bucketName + "/" + key + " to " + file);
					break;
				}
			}
		}
	}

	private void deleteObject(String key) {
		s3.deleteObject(bucketName, key);
	}

	private void putObject(File file, String key) throws IOException {
		s3.putObject(bucketName, aesKeyBase64, encryptPublicKeyBase64, signPrivateKeyBase64, key, CONTENT_TYPE, file);
	}
	
	private GetObjectOutcome getObject(String key, File file) throws IOException {
		return s3.getObject(bucketName, aesKeyBase64, decryptPrivateKeyBase64, verifyPublicKeyBase64, key, file);
	}
}
