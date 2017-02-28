package ch.furthermore.s3tool.commands;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import ch.furthermore.s3tool.iam.IAM;
import ch.furthermore.s3tool.s3.S3;

@Service("createBucket" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CreateBucketCommand extends Command {
	@Autowired
	private S3 s3;
	
	@Autowired
	private IAM iam;
	
	@Value(value="${bucketName}")
	private String bucketName;
	
	@Override
	public void execute() throws IOException {
		s3.createBucket(bucketName);
		
		iam.createROGroup(bucketName);
		iam.createRWGroup(bucketName);
		
		sysout("bucketName=" + bucketName);
	}
}
