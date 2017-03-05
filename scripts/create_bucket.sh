#!/bin/bash

#Caution: SAMPLE SCRIPT - NO INPUT VALIDATION!!!

#Pre-conditions:
#export IAM_S3_ADMIN_ACCESS_KEY=...
#export IAM_S3_ADMIN_SECRET_KEY=...

JAR_FILE=$(dirname "$0")/../target/s3tool-0.0.3-SNAPSHOT.jar
 
BUCKET_NAME=$1 

mkdir -p bucket 
java -jar $JAR_FILE --command=createBucket --accessKey=$IAM_S3_ADMIN_ACCESS_KEY --secretKey=$IAM_S3_ADMIN_SECRET_KEY --bucketName=$BUCKET_NAME > bucket/bucket-name.properties
java -jar $JAR_FILE --command=genEncryptionKeyPair > bucket/encryption-pair.properties
java -jar $JAR_FILE --command=genSigningKeyPair > bucket/signing-pair.properties
