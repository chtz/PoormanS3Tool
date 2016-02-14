package ch.tschenett.s3tool;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GenKeyCommand extends Command {
	@Autowired
	private Crypto crypto;
	
	@Override
	public void execute() throws IOException {
		sysout(crypto.genKey());
	}
}
