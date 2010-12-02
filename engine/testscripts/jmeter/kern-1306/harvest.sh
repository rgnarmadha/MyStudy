#!/bin/bash
# usage: harvest.sh <destination path> <how many MBytes to harvest> <html|msword|pdf|ppt|rss|txt|xls> <Yahoo! appid>
DEST=$1
TARGETSIZE=$2
FORMAT=$3
APPID=$4
MYPATH=`pwd $0`
SIZE=`du -ms $DEST | cut -f1`
pushd $DEST > /dev/null 2>&1
while [  $SIZE -lt $TARGETSIZE ]; do
    `ruby $MYPATH/yahoo-search.rb $DEST $FORMAT $APPID`
    let SIZE=`du -ms . | cut -f1`
    echo Harvest directory is $SIZE MBytes
done
popd > /dev/null 2>&1
