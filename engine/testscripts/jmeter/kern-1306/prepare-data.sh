#!/bin/bash
# create a netids file
# create netid pairs file
# create user tags file
# create content uploads file
# add users to nakamura
# load-messages.jmx
# load-files.jmx
# load-tags.jmx



# sanity checks
#  checking that all the required programs are available.

source ./utils/sanity_check_tools.sh
SANE=$?

if [[ $SANE -eq 1 ]]
then
 echo "Environment bad, exiting"
 exit 1
fi

## next check that we at least have a number of users to create

[[ -n "$1" ]] || { echo "Usage: prepare-data.sh <number of users to create> <absolute path to content directory for uploads>"; exit 1 ; }

NUMUSERS=$1

# test to see if we have a content directory, and if it exists

[[ -n "$2" ]] || { echo "Usage: prepare-data.sh <number of users to create> <absolute path to content directory for uploads>"; exit 1 ; }


if [ ! -d $2 ]
then
 echo "Unable to find directory $2" ; 
 exit 1
fi

CONTENTDIR=$2

NUMMESSAGES=$(( $NUMUSERS * 100 ))
NUMTAGS=$(( $NUMUSERS * 3 ))

echo "creating $NUMUSERS netIDs in ../netids01.csv"

if [ -e ../netidusers.pl ]
then
  perl ../netidusers.pl $NUMUSERS > netids01.csv
else
 echo "Unable to find ../netidusers.pl"
 echo "exiting"
 exit 1
fi

echo "creating $NUMMESSAGES message recipients in recipients.csv"
if [ -e generate-recipient-list.rb ]
then
 ruby generate-recipient-list.rb $NUMMESSAGES > recipients.csv
else
 echo "unable to find generate-recipient-l9ist.rb"
 echo "exiting"
 exit 1
fi

echo "creating $NUMTAGS user profile tags in user-tags.csv"
if [ -e generate-user-tags.rb ]
then 
 ruby generate-user-tags.rb $NUMTAGS > user-tags.csv
else
 echo "unable to find generate-user-tags.rb"
 echo "exiting"
 exit 1
fi

echo "creating list of content from $CONTENTDIR in content.txt"
if [ -d $CONTENTDIR ]
then
 find $CONTENTDIR -type f > content.txt
else
 echo "unable to find $CONTENTDIR"
 exit 1
fi

echo "creating ID,FILEPATH,MIMETYPE for file uploads in file-uploads.csv"
if [ -e generate-file-upload-list.rb ]
then
 ruby generate-file-upload-list.rb > file-uploads.csv
else 
 echo "unable to find generate-file-upload-list.rb"
 exit 1
fi

