# PoormanS3Tool

A little helper to encode+upload and download+decode files to/from S3.

[ ![Codeship Status for chtz/PoormanS3Tool](https://codeship.com/projects/99154270-b4b1-0133-4775-3e023a4cadff/status?branch=master)](https://codeship.com/projects/133982)

Download [s3tool-0.0.2-SNAPSHOT.jar](https://s3-eu-west-1.amazonaws.com/www.opensource.p.iraten.ch/s3tool-0.0.2-SNAPSHOT.jar) (built by Codeship)

# Samples (currently slightly OUT OF DATE)

## Create bucket

```
$ java -jar target/s3tool-0.0.2-SNAPSHOT.jar --command=createBucket --accessKey=AKI<S3FullAccessKey> --secretKey=Lmr<S3FullAccessKey> --bucketName=poormans3test20170225a --region=eu-west-1
```

```
bucketName=poormans3test20170225a
```

## Create read/write user

```
$ java -jar target/s3tool-0.0.2-SNAPSHOT.jar --command=createUser --accessKey=AKI<IAMFullAccessKey> --secretKey=v/r<IAMFullAccessKey> --userName=poormans3test20170225a-rwuser --bucketName=poormans3test20170225a --readOnly=false
```

```
accessKey=AKI<Generated>
secretKey=g6t<Generated>
```

## Create AES key

```
$ java -jar target/s3tool-0.0.2-SNAPSHOT.jar --command=genKey
```

```
aesKey=ePE<Generated>
```

# Sync local dir to s3 (create & delete files in s3)

```
$ mkdir testOut
$ echo hallo > testOut/halli.txt
$ echo hallo > testOut/hallo.txt
$ java -jar target/s3tool-0.0.2-SNAPSHOT.jar --command=upSync --accessKey=AKI<Generated> --secretKey=g6t<Generated> --bucketName=poormans3test20170225a --directory=testOut --aesKey=ePE<Generated>
```

```
uploaded new testOut/halli.txt to s3://poormans3test20170225a/halli.txt
uploaded new testOut/hallo.txt to s3://poormans3test20170225a/hallo.txt
```

```
$ echo hallo > testOut/hello.txt
$ rm testOut/halli.txt 
$ java -jar target/s3tool-0.0.2-SNAPSHOT.jar --command=upSync --accessKey=AKI<Generated> --secretKey=g6t<Generated> --bucketName=poormans3test20170225a --directory=testOut --aesKey=ePE<Generated>
```

```
ignored older testOut/hallo.txt
uploaded new testOut/hello.txt to s3://poormans3test20170225a/hello.txt
deleted s3://poormans3test20170225a/halli.txt
```

## Sync s3 to local dir (create & delete local files)

```
$ mkdir testIn
$ java -jar target/s3tool-0.0.2-SNAPSHOT.jar --command=downSync --accessKey=AKI<Generated> --secretKey=g6t<Generated> --bucketName=poormans3test20170225a --directory=testIn --aesKey=ePE<Generated>
```

```
downloaded new s3://poormans3test20170225a/hallo.txt to testIn/hallo.txt
downloaded new s3://poormans3test20170225a/hello.txt to testIn/hello.txt
```

```
$ java -jar target/s3tool-0.0.2-SNAPSHOT.jar --command=downSync --accessKey=AKI<Generated> --secretKey=g6t<Generated> --bucketName=poormans3test20170225a --directory=testIn --aesKey=ePE<Generated>
```

```
ignored older s3://poormans3test20170225a/hallo.txt
ignored older s3://poormans3test20170225a/hello.txt
```
