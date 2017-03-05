package ch.furthermore.s3tool.commands;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.s3.FileSyncInfo;

@Service("downSync" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DownSyncCommand extends SyncCommandBase {
	protected List<FileSyncInfo> gatherFilesToSync(List<FileSyncInfo> localVersions, List<FileSyncInfo> bucketVersions) { 
		Map<String,FileSyncInfo> localMap = map(localVersions);
		Map<String,FileSyncInfo> bucketMap = map(bucketVersions);
		
		List<FileSyncInfo> result = new LinkedList<FileSyncInfo>();
		
		for (String key : bucketMap.keySet()) {
			FileSyncInfo bucketVersion = bucketMap.get(key);
			if (bucketVersion.isDeleted()) {
				result.add(bucketVersion);
			}
			else if (localMap.containsKey(key)) {
				FileSyncInfo localVersion = localMap.get(key);
				if (localVersion.getVersion() < bucketVersion.getVersion()) {
					result.add(bucketVersion);
				}
				else {
					//nothing to do
				}
			}
			else {
				result.add(bucketVersion);
			}
		}
		
		for (String key : localMap.keySet()) {
			FileSyncInfo localVersion = localMap.get(key);
			
			if (!bucketMap.containsKey(key)) {
				result.add(new FileSyncInfo(key, localVersion.getLastModified(), true, false));
			}
		}
		
		return result;
	}
}
