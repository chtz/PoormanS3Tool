# PoormanS3Tool

A little helper to encode+upload/download+decode files to/from S3.

[ ![Codeship Status for chtz/PoormanS3Tool](https://codeship.com/projects/99154270-b4b1-0133-4775-3e023a4cadff/status?branch=master)](https://codeship.com/projects/133982)

Download [s3tool-0.0.1-SNAPSHOT.jar](https://s3-eu-west-1.amazonaws.com/www.opensource.p.iraten.ch/s3tool-0.0.1-SNAPSHOT.jar) (built by Codeship)

BTW., USE AT YOUR OWN RISK!

# Samples

Generate AES key:

    export AES_KEY=$(java -jar target/s3tool-0.0.1-SNAPSHOT.jar --command=genKey)

Encode file and upload (S3 putObject) to bucket:

    java -jar target/s3tool-0.0.1-SNAPSHOT.jar \
    	--accessKey=XXX \
    	--secretKey=XXX \
    	--bucketName=xxx.swreleases \
    	--key=s3tool-0.0.1-SNAPSHOT.jar \
    	--file=target/s3tool-0.0.1-SNAPSHOT.jar \
    	--contentType=application/octet-stream \
    	--command=putObject \
    	--aesKey=$AES_KEY
		
Download (S3 getObject) file from bucket and decode (use HTTP endpoint to make certain corporate firewalls happy):		
		
	java -jar target/s3tool-0.0.1-SNAPSHOT.jar \
	    --accessKey=XXX \
	    --secretKey=XXX \
	    --bucketName=xxx.swreleases \
	    --endpoint='http://s3-eu-west-1.amazonaws.com' \
	    --key=s3tool-0.0.1-SNAPSHOT.jar \
	    --file=testdata/s3tool-0.0.1-SNAPSHOT.jar \
	    --contentType=application/octet-stream \
	    --command=getObject \
	    --aesKey=$AES_KEY
	
Upload (S3 putObject) unencrypted file to (website hosting enabled) S3 bucket: 	
	
	java -jar target/s3tool-0.0.1-SNAPSHOT.jar \
		--accessKey=XXX \
		--secretKey=XXX \
		--bucketName=www.swreleases.foo \
		--key=s3tool-0.0.1-SNAPSHOT.jar \
		--keyPublic=true \
		--file=target/s3tool-0.0.1-SNAPSHOT.jar \
		--contentType=application/octet-stream \
		--command=putObject
		
Download (HTTP GET) unencrypted file from (website hosting enabled) S3 bucket:	
		
	java -jar target/s3tool-0.0.1-SNAPSHOT.jar \
		--url='http://www.swreleases.foo/s3tool-0.0.1-SNAPSHOT.jar' \
		--file='testdata/s3tool-0.0.1-SNAPSHOT.jar.pub' \
		--command=download
		
Encode file (AES):		
		
	java -jar target/s3tool-0.0.1-SNAPSHOT.jar \
	    --file=testdata/s3tool-0.0.1-SNAPSHOT.jar \
	    --file2=testdata/s3tool-0.0.1-SNAPSHOT.jar.enc \
	    --command=encode \
	    --aesKey=$AES_KEY
	
Decode file (AES):
	
	java -jar target/s3tool-0.0.1-SNAPSHOT.jar \
	    --file=testdata/s3tool-0.0.1-SNAPSHOT.jar.enc \
	    --file2=testdata/s3tool-0.0.1-SNAPSHOT.jar.dec \
	    --command=decode \
	    --aesKey=$AES_KEY

Upload all new jar files in directory 'relX' to S3 bucket (prepend keys with 'releaseX/'):

	java -jar target/s3tool-0.0.1-SNAPSHOT.jar \
		--accessKey=XXX \
		--secretKey=XXX \
		--bucketName=xxx.swreleases \
		--command=upSync \
		--directory=relX \
		--filenamePattern='.*\.jar' \
		--prefix=releaseX/
		
Download all new jar files (with key prefix 'releaseX/') from S3 bucket to directory 'relX':
		
	java -jar target/s3tool-0.0.1-SNAPSHOT.jar \
		--accessKey=XXX \
		--secretKey=XXX \
		--bucketName=xxx.swreleases \
		--command=downSync \
		--directory=relX \
		--prefix=releaseX/
