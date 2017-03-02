package ch.furthermore.s3tool.commands;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.s3.FileVersion;
import ch.furthermore.s3tool.s3.LocalDirectory;
import ch.furthermore.s3tool.s3.S3;
import ch.furthermore.s3tool.s3.S3.GetObjectOutcome;

@Service("sync" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SyncCommand extends Command { 
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
		LocalDirectory localDirectory = new LocalDirectory(new File(directoryname));
		
		for (FileVersion version : mostRecentVersions(localDirectory.versions(), s3.versions(bucketName))) {
			if (version.isLocal()) {
				if (version.isDeleted()) {
					deleteRemote(version);
				}
				else {
					upload(version);
				}
			}
			else {
				if (version.isDeleted()) {
					deleteLocal(version);
				}
				else {
					download(version);
				}
			}
		}
		
		localDirectory.updateCache();
	}

	private void download(FileVersion version) throws IOException {
		File file = new File(directoryname, version.getKey());
		
		switch (getObject(version.getKey(), file)) {
		case KEY_DECODING_FAILED:
			createEmptyMarkerFile(version);
			syserr("NOT downloaded s3://" + bucketName + "/" + version.getKey() + " (key decode failed). Leaving empty " + file);
			break;
			
		case SIGNATURE_VERIFICATION_FAILED:
			createEmptyMarkerFile(version);
			syserr("NOT downloaded s3://" + bucketName + "/" + version.getKey() + " (signature verification failed). Leaving empty " + file);
			break;
			
		case SUCCESS:
			file.setLastModified(version.getVersion());
			syserr("downloaded s3://" + bucketName + "/" + version.getKey() + " to " + file);
			break;
		}
	}
	
	private void createEmptyMarkerFile(FileVersion version) throws IOException {
		File file = new File(directoryname, version.getKey());
		file.delete();
		file.createNewFile();
		file.setLastModified(version.getVersion());
	}

	private GetObjectOutcome getObject(String key, File file) throws IOException {
		return s3.getObject(bucketName, aesKeyBase64, decryptPrivateKeyBase64, verifyPublicKeyBase64, key, file);
	}

	private void upload(FileVersion version) throws IOException {
		File file = new File(directoryname, version.getKey());
		
		putObject(file, version.getKey());
	
		syserr("uploaded " + file + " to s3://" + bucketName + "/" + version.getKey());
	}
	
	private void putObject(File file, String key) throws IOException {
		s3.putObject(bucketName, aesKeyBase64, encryptPublicKeyBase64, signPrivateKeyBase64, key, CONTENT_TYPE, file);
	}
	
	private void deleteLocal(FileVersion version) {
		File file = new File(directoryname, version.getKey());
		
		if (file.exists()) {
			file.delete();
			
			syserr("deleted " + file);
		}
	}

	private void deleteRemote(FileVersion version) throws IOException {
		deleteObject(version.getKey());
		
		syserr("deleted s3://" + bucketName + "/" + version.getKey());
	}
	
	private void deleteObject(String key) throws IOException {
		s3.deleteObject(bucketName, key);
	}

	private List<FileVersion> mostRecentVersions(List<FileVersion> localVersions, List<FileVersion> bucketVersions) { 
		Map<String,FileVersion> localMap = map(localVersions);
		Map<String,FileVersion> bucketMap = map(bucketVersions);
		
		List<FileVersion> result = new LinkedList<FileVersion>();
		for (String key : localMap.keySet()) {
			FileVersion localVersion = localMap.get(key);
			if (bucketMap.containsKey(key)) {
				FileVersion bucketVersion = bucketMap.get(key);
				if (localVersion.getVersion() / 1000 > bucketVersion.getVersion() / 1000) {
					result.add(localVersion);
				}
				else if (localVersion.getVersion() / 1000 < bucketVersion.getVersion() / 1000) {
					result.add(bucketVersion);
				}
				else {
					if (localVersion.isDeleted() && bucketVersion.isDeleted()) {
						//nothing to do
					}
					else if (localVersion.isDeleted()) {
						result.add(localVersion);
					}
					else if (bucketVersion.isDeleted()) {
						result.add(bucketVersion);
					}
					else {
						//nothing to do
					}
				}
			}
			else {
				result.add(localVersion);
			}
		}
		
		for (String key : bucketMap.keySet()) {
			FileVersion bucketVersion = bucketMap.get(key);
			if (!localMap.containsKey(key)) {
				result.add(bucketVersion);
			}
		}
		
		return result;
	}

	private Map<String, FileVersion> map(List<FileVersion> versions) {
		Map<String,FileVersion> m = new HashMap<String, FileVersion>();
		for (FileVersion v : versions) {
			m.put(v.getKey(), v);
		}
		return m;
	}
}
