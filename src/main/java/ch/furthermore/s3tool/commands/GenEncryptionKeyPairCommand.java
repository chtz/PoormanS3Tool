package ch.furthermore.s3tool.commands;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.crypto.Crypto;
import ch.furthermore.s3tool.crypto.KeyPair;

@Service("genEncryptionKeyPair" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class GenEncryptionKeyPairCommand extends Command {
	@Autowired
	private Crypto crypto;
	
	@Override
	public void execute() throws IOException {
		KeyPair keyPair = crypto.genKeyPair();
		
		sysout("decryptPrivateKey=" + keyPair.getPrivateKey());
		sysout("encryptPublicKey=" + keyPair.getPublicKey());
	}
}
