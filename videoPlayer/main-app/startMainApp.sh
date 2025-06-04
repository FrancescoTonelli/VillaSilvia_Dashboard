#!/bin/bash

mvn  package

cd target/distribution
./run.sh