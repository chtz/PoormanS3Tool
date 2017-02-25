package ch.furthermore.s3tool.commands;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.AccessKey;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.PutUserPolicyRequest;
import com.amazonaws.services.identitymanagement.model.User;

@Service("createUser" + Command.COMMAND_BEAN_NAME_SUFFIX)
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class CreateUserCommand extends Command {
	@Value(value="${accessKey}")
	private String accessKey;
	
	@Value(value="${secretKey}")
	private String secretKey;
	
	@Value(value="${userName}")
	private String userName;
	
	@Value(value="${bucketName}")
	private String bucketName;
	
	@Value(value="${readOnly}")
	private boolean readOnly;
	
	private AmazonIdentityManagement iam;

	@PostConstruct
	public void init() {
		iam = new AmazonIdentityManagementClient(new BasicAWSCredentials(accessKey, secretKey));
	}
	
	@Override
	public void execute() throws IOException {
		User user = iam.createUser(new CreateUserRequest(userName)).getUser();
		
		if ("true".equals(readOnly)) {
		    String policy = "{"
		    		+ "\"Version\": \"2012-10-17\","
		    		+ "\"Statement\": ["
		    		+ "    {"
		    		+ "       \"Effect\": \"Allow\","
		    		+ "       \"Action\": ["
		    		+ "           \"s3:GetBucketLocation\","
		    		+ "           \"s3:GetBucketVersioning\","
		     		+ "           \"s3:GetBucketWebsite\","
		    		+ "           \"s3:GetObject\","
		      		+ "           \"s3:GetObjectAcl\","
		      		+ "           \"s3:GetObjectVersion\","
		        	+ "           \"s3:ListAllMyBuckets\","
		       		+ "           \"s3:ListBucket\","
		         	+ "           \"s3:ListBucketMultipartUploads\","
		        	+ "           \"s3:ListBucketVersions\","
		         	+ "           \"s3:ListMultipartUploadParts\""
		     		+ "       ],"
		    		+ "       \"Resource\": ["
		        	+ "       	  \"arn:aws:s3:::" + bucketName + "\","
		         	+ "           \"arn:aws:s3:::" + bucketName + "/*\""
		         	+ "       ]"
		         	+ "    }"
		    		+ "]}";
			iam.putUserPolicy(new PutUserPolicyRequest(user.getUserName(), "s3-" + bucketName + "-readonly", policy));
		}
		else {
			String policy = "{"
		    		+ "\"Version\": \"2012-10-17\","
		    		+ "\"Statement\": ["
		    		+ "    {"
		    		+ "       \"Effect\": \"Allow\","
		    		+ "       \"Action\": ["
		    		+ "           \"s3:*\""
		     		+ "       ],"
		    		+ "       \"Resource\": ["
		    		+ "       	  \"arn:aws:s3:::" + bucketName + "\","
		         	+ "           \"arn:aws:s3:::" + bucketName + "/*\""
		         	+ "       ]"
		         	+ "    }"
		    		+ "]}";
			iam.putUserPolicy(new PutUserPolicyRequest(user.getUserName(), "s3-" + bucketName + "-readwrite", policy));
		}
		
		AccessKey accessKey = iam.createAccessKey(new CreateAccessKeyRequest(user.getUserName())).getAccessKey();
		
		sysout("accessKey=" + accessKey.getAccessKeyId());
		sysout("secretKey=" + accessKey.getSecretAccessKey());
	}
}
