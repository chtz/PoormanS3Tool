#!/bin/bash
(cd ../../.. && mvn clean install)
cp ../../../target/s3tool-0.0.1-SNAPSHOT.jar .
sudo docker build -t chtz/s3tool .