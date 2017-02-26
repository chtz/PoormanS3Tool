package ch.furthermore.s3tool.commands;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.iam.IAM;

@Service("listUsers" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ListUsersCommand extends Command {
	@Value(value="${bucketName}")
	private String bucketName;
	
	@Autowired
	private IAM iam;
	
	@Override
	public void execute() throws IOException {
		List<String> users = iam.listUsers(bucketName);
		
		for (String user : users) {
			sysout("userName=" + user);
		}
	}
}
