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

@Service("upSync" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UpSyncCommand extends Command {
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
	
	@Autowired
	private S3 s3;
	
	@Override
	public void execute() throws IOException {
		final Map<String,Long> lastModifiedByKey = s3.listKeysWithLastModifiedMeta(bucketName);
		
		File directory = new File(directoryname);
		for (File file : directory.listFiles()) {
			if (file.isDirectory()) continue;
			
			String key = file.getName();
			
			if (lastModifiedByKey.containsKey(key)) {
				long lastModifiedS3 = lastModifiedByKey.remove(key);
				
				if (((long)(file.lastModified() / 1000)) > ((long)(lastModifiedS3 / 1000))) {
					putObject(file, key);
					
					syserr("uploaded newer " + file + " to s3://" + bucketName + "/" + key);
				}
				else {
					syserr("ignored older " + file);
				}
			}
			else {
				putObject(file, key);
				
				syserr("uploaded new " + file + " to s3://" + bucketName + "/" + key);
			}
		}

		for (String key : lastModifiedByKey.keySet()) {
			deleteObject(key);
			
			syserr("deleted s3://" + bucketName + "/" + key);
		}
	}

	private void deleteObject(String key) {
		s3.deleteObject(bucketName, key);
	}

	private void putObject(File file, String key) throws IOException {
		s3.putObject(bucketName, aesKeyBase64, encryptPublicKeyBase64, signPrivateKeyBase64, key, CONTENT_TYPE, file);
	}
}
