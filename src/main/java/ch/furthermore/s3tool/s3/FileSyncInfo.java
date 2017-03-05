package ch.furthermore.s3tool.s3;

import java.io.File;

public class FileSyncInfo {
	private final String key;
	private final long lastModified;
	private final boolean deleted;
	private final boolean local;
	
	public FileSyncInfo(File f, boolean deleted, boolean local) {
		this(f.getName(), f.lastModified(), deleted, local);
	}
	
	public FileSyncInfo(String key, long lastModified, boolean deleted, boolean local) {
		this.key = key;
		this.lastModified = lastModified;
		this.deleted = deleted;
		this.local = local;
	}
	
	public long getVersion() {
		return lastModified / 1000;
	}
	
	public boolean isLocal() {
		return local;
	}

	public String getKey() {
		return key;
	}

	public long getLastModified() {
		return lastModified;
	}

	public boolean isDeleted() {
		return deleted;
	}
}
