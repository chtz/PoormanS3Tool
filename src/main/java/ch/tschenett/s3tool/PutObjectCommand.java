package ch.tschenett.s3tool;

import java.io.File;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PutObjectCommand extends Command {
	@Value(value="${key}")
	private String key;
	
	@Value(value="${keyPublic:false}")
	private String keyPublic;
	
	@Value(value="${file}")
	private String filename;
	
	@Value(value="${contentType:application/octet-stream}")
	private String contentType;
	
	@Value(value="${aesKey:}")
	private String aesKeyBase64;
	
	@Autowired
	private S3 s3;
	
	@Override
	public void execute() throws IOException {
		File file = new File(filename);
		String uploadKey = key;
		
		if ("".equals(aesKeyBase64)) {
			s3.putObject(uploadKey, contentType, "true".equals(keyPublic), file);
		}
		else {
			s3.putObject(aesKeyBase64, uploadKey, contentType, "true".equals(keyPublic), file);
		}
		
		syserr("uploaded " + file + " to " + key);
	}
}
