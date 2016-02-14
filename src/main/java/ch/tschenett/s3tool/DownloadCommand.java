package ch.tschenett.s3tool;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service("download" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DownloadCommand extends Command {
	@Value(value="${url}")
	private String url;
	
	@Value(value="${file}")
	private String filename;
	
	@Value(value="${aesKey:}")
	private String aesKeyBase64;
	
	@Autowired
	private Crypto crypto;
	
	@Override
	public void execute() throws IOException {
		BufferedInputStream urlIn = new BufferedInputStream(new URL(url).openStream());
		File file = new File(filename);
		
		if ("".equals(aesKeyBase64)) {
			crypto.copyToFileClosing(urlIn, file);
		}
		else {
			crypto.decodeToFileClosing(aesKeyBase64, urlIn, file);
		}
		
		syserr("downloaded " + url + " to " + file);
	}
}
