package ch.tschenett.s3tool;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DownSyncCommand extends Command {
	@Value(value="${directory}")
	private String directoryname;
	
	@Value(value="${prefix}")
	private String prefix;

	@Value(value="${aesKey:}")
	private String aesKeyBase64;
	
	@Autowired
	private S3 s3;
	
	@Override
	public void execute() throws IOException {
		final Map<String,Long> lastModifiedByShortKey = s3.listShortKeysWithLastModifiedMeta(prefix);
		
		File directory = new File(directoryname);
		for (Map.Entry<String, Long> lastModifiedShortKey : lastModifiedByShortKey.entrySet()) {
			String key = prefix + lastModifiedShortKey.getKey();
			File file = new File(directory, lastModifiedShortKey.getKey());
			
			if (file.exists()) {
				if (file.lastModified() < lastModifiedShortKey.getValue()) {
					getObject(key, file);
					file.setLastModified(lastModifiedShortKey.getValue());
					
					syserr("downloaded newer " + key + " to " + file);
				}
				else {
					syserr("ignored older " + key);
				}
			}
			else {
				getObject(key, file);
				file.setLastModified(lastModifiedShortKey.getValue());
				
				syserr("downloaded new " + key + " to " + file);
			}
		}
	}

	private void getObject(String key, File file) throws IOException {
		if ("".equals(aesKeyBase64)) {
			s3.getObject(key, file);
		}
		else {
			s3.getObject(aesKeyBase64, key, file);
		}
	}
}
