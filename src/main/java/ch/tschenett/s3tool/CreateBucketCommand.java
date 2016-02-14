package ch.tschenett.s3tool;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service("createBucket" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CreateBucketCommand extends Command {
	@Autowired
	private S3 s3;
	
	@Override
	public void execute() throws IOException {
		s3.createBucket();
		
		syserr("bucket created");
	}
}
