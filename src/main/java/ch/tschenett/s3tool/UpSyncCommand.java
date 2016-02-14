package ch.tschenett.s3tool;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service("upSync" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UpSyncCommand extends Command {
	@Value(value="${directory}")
	private String directoryname;
	
	@Value(value="${filenamePattern}")
	private String filenamePatternString;
	
	@Value(value="${prefix}")
	private String prefix;

	@Value(value="${contentType:application/octet-stream}")
	private String contentType;
	
	@Value(value="${keyPublic:false}")
	private String keyPublic;
	
	@Value(value="${aesKey:}")
	private String aesKeyBase64;
	
	@Autowired
	private S3 s3;
	
	@Override
	public void execute() throws IOException {
		final Map<String,Long> lastModifiedByShortKey = s3.listShortKeysWithLastModifiedMeta(prefix);
		
		File directory = new File(directoryname);
		for (File file : directory.listFiles(new FilenameFilter() {
			private Pattern filenamePattern = Pattern.compile(filenamePatternString);
			
			@Override
			public boolean accept(File dir, String name) {
				return filenamePattern.matcher(name).matches();
			}
		})) {
			String shortKey = file.getName();
			String key = prefix + shortKey;
			
			if (lastModifiedByShortKey.containsKey(shortKey)) {
				long lastModifiedS3 = lastModifiedByShortKey.get(shortKey);
				
				if (file.lastModified() > lastModifiedS3) {
					putObject(file, key);
					
					syserr("uploaded newer " + file + " to " + key);
				}
				else {
					syserr("ignored older " + file);
				}
			}
			else {
				putObject(file, key);
				
				syserr("uploaded new " + file + " to " + key);
			}
		}
	}

	private void putObject(File file, String key) throws IOException {
		if ("".equals(aesKeyBase64)) {
			s3.putObject(key, contentType, "true".equals(keyPublic), file);
		}
		else {
			s3.putObject(aesKeyBase64, key, contentType, "true".equals(keyPublic), file);
		}
	}
}
