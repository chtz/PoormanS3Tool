package ch.furthermore.s3tool.s3;

import java.io.File;

public class FileVersion {
	final String key;
	final long version;
	final boolean deleted;
	final boolean local;
	
	public FileVersion(File f, boolean deleted, boolean local) {
		this(f.getName(), f.lastModified(), deleted, local);
	}
	
	public FileVersion(String key, long lastModified, boolean deleted, boolean local) {
		this.key = key;
		this.version = lastModified;
		this.deleted = deleted;
		this.local = local;
	}
	
	public boolean isLocal() {
		return local;
	}

	public String getKey() {
		return key;
	}

	public long getVersion() {
		return version;
	}

	public boolean isDeleted() {
		return deleted;
	}

	@Override
	public String toString() {
		return "FileVersion [key=" + key + ", version=" + version + ", deleted=" + deleted + ", local=" + local + "]";
	}
}
