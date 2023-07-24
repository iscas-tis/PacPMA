#!/bin/bash

mvn clean package
cp target/pacpma-0.0.1-SNAPSHOT-jar-with-dependencies.jar pacpma.jar
