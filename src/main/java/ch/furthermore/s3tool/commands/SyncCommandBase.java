package ch.furthermore.s3tool.commands;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ch.furthermore.s3tool.s3.FileSyncInfo;
import ch.furthermore.s3tool.s3.LocalDirectory;
import ch.furthermore.s3tool.s3.S3;
import ch.furthermore.s3tool.s3.S3.GetObjectOutcome;

public abstract class SyncCommandBase extends Command { 
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
	
	private LocalDirectory localDirectory;
	
	@PostConstruct
	public void init() {
		localDirectory = new LocalDirectory(new File(directoryname));
	}
	
	protected abstract List<FileSyncInfo> gatherFilesToSync(List<FileSyncInfo> localVersions, List<FileSyncInfo> bucketVersions);
	
	protected void afterSync(LocalDirectory localDirectory) throws IOException {
		//do nothing by default
	}
	
	@Override
	public void execute() throws IOException {
		List<FileSyncInfo> localVersions = localDirectory.versions();
		List<FileSyncInfo> bucketVersions = s3.versions(bucketName);
		
		for (FileSyncInfo version : gatherFilesToSync(localVersions, bucketVersions)) {
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
		
		afterSync(localDirectory);
	}

	protected Map<String, FileSyncInfo> map(List<FileSyncInfo> versions) {
		Map<String,FileSyncInfo> m = new HashMap<String, FileSyncInfo>();
		for (FileSyncInfo v : versions) {
			m.put(v.getKey(), v);
		}
		return m;
	}

	private void download(FileSyncInfo version) throws IOException {
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
			file.setLastModified(version.getLastModified());
			syserr("downloaded s3://" + bucketName + "/" + version.getKey() + " to " + file);
			break;
		}
	}
	
	private void createEmptyMarkerFile(FileSyncInfo version) throws IOException {
		File file = new File(directoryname, version.getKey());
		file.delete();
		file.createNewFile();
		file.setLastModified(version.getLastModified());
	}

	private GetObjectOutcome getObject(String key, File file) throws IOException {
		return s3.getObject(bucketName, aesKeyBase64, decryptPrivateKeyBase64, verifyPublicKeyBase64, key, file);
	}

	private void upload(FileSyncInfo version) throws IOException {
		File file = new File(directoryname, version.getKey());
		
		putObject(file, version.getKey());
	
		syserr("uploaded " + file + " to s3://" + bucketName + "/" + version.getKey());
	}
	
	private void putObject(File file, String key) throws IOException {
		s3.putObject(bucketName, aesKeyBase64, encryptPublicKeyBase64, signPrivateKeyBase64, key, CONTENT_TYPE, file);
	}
	
	private void deleteLocal(FileSyncInfo version) {
		File file = new File(directoryname, version.getKey());
		
		if (file.exists()) {
			file.delete();
			
			syserr("deleted " + file);
		}
	}

	private void deleteRemote(FileSyncInfo version) throws IOException {
		deleteObject(version.getKey());
		
		syserr("deleted s3://" + bucketName + "/" + version.getKey());
	}
	
	private void deleteObject(String key) throws IOException {
		s3.deleteObject(bucketName, key);
	}
}
