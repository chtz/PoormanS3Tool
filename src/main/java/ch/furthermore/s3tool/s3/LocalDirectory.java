package ch.furthermore.s3tool.s3;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class LocalDirectory {
	private final File baseDir;
	private final File cacheDir;

	public LocalDirectory(File baseDir) {
		this.baseDir = baseDir;
		
		this.cacheDir = new File(baseDir, ".lastsync");
		this.cacheDir.mkdir();
	}
	
	public List<FileVersion> versions() {
		List<FileVersion> result = new LinkedList<FileVersion>();
		
		for (File v : cacheDir.listFiles()) {
			File f = new File(baseDir, v.getName());
			
			if (!f.exists()) {
				result.add(new FileVersion(v, true, true));
			}
		}
		
		for (File f : baseDir.listFiles()) {
			if (f.isDirectory()) continue;
			
			result.add(new FileVersion(f, false, true));
		}
		
		return result;
	}
	
	public void updateCache() throws IOException {
		clearCache();
		
		for (File f : baseDir.listFiles()) {
			if (f.isDirectory()) continue;
			
			File v = new File(cacheDir, f.getName());
			v.createNewFile();
			v.setLastModified(f.lastModified());
		}
	}

	private void clearCache() {
		for (File f : cacheDir.listFiles()) {
			if (f.isDirectory()) continue;
			
			f.delete();
		}
	}
}
