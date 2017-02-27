package ch.furthermore.s3tool.commands;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.iam.IAM;

@Service("deleteUser" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DeleteUserCommand extends Command {
	@Value(value="${userName}")
	private String userName;
	
	@Autowired
	private IAM iam;
	
	@Override
	public void execute() throws IOException {
		iam.deleteUser(userName);
	}
}
