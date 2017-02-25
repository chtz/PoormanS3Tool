[ ![Codeship Status for chtz/PoormanS3Tool](https://codeship.com/projects/99154270-b4b1-0133-4775-3e023a4cadff/status?branch=master)](https://codeship.com/projects/133982)

Download [s3tool-0.0.1-SNAPSHOT.jar](https://s3-eu-west-1.amazonaws.com/www.opensource.p.iraten.ch/s3tool-0.0.1-SNAPSHOT.jar) (built by Codeship) *FIXME NEW VERSION*

BTW., USE AT YOUR OWN RISK!

## create bucket

```
$ java -jar target/s3tool-0.0.2-SNAPSHOT.jar --command=createBucket --accessKey=AKIXXX --secretKey=LmrXXX --bucketName=poormans3test20170225a --region=eu-west-1
```

```
bucketName=poormans3test20170225a
```

## create read/write user

```
$ java -jar target/s3tool-0.0.2-SNAPSHOT.jar --command=createUser --accessKey=AKIYYY --secretKey=v/rYYY --userName=poormans3test20170225a-rwuser --bucketName=poormans3test20170225a --readOnly=false
```

```
accessKey=AKIZZZ
secretKey=g6tZZZ
```

## create AES key

```
$ java -jar target/s3tool-0.0.2-SNAPSHOT.jar --command=genKey
```

```
aesKey=ePEZZZ
```

# sync local dir to s3 (create & delete files in s3)

```
$ mkdir testOut
$ echo hallo > testOut/halli.txt
$ echo hallo > testOut/hallo.txt
$ java -jar target/s3tool-0.0.2-SNAPSHOT.jar --command=upSync --accessKey=AKIZZZ --secretKey=g6tZZZ --bucketName=poormans3test20170225a --directory=testOut --aesKey=ePEZZZ
```

```
uploaded new testOut/halli.txt to halli.txt
uploaded new testOut/hallo.txt to hallo.txt
```

```
$ echo hallo > testOut/hello.txt
$ rm testOut/halli.txt 
$ java -jar target/s3tool-0.0.2-SNAPSHOT.jar --command=upSync --accessKey=AKIZZZ --secretKey=g6tZZZ --bucketName=poormans3test20170225a --directory=testOut --aesKey=ePEZZZ
```

```
ignored older testOut/hallo.txt
uploaded new testOut/hello.txt to hello.txt
deleted halli.txt
```

## sync s3 to local dir (create & delete local files)

```
$ mkdir testIn
$ java -jar target/s3tool-0.0.2-SNAPSHOT.jar --command=downSync --accessKey=AKIZZZ --secretKey=g6tZZZ --bucketName=poormans3test20170225a --directory=testIn --aesKey=ePEZZZ
```

```
downloaded new hallo.txt to testIn/hallo.txt
downloaded new hello.txt to testIn/hello.txt
```

```
$ java -jar target/s3tool-0.0.2-SNAPSHOT.jar --command=downSync --accessKey=AKIZZZ --secretKey=g6tZZZ --bucketName=poormans3test20170225a --directory=testIn --aesKey=ePEZZZ
```

```
ignored older hallo.txt
ignored older hello.txt
```
