package ch.furthermore.s3tool.commands;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.s3.FileVersion;

@Service("sync" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SyncCommand extends SyncCommandBase {
	protected void afterSync() throws IOException {
		localDirectory.updateCache();
	}
	
	protected List<FileVersion> mostRecentVersions(List<FileVersion> localVersions, List<FileVersion> bucketVersions) { 
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
}
