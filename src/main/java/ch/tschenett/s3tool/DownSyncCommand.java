package ch.tschenett.s3tool;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service("downSync" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DownSyncCommand extends Command {
	@Value(value="${directory}")
	private String directoryname;
	
	@Value(value="${prefix}")
	private String prefix;

	@Value(value="${aesKey:}")
	private String aesKeyBase64;
	
	@Value(value="${filenamePattern:NO_PATTERN}")
	private String filenamePatternString;
	
	@Value(value="${deleteLocal:false}")
	private boolean deleteLocal;
	
	@Autowired
	private S3 s3;
	
	@Override
	public void execute() throws IOException {
		final Map<String,Long> lastModifiedByShortKey = s3.listShortKeysWithLastModifiedMeta(prefix);
		
		Set<String> processedKeys = new HashSet<String>();
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
			
			processedKeys.add(key);
		}
		
		for (File file : directory.listFiles(new FilenameFilter() {
			private Pattern filenamePattern = Pattern.compile(filenamePatternString);
			
			@Override
			public boolean accept(File dir, String name) {
				return filenamePattern.matcher(name).matches();
			}
		})) {
			String shortKey = file.getName();
			String key = prefix + shortKey;
			
			if (!processedKeys.contains(key)) {
				if (deleteLocal) {
					file.delete();
					
					syserr("deleted " + file);	
				}
				else {
					syserr("delete candidate " + file);
				}
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
