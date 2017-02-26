package ch.furthermore.s3tool.commands;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.amazonaws.services.identitymanagement.model.AccessKey;

import ch.furthermore.s3tool.iam.IAM;

@Service("createUser" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CreateUserCommand extends Command {
	@Value(value="${userName}")
	private String userName;
	
	@Value(value="${bucketName}")
	private String bucketName;
	
	@Value(value="${readOnly}")
	private boolean readOnly;
	
	@Autowired
	private IAM iam;
	
	@Override
	public void execute() throws IOException {
		AccessKey accessKey = iam.createGroupUser(userName, readOnly, bucketName);
		
		sysout("accessKey=" + accessKey.getAccessKeyId());
		sysout("secretKey=" + accessKey.getSecretAccessKey());
	}
}
