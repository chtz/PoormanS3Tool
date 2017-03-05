package ch.furthermore.s3tool.commands;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.s3.FileSyncInfo;

@Service("upSync" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UpSyncCommand extends SyncCommandBase {
	protected List<FileSyncInfo> gatherFilesToSync(List<FileSyncInfo> localVersions, List<FileSyncInfo> bucketVersions) { 
		Map<String,FileSyncInfo> localMap = map(localVersions);
		Map<String,FileSyncInfo> bucketMap = map(bucketVersions);
		
		List<FileSyncInfo> result = new LinkedList<FileSyncInfo>();
		
		for (String key : localMap.keySet()) {
			FileSyncInfo localVersion = localMap.get(key);
			if (bucketMap.containsKey(key)) {
				FileSyncInfo bucketVersion = bucketMap.get(key);
				if (bucketVersion.isDeleted()) {
					result.add(localVersion);
				}
				else if (bucketVersion.getVersion() < localVersion.getVersion()) {
					result.add(localVersion);
				}
				else {
					//nothing to do
				}
			}
			else {
				result.add(localVersion);
			}
		}
		
		for (String key : bucketMap.keySet()) {
			FileSyncInfo bucketVersion = bucketMap.get(key);
			
			if (!bucketVersion.isDeleted() && !localMap.containsKey(key)) {
				result.add(new FileSyncInfo(key, bucketVersion.getLastModified(), true, true));
			}
		}
		
		return result;
	}
}
