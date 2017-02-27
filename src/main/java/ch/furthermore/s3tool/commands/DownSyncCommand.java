package ch.furthermore.s3tool.commands;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.s3.S3;
import ch.furthermore.s3tool.s3.S3.GetObjectOutcome;

@Service("downSync" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DownSyncCommand extends Command {
	@Value(value="${bucketName}")
	private String bucketName;
	
	@Value(value="${directory}")
	private String directoryname;
	
	@Value(value="${aesKey:}")
	private String aesKeyBase64;
	
	@Value(value="${decryptPrivateKey:}")
	private String decryptPrivateKeyBase64;
	
	@Value(value="${verifyPublicKey:}")
	private String verifyPublicKeyBase64;
	
	@Autowired
	private S3 s3;
	
	@Override
	public void execute() throws IOException {
		final Map<String,Long> lastModifiedByKey = s3.listKeysWithLastModifiedMeta(bucketName);
		
		Set<String> processedKeys = new HashSet<String>();
		File directory = new File(directoryname);
		for (Map.Entry<String, Long> lastModifiedKey : lastModifiedByKey.entrySet()) {
			String key = lastModifiedKey.getKey();
			File file = new File(directory, lastModifiedKey.getKey());
			
			if (file.exists()) {
				if (((long)(file.lastModified()  / 1000)) < ((long)(lastModifiedKey.getValue() / 1000))) {
					switch (getObject(key, file)) {
					case KEY_DECODING_FAILED:
						file.delete();
						syserr("NOT downloaded newer s3://" + bucketName + "/" + key + " (key decode failed). Removed " + file);
						break;
					case SIGNATURE_VERIFICATION_FAILED:
						syserr("downloaded newer s3://" + bucketName + "/" + key + ", but signature verification failed. Removed " + file);
						break;
					case SUCCESS:
						file.setLastModified(lastModifiedKey.getValue());
						syserr("downloaded newer s3://" + bucketName + "/" + key + " to " + file);
						break;
					}
				}
				else {
					syserr("ignored older s3://" + bucketName + "/" + key);
				}
			}
			else {
				switch (getObject(key, file)) {
				case KEY_DECODING_FAILED:
					syserr("NOT downloaded new s3://" + bucketName + "/" + key + " (key decode failed)");
					break;
				case SIGNATURE_VERIFICATION_FAILED:
					syserr("downloaded new s3://" + bucketName + "/" + key + ", but signature verification failed. Removed " + file);
					break;
				case SUCCESS:
					file.setLastModified(lastModifiedKey.getValue());
					syserr("downloaded new s3://" + bucketName + "/" + key + " to " + file);
					break;
				}
			}
			
			processedKeys.add(key);
		}
		
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) continue;
			
			String key = file.getName();
			
			if (!processedKeys.contains(key)) {
				file.delete();
				
				syserr("deleted " + file);	
			}
		}
	}

	private GetObjectOutcome getObject(String key, File file) throws IOException {
		return s3.getObject(bucketName, aesKeyBase64, decryptPrivateKeyBase64, verifyPublicKeyBase64, key, file);
	}
}
