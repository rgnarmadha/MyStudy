#!/bin/bash

# ------------------------------------------------------
#
# Build script, to generate *NIX tarballs, and Windows
# friendly zip files.
#
# ------------------------------------------------------



# ---------------------------
# User customisable ENV VARs
# ---------------------------

K2_version="sakai-k2-0.10-SNAPSHOT-binary"
mvn_bin="/usr/bin/mvn"
tar_bin="/usr/bin/tar"
zip_bin="/usr/bin/zip"
java_bin="`which java`"

#-------------------------------
#   DO NOT EDIT BELOW THIS LINE
# -------------------------------

export K2_version
export mvn_bin
export tar_bin
export zip_bin
export java_bin

# -----------------------------------
# Define ENV VARs used in the script
# -----------------------------------


export target=release
export build_root=`pwd`
export MAVEN_OPTS="-Xms256m -Xmx800m -XX:PermSize=256m -XX:NewSize=64m"

# -----------------------
# Check for requirements
# -----------------------

clear
echo "Checking for dependencies"

if [ -n "${K2_version:+x}" ]
 then
  echo "K2_version ENV VAR set correctly"
 else
  echo "You dont seem to have set the 'K2_version' ENV VAR in this script. Please check and run this script again.  Exiting."
 exit
fi

if [ -f "$java_bin" ] 
 then 
  echo "Java found..."
 else 
  echo "Java not found.  Exiting.  Check that you have java installed correctly, you may need to edit the 'java_bin' ENV VAR in this script."
 exit
fi

if [ -f "$zip_bin" ]
 then 
  echo "zip found..."
 else
  echo "zip not found.  Exiting.  Check that you have zip installed correctly, you may need to edit the 'zip_bin' ENV VAR in this script."
 exit
fi

if [ -f "$tar_bin" ]
 then 
  echo "tar found..."
 else
  echo "tar not found.  Exiting.  Check that you have tar installed correctly, you may need to edit the 'tar_bin' ENV VAR in this script."
 exit
fi


if [ -f "$mvn_bin" ]
 then 
  echo "maven found..."
 else
  echo "maven not found.  Exiting.  Check that you have java installed correctly, you may need to edit the 'mvn_bin' ENV VAR in this script."
 exit
fi


if [ -d "$target" ]
 then 
  clear
  echo "Sorry we cannot proceed as you already seem to have a $target folder.  Please check manually and resolve."
  echo "This script will create a folder called ' $target ' in this folder."
  echo ""  
  echo ""
  echo ""
 exit
 else
  echo "' $target ' Does not seem to exist.  Good."
fi

# ------------------------
# Create folders required 
# ------------------------

echo ""
echo ""
echo ""
echo "Creating requisite folders for the build folder"
mkdir ${target} > /dev/null
mkdir ${target}/${K2_version} > /dev/null 
mkdir ${target}/${K2_version}/lib > /dev/null
mkdir ${target}/${K2_version}/src > /dev/null 
echo "Done ..."


echo ""
echo ""
echo "Copying files ready for the tar ball"
cp ${build_root}/../app/target/*SNAPSHOT.jar ${build_root}/${target}/${K2_version}/lib > /dev/null 2>&1
cp ${build_root}/../app/target/*sources.jar ${build_root}/${target}/${K2_version}/src > /dev/null 2>&1
cp -r ${build_root}/app/src/main/resources/scripts/bin-dist/ ${build_root}/${target}/${K2_version}/ > /dev/null 2>&1
cp ${build_root}/../app/LICENSE ${build_root}/${target}/${K2_version}/ > /dev/null 2>&1
cp ${build_root}/../app/NOTICE ${build_root}/${target}/${K2_version}/ > /dev/null 2>&1
echo "Done ..."

# Generate README.txt in the root of ${build_root}/${target}/${K2_version}

echo ""
echo ""
echo "Generating readme ..."
${build_root}/010_gen_README.sh > /dev/null 2>&1 
echo "Done ..."


pushd ${build_root}/${target}/ > /dev/null 2>&1

echo ""
echo "Creating tarball"
${tar_bin} cvzf ${K2_version}.tar.gz ${K2_version} > /dev/null 2>&1
echo "Done ..."
echo ""
echo "Creating zip file"
${zip_bin} -r ${K2_version}.zip ${K2_version} > /dev/null 2>&1
echo "Done ..."

popd > /dev/null 2>&1



