package ch.furthermore.s3tool.commands;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.s3.FileVersion;

@Service("upSync" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UpSyncCommand extends SyncCommandBase {
	protected void afterSync() throws IOException {
		//nothing to do
	}
	
	protected List<FileVersion> mostRecentVersions(List<FileVersion> localVersions, List<FileVersion> bucketVersions) { 
		Map<String,FileVersion> localMap = map(localVersions);
		Map<String,FileVersion> bucketMap = map(bucketVersions);
		
		List<FileVersion> result = new LinkedList<FileVersion>();
		
		for (String key : localMap.keySet()) {
			FileVersion localVersion = localMap.get(key);
			if (bucketMap.containsKey(key)) {
				FileVersion bucketVersion = bucketMap.get(key);
				if (bucketVersion.isDeleted()) {
					result.add(localVersion);
				}
				else if (bucketVersion.getVersion() / 1000 < localVersion.getVersion() / 1000) {
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
			FileVersion bucketVersion = bucketMap.get(key);
			
			if (!bucketVersion.isDeleted() && !localMap.containsKey(key)) {
				result.add(new FileVersion(key, bucketVersion.getVersion(), true, true));
			}
		}
		
		return result;
	}
}
