#!/bin/bash
# Treat unset variables as an error when performing parameter expansion
set -o nounset
# Exit immediately if a simple command exits with a non-zero status
set -o errexit

if [ ! -d ~/logs ]
then
	mkdir ~/logs
fi

rm -rf ~/logs/*
date > ~/logs/sakai3-build.log.txt
/home/hybrid/nightly.sh >> ~/logs/sakai3-build.log.txt 2>&1
date >> ~/logs/sakai3-build.log.txt
