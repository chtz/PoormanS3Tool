package ch.furthermore.s3tool.commands;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.crypto.Crypto;

@Service("genKey" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GenKeyCommand extends Command {
	@Autowired
	private Crypto crypto;
	
	@Override
	public void execute() throws IOException {
		sysout("aesKey=" + crypto.genKey());
	}
}
