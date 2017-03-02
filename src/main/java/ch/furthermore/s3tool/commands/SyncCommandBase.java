package ch.furthermore.s3tool.commands;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ch.furthermore.s3tool.s3.FileVersion;
import ch.furthermore.s3tool.s3.LocalDirectory;
import ch.furthermore.s3tool.s3.S3;
import ch.furthermore.s3tool.s3.S3.GetObjectOutcome;

public abstract class SyncCommandBase extends Command { 
	static final String CONTENT_TYPE = "application/octet-stream";
	
	@Value(value="${bucketName}")
	String bucketName;
	
	@Value(value="${directory}")
	String directoryname;
	
	@Value(value="${aesKey:}")
	String aesKeyBase64;
	
	@Value(value="${encryptPublicKey:}")
	String encryptPublicKeyBase64;
	
	@Value(value="${signPrivateKey:}")
	String signPrivateKeyBase64;
	
	@Value(value="${decryptPrivateKey:}")
	String decryptPrivateKeyBase64;
	
	@Value(value="${verifyPublicKey:}")
	String verifyPublicKeyBase64;
	
	@Autowired
	S3 s3;
	
	LocalDirectory localDirectory;
	
	@PostConstruct
	public void init() {
		localDirectory = new LocalDirectory(new File(directoryname));
	}
	
	protected List<FileVersion> versionsToSync() {
		List<FileVersion> localVersions = localDirectory.versions();
		List<FileVersion> bucketVersions = s3.versions(bucketName);
		
		return mostRecentVersions(localVersions, bucketVersions);
	}
	
	protected abstract List<FileVersion> mostRecentVersions(List<FileVersion> localVersions, List<FileVersion> bucketVersions);
	
	protected abstract void afterSync() throws IOException;
	
	@Override
	public void execute() throws IOException {
		for (FileVersion version : versionsToSync()) {
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
		
		afterSync();
	}

	Map<String, FileVersion> map(List<FileVersion> versions) {
		Map<String,FileVersion> m = new HashMap<String, FileVersion>();
		for (FileVersion v : versions) {
			m.put(v.getKey(), v);
		}
		return m;
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
}
