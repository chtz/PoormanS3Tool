#!/bin/bash

#Caution: SAMPLE SCRIPT - NO INPUT VALIDATION!!!

#Pre-conditions:
#export IAM_S3_ADMIN_ACCESS_KEY=...
#export IAM_S3_ADMIN_SECRET_KEY=...

JAR_FILE=$(dirname "$0")/../target/s3tool-0.0.3-SNAPSHOT.jar
 
BUCKET_NAME=$1
USER_NAME=$2 

mkdir -p $USER_NAME
mkdir -p $USER_NAME/sync
 
java -jar $JAR_FILE --command=createUser --accessKey=$IAM_S3_ADMIN_ACCESS_KEY --secretKey=$IAM_S3_ADMIN_SECRET_KEY --userName=$USER_NAME --bucketName=$BUCKET_NAME --readOnly=false > $USER_NAME/application.properties

cat bucket/bucket-name.properties >> $USER_NAME/application.properties
cat bucket/encryption-pair.properties >> $USER_NAME/application.properties
cat bucket/signing-pair.properties >> $USER_NAME/application.properties
echo command=sync >> $USER_NAME/application.properties
echo directory=sync >> $USER_NAME/application.properties

cp $JAR_FILE $USER_NAME/
 
echo java -jar s3tool-0.0.3-SNAPSHOT.jar > $USER_NAME/sync.sh
chmod +x $USER_NAME/sync.sh
