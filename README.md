# PoormanS3Tool

[ ![Codeship Status for chtz/PoormanS3Tool](https://codeship.com/projects/99154270-b4b1-0133-4775-3e023a4cadff/status?branch=master)](https://codeship.com/projects/133982)

Download [s3tool-0.0.2.jar](https://s3-eu-west-1.amazonaws.com/www.opensource.p.iraten.ch/s3tool-0.0.2.jar) (built by Codeship)

Download [s3tool-0.0.3-SNAPSHOT.jar](https://s3-eu-west-1.amazonaws.com/www.opensource.p.iraten.ch/s3tool-0.0.3-SNAPSHOT.jar) (built by Codeship)

## End-to-End Scenario

Company A -> Files -(up sync: sign & encrypt)-> s3://bucket/ -(down sync: decrypt & verify)-> Company B

After up sync: s3://bucket is identical with local directory (files added, files replaced, files removed in S3)

After down sync: local directory is identical with s3://bucket (files added, files replaced, files removed in local directory)

# Samples

## Up Sync

```
cat application.properties 
```

```
bucketName=samplebucket
accessKey=AKIAJ6...
secretKey=69xEKxamT...
encryptPublicKey=MIICIjAN...
signPrivateKey=MIIJQwIBAD...
command=upSync
directory=out
```

```
java -jar s3tool-0.0.2.jar
```

## Down Sync

```
cat application.properties
```

``` 
bucketName=samplebucket
accessKey=AKIAJZ...
secretKey=DKPJ3e32x...
decryptPrivateKey=MIIJQwIBA...
verifyPublicKey=MIICIjANBgkq...
command=downSync
directory=in
```

```
java -jar s3tool-0.0.2.jar
```

## Two-way Sync

```
cat application.properties
```

``` 
bucketName=samplebucket
accessKey=AKIAJZ...
secretKey=DKPJ3e32x...
decryptPrivateKey=MIIJQwIBA...
verifyPublicKey=MIICIjANBgkq...
encryptPublicKey=MIICIjAN...
signPrivateKey=MIIJQwIBAD...
command=sync
directory=sync
```

```
java -jar s3tool-0.0.2.jar
```

## Admin tasks

### 1) Create S3 bucket (and read-only IAM group and read/write IAM group), encryption RSA key pair and signing RSA key pair

```
export IAM_S3_ADMIN_ACCESS_KEY=...
export IAM_S3_ADMIN_SECRET_KEY=...
mkdir -p samplebucket
cd samplebucket
../scripts/create_bucket.sh samplebucket
```

### 2a) Create up sync read/write IAM user, configuration file and script

```
export IAM_S3_ADMIN_ACCESS_KEY=...
export IAM_S3_ADMIN_SECRET_KEY=...
cd samplebucket
../scripts/create_uploader_config.sh samplebucket samplebucket-uploader
```

### 2b) Create down sync read-only IAM user, configuration file and script

```
export IAM_S3_ADMIN_ACCESS_KEY=...
export IAM_S3_ADMIN_SECRET_KEY=...
cd samplebucket
../scripts/create_downloader_config.sh samplebucket samplebucket-downloader
```

### 2c) Create two-way sync read/write IAM user, configuration file and script

```
export IAM_S3_ADMIN_ACCESS_KEY=...
export IAM_S3_ADMIN_SECRET_KEY=...
cd samplebucket
../scripts/create_sync_config.sh samplebucket samplebucket-syncer
```

### 3) Delete bucket (and all data), IAM (read/write and read-only) groups and all (read/wite and read-only) IAM users

```
export IAM_S3_ADMIN_ACCESS_KEY=...
export IAM_S3_ADMIN_SECRET_KEY=...
cd samplebucket
../scripts/delete_bucket.sh samplebucket
```
