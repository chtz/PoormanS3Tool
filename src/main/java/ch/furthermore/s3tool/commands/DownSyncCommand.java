package ch.furthermore.s3tool.commands;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.s3.FileVersion;

@Service("downSync" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DownSyncCommand extends SyncCommandBase {
	protected List<FileVersion> gatherVersionsToSync(List<FileVersion> localVersions, List<FileVersion> bucketVersions) { 
		Map<String,FileVersion> localMap = map(localVersions);
		Map<String,FileVersion> bucketMap = map(bucketVersions);
		
		List<FileVersion> result = new LinkedList<FileVersion>();
		
		for (String key : bucketMap.keySet()) {
			FileVersion bucketVersion = bucketMap.get(key);
			if (bucketVersion.isDeleted()) {
				result.add(bucketVersion);
			}
			else if (localMap.containsKey(key)) {
				FileVersion localVersion = localMap.get(key);
				if (localVersion.getVersion() / 1000 < bucketVersion.getVersion() / 1000) {
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
			FileVersion localVersion = localMap.get(key);
			
			if (!bucketMap.containsKey(key)) {
				result.add(new FileVersion(key, localVersion.getVersion(), true, false));
			}
		}
		
		return result;
	}
}
