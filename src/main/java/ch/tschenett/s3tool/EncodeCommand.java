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
public class EncodeCommand extends Command {
	@Value(value="${file}")
	private String filename;
	
	@Value(value="${file2}")
	private String filename2;
	
	@Value(value="${aesKey}")
	private String aesKeyBase64;
	
	@Autowired
	private Crypto crypto;
	
	@Override
	public void execute() throws IOException {
		File plainFile = new File(filename);
		File encodedFile = new File(filename2);
		
		crypto.encodeFile(aesKeyBase64, plainFile, encodedFile);
		
		syserr("encoded " + plainFile + " to " + encodedFile);
	}
}
