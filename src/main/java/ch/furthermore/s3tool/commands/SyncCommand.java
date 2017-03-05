package ch.furthermore.s3tool.commands;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.s3.FileSyncInfo;
import ch.furthermore.s3tool.s3.LocalDirectory;

@Service("sync" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class SyncCommand extends SyncCommandBase {
	protected void afterSync(LocalDirectory localDirectory) throws IOException {
		localDirectory.updateCache();
	}
	
	protected List<FileSyncInfo> gatherFilesToSync(List<FileSyncInfo> localVersions, List<FileSyncInfo> bucketVersions) { 
		Map<String,FileSyncInfo> localMap = map(localVersions);
		Map<String,FileSyncInfo> bucketMap = map(bucketVersions);
		
		List<FileSyncInfo> result = new LinkedList<FileSyncInfo>();
		
		for (String key : localMap.keySet()) {
			FileSyncInfo localVersion = localMap.get(key);
			if (bucketMap.containsKey(key)) {
				FileSyncInfo bucketVersion = bucketMap.get(key);
				if (localVersion.getVersion() > bucketVersion.getVersion()) {
					result.add(localVersion);
				}
				else if (localVersion.getVersion() < bucketVersion.getVersion()) {
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
			FileSyncInfo bucketVersion = bucketMap.get(key);
			if (!localMap.containsKey(key)) {
				result.add(bucketVersion);
			}
		}
		
		return result;
	}
}
