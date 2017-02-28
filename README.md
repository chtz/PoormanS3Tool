# PoormanS3Tool

[ ![Codeship Status for chtz/PoormanS3Tool](https://codeship.com/projects/99154270-b4b1-0133-4775-3e023a4cadff/status?branch=master)](https://codeship.com/projects/133982)

Download [s3tool-0.0.2-SNAPSHOT.jar](https://s3-eu-west-1.amazonaws.com/www.opensource.p.iraten.ch/s3tool-0.0.2-SNAPSHOT.jar) (built by Codeship)

## End-to-End Scenario

Company A -> Released Software Artifacts -(sign & encrypt)-> s3://bucket/ -(decrypt & verify)-> Company B

## Up Sync

Local Directory/* -(for each file)-> -(if new or newer)-> -(sign)-> -(encrypt)-> s3://bucket/*

## Down Sync

s3://bucket/* -(for each file)-> -(if new or newer)-> -(decrypt)-> -(verify)-> Local Directory/

# Samples

## Up Sync - Upload new(er) files in a local directory to S3

```
cat application-uploader.properties 
```

```
bucketName=cascloud2017
accessKey=AKIAJ6...
secretKey=69xEKxamT...
encryptPublicKey=MIICIjAN...
signPrivateKey=MIIJQwIBAD...
command=upSync
directory=casOut
```

```
java -jar s3tool-0.0.2-SNAPSHOT.jar --spring.profiles.active=uploader
```

## Down Sync - Download new(er) files from S3 to a local directory

```
cat application-downloader.properties
```

``` 
bucketName=cascloud2017
accessKey=AKIAJZ...
secretKey=DKPJ3e32x...
decryptPrivateKey=MIIJQwIBA...
verifyPublicKey=MIICIjANBgkq...
command=downSync
directory=casIn
```

```
java -jar s3tool-0.0.2-SNAPSHOT.jar --spring.profiles.active=downloader
```

## Admin tasks

### 1) Create bucket, read-only user, read-write user, encryption key pair and signing key pair

```
export IAM_S3_ADMIN_ACCESS_KEY=...
export IAM_S3_ADMIN_SECRET_KEY=... 
java -jar s3tool-0.0.2-SNAPSHOT.jar --command=createBucket --accessKey=$IAM_S3_ADMIN_ACCESS_KEY --secretKey=$IAM_S3_ADMIN_SECRET_KEY --region=eu-west-1 --bucketName=cascloud2017 > bucket.properties
java -jar s3tool-0.0.2-SNAPSHOT.jar --command=createUser --accessKey=$IAM_S3_ADMIN_ACCESS_KEY --secretKey=$IAM_S3_ADMIN_SECRET_KEY --userName=cascloud2017-rw --bucketName=cascloud2017 --readOnly=false > writer.properties
java -jar s3tool-0.0.2-SNAPSHOT.jar --command=createUser --accessKey=$IAM_S3_ADMIN_ACCESS_KEY --secretKey=$IAM_S3_ADMIN_SECRET_KEY --userName=cascloud2017-ro --bucketName=cascloud2017 --readOnly=true > reader.properties
java -jar s3tool-0.0.2-SNAPSHOT.jar --command=genEncryptionKeyPair > encryption-pair.properties
java -jar s3tool-0.0.2-SNAPSHOT.jar --command=genSigningKeyPair > signing-pair.properties
```

### 2) Create configuration file and script for "up sync" role 

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

### 3) Create configuration file and script for "down sync" role

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
