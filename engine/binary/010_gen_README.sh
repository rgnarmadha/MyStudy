#!/bin/bash

# This script will generate the README file in the root of the tarball.

echo "This is a binary distribution of Sakai K2 $K2_version based on Sling.

To use, on wndows Start->Run run.bat
On Linux, OSX, Unix execute run.sh

then point your browser to http://localhost:8080/

The admin username is admin and password is admin

You will need about 128M of memory unless you hammer the server that starts" > ${build_root}/${target}/${K2_version}/README.txt
