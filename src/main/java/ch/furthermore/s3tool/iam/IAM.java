package ch.furthermore.s3tool.iam;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.AccessKey;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.AddUserToGroupRequest;
import com.amazonaws.services.identitymanagement.model.CreateAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.CreateGroupRequest;
import com.amazonaws.services.identitymanagement.model.CreateGroupResult;
import com.amazonaws.services.identitymanagement.model.CreateUserRequest;
import com.amazonaws.services.identitymanagement.model.DeleteAccessKeyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteGroupPolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteGroupRequest;
import com.amazonaws.services.identitymanagement.model.DeleteUserRequest;
import com.amazonaws.services.identitymanagement.model.GetGroupRequest;
import com.amazonaws.services.identitymanagement.model.Group;
import com.amazonaws.services.identitymanagement.model.ListAccessKeysRequest;
import com.amazonaws.services.identitymanagement.model.ListGroupPoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListGroupsForUserRequest;
import com.amazonaws.services.identitymanagement.model.ListUsersRequest;
import com.amazonaws.services.identitymanagement.model.ListUsersResult;
import com.amazonaws.services.identitymanagement.model.PutGroupPolicyRequest;
import com.amazonaws.services.identitymanagement.model.RemoveUserFromGroupRequest;
import com.amazonaws.services.identitymanagement.model.User;

@Service
@Scope(value=ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class IAM {
	@Value(value="${region:eu-west-1}")
	private String region;
	
	@Value(value="${accessKey}")
	private String accessKey;
	
	@Value(value="${secretKey}")
	private String secretKey;
	
	private AmazonIdentityManagement iam;

	@PostConstruct
	public void init() {
		iam = AmazonIdentityManagementClientBuilder
				.standard()
				.withRegion(region)
				.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
				.build();
	}
	
	public void createROGroup(String bucketName) {
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
		
		createGroup(bucketName, true, policy);
	}
	
	public void createRWGroup(String bucketName) {
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
		
		createGroup(bucketName, false, policy);
	}
	
	private void createGroup(String bucketName, boolean readOnly, String policy) {
		CreateGroupResult result = iam.createGroup(new CreateGroupRequest(groupName(bucketName, readOnly))
				.withPath(pathName(bucketName)));
		
		iam.putGroupPolicy(new PutGroupPolicyRequest(result.getGroup().getGroupName(), bucketName + "-" + readWriteInfix(readOnly) + "-policy", policy));
	}
	
	public void deleteROGroup(String bucketName) {
		deleteGroup(bucketName, true);
	}

	public void deleteRWGroup(String bucketName) {
		deleteGroup(bucketName, false);
	}
	
	public void deleteGroup(String bucketName, boolean readOnly) {
		String groupName = groupName(bucketName, readOnly);
		
		for (String policyName : iam.listGroupPolicies(new ListGroupPoliciesRequest(groupName)).getPolicyNames()) {
			iam.deleteGroupPolicy(new DeleteGroupPolicyRequest(groupName, policyName));
		}
		
		for (User user : iam.getGroup(new GetGroupRequest(groupName)).getUsers()) {
			iam.removeUserFromGroup(new RemoveUserFromGroupRequest(groupName, user.getUserName()));
		}
		
		iam.deleteGroup(new DeleteGroupRequest(groupName));
	}

	private String pathName(String bucketName) {
		return "/" + bucketName + "/";
	}

	private String groupName(String bucketName, boolean readOnly) {
		return bucketName + "-" + readWriteInfix(readOnly) + "-group";
	}

	private String readWriteInfix(boolean readOnly) {
		return readOnly ? "ro" : "rw";
	}
	
	public AccessKey createGroupUser(String userName, boolean readOnly, String bucketName) {
		User user = iam.createUser(new CreateUserRequest(userName)
				.withPath(pathName(bucketName))).getUser();
		
		iam.addUserToGroup(new AddUserToGroupRequest(groupName(bucketName, readOnly), user.getUserName()));
		
		return iam.createAccessKey(new CreateAccessKeyRequest(user.getUserName())).getAccessKey();
	}

	public List<String> listUsers(String bucketName) {
		List<String> users = new LinkedList<String>();
		
		ListUsersResult result = iam.listUsers(new ListUsersRequest().withPathPrefix(pathName(bucketName)));
		for (;;) {
			for (User user : result.getUsers()) {
				users.add(user.getUserName());
			}
			
			if (result.isTruncated()) {
				iam.listUsers(new ListUsersRequest().withMarker(result.getMarker()));
			}
			else {
				break;
			}
		}
		
		return users;
	}

	public void deleteUser(String userName) {
		for (Group group : iam.listGroupsForUser(new ListGroupsForUserRequest(userName)).getGroups()) {
			iam.removeUserFromGroup(new RemoveUserFromGroupRequest(group.getGroupName(), userName));
		}
		
		for (AccessKeyMetadata a : iam.listAccessKeys(new ListAccessKeysRequest().withUserName(userName)).getAccessKeyMetadata()) {
			iam.deleteAccessKey(new DeleteAccessKeyRequest(userName, a.getAccessKeyId()));
		}
		
		iam.deleteUser(new DeleteUserRequest(userName)); 
	}

	public void deleteUsers(String bucketName) {
		for (String userName : listUsers(bucketName)) {
			deleteUser(userName);
		}
	}
}
