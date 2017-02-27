# PoormanS3Tool

A little helper to encode+upload and download+decode files to/from S3.

[ ![Codeship Status for chtz/PoormanS3Tool](https://codeship.com/projects/99154270-b4b1-0133-4775-3e023a4cadff/status?branch=master)](https://codeship.com/projects/133982)

Download [s3tool-0.0.2-SNAPSHOT.jar](https://s3-eu-west-1.amazonaws.com/www.opensource.p.iraten.ch/s3tool-0.0.2-SNAPSHOT.jar) (built by Codeship)

# Samples

## Pre-conditions

```
export IAM_S3_ADMIN_ACCESS_KEY=...
export IAM_S3_ADMIN_SECRET_KEY=... 
```

## Create bucket, RO/RW users (S3 access keys) and key pairs

```
java -jar s3tool-0.0.2-SNAPSHOT.jar --command=createBucket --accessKey=$IAM_S3_ADMIN_ACCESS_KEY --secretKey=$IAM_S3_ADMIN_SECRET_KEY --region=eu-west-1 --bucketName=cascloud2017 > bucket.properties
java -jar s3tool-0.0.2-SNAPSHOT.jar --command=createUser --accessKey=$IAM_S3_ADMIN_ACCESS_KEY --secretKey=$IAM_S3_ADMIN_SECRET_KEY --userName=cascloud2017-rw --bucketName=cascloud2017 --readOnly=false > writer.properties
java -jar s3tool-0.0.2-SNAPSHOT.jar --command=createUser --accessKey=$IAM_S3_ADMIN_ACCESS_KEY --secretKey=$IAM_S3_ADMIN_SECRET_KEY --userName=cascloud2017-ro --bucketName=cascloud2017 --readOnly=true > reader.properties
java -jar s3tool-0.0.2-SNAPSHOT.jar --command=genEncryptionKeyPair > encryption-pair.properties
java -jar s3tool-0.0.2-SNAPSHOT.jar --command=genSigningKeyPair > signing-pair.properties
```

# Create configuration file and script for uploader role/user 

```
cp bucket.properties application-uploader.properties
cat writer.properties >> application-uploader.properties
cat encryption-pair.properties | grep encryptPublicKey >> application-uploader.properties
cat signing-pair.properties | grep signPrivateKey >> application-uploader.properties
echo command=upSync >> application-uploader.properties
echo directory=casOut >> application-uploader.properties

echo java -jar s3tool-0.0.2-SNAPSHOT.jar --spring.profiles.active=uploader > upload.sh
chmod +x upload.sh
```

# Create configuration file and script for downloader role/user

```
cp bucket.properties application-downloader.properties
cat reader.properties >> application-downloader.properties
cat encryption-pair.properties | grep decryptPrivateKey >> application-downloader.properties
cat signing-pair.properties | grep verifyPublicKey >> application-downloader.properties
echo command=downSync >> application-downloader.properties
echo directory=casIn >> application-downloader.properties

echo java -jar s3tool-0.0.2-SNAPSHOT.jar --spring.profiles.active=downloader > download.sh
chmod +x download.sh
```

# Test upload and download

```
mkdir -p casOut
echo test > casOut/test.txt
./upload.sh 
```

```
uploaded new casOut/test.txt to s3://cascloud2017/test.txt
```

```
mkdir -p casIn
./download.sh
```

```
downloaded new s3://cascloud2017/test.txt to casIn/test.txt
```
