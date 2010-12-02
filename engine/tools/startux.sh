#!/bin/bash
#
# Use this script to map the UX files easily
#
# Usage:
# ./startux.sh -d /opt/sakai/3akai-ux/dev -w /opt/sakai/3akai-ux/devwidgets/
# Parameters
#  -d : Path to where your /dev ux folder is located
#  -w : Path to where your /devwidgets ux folder is located
#

JAR=''
DEV=''
DEVWIDGETS=''

while getopts ":j:d:w:" opt; do
  case $opt in
    d)
      DEV="$OPTARG"
      ;;
    w)
      DEVWIDGETS=$OPTARG
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      exit 1
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done

# Set /dev
curl -d"provider.roots=/dev" -d"provider.file=$DEV" -d"propertylist=provider.roots,provider.file"  -d"apply=true" -d"factoryPid=org.apache.sling.fsprovider.internal.FsResourceProvider" -d"action=ajaxConfigManager" -g http://admin:admin@localhost:8080/system/console/configMgr/[Temporary%20PID%20replaced%20by%20real%20PID%20upon%20save]
echo "/dev set."

# Set /devwidgets
curl -d"provider.roots=/devwidgets" -d"provider.file=$DEVWIDGETS" -d"propertylist=provider.roots,provider.file"  -d"apply=true" -d"factoryPid=org.apache.sling.fsprovider.internal.FsResourceProvider" -d"action=ajaxConfigManager" -g http://admin:admin@localhost:8080/system/console/configMgr/[Temporary%20PID%20replaced%20by%20real%20PID%20upon%20save]
echo "/devwidgets set."
