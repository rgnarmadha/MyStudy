#!/bin/bash
#
# sanity check for tools we need.
#
# skipping jmeter for now, as there
# 

# add what you need to this list

for NEED in ruby perl curl java
do

  type -P $NEED &>/dev/null   || { echo "can't find $NEED"  >&2; 
                                   ERRORS=1; }
done


if [[ $ERRORS > 0 ]]
then
 echo "Fatal environment setup errors, exiting ${ERRORS}"
 exit 1
fi


