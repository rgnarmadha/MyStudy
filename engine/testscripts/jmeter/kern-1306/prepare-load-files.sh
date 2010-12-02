#!/bin/sh

if [ ! -d TestContent ]
then
   echo "grabbing files from NYU"
   curl https://files.nyu.edu/maw1/public/kern-1306/TestContent.zip > TestContent.zip
   curl https://files.nyu.edu/maw1/public/kern-1306/file-uploads.csv > file-uploads.csv
   unzip TestContent.zip
   rm TestContent.zip
else
  echo "If you really want to refresh the Test Content, remove the TestContent directory and rerun this script."
fi
