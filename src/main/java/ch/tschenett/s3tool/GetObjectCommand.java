package ch.tschenett.s3tool;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service("getObject" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GetObjectCommand extends Command {
	@Value(value="${key}")
	private String key;
	
	@Value(value="${file}")
	private String filename;
	
	@Value(value="${aesKey:}")
	private String aesKeyBase64;
	
	@Autowired
	private S3 s3;
	
	@Override
	public void execute() throws IOException {
		String downloadKey = key;
		File file = new File(filename);
		
		if ("".equals(aesKeyBase64)) {
			s3.getObject(downloadKey, file);
		}
		else {
			s3.getObject(aesKeyBase64, downloadKey, file);
		}
		
		syserr("downloaded " + key + " to " + file);
	}
}
