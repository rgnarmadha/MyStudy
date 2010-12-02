#!/bin/sh
#
#  This scipt is run on the AWS instance to configure the instance ready to run.


NAK_VERSION=$1
if [[ ! -d jdk1.5.0_22 ]]
then
  sh jdk-1_5_0_22-linux-i586.bin
fi
rm java
ln -s jdk1.5.0_22 java

cat << EOF > start.sh
#!/bin/sh
export JAVA_HOME=$HOME/java
export PATH=$PATH:$HOME/java/bin

running=\`ps aux | grep org.sakaiproject.nakamura.app-0.5 | grep -v grep | cut -c5-15\`
if [[ a\$running != a ]]
then 
  ps aux | grep org.sakaiproject.nakamura.app-0.5 | grep -v grep
  echo Instance already running
else
  echo Starting new instance....
  java -Xmx1024m -XX:PermSize=256m -server -jar org.sakaiproject.nakamura.app-${NAK_VERSION}.jar 1> run.log 2>&1 &    
  echo Waiting for 5...  
  sleep 5
  cat run.log
  echo .... 
fi
EOF

cat << EOF > stop.sh
#!/bin/sh
running=\`ps aux | grep org.sakaiproject.nakamura.app-0.5 | grep -v grep | cut -c5-15\`
if [[ a\$running != a ]]
then
    ps aux | grep org.sakaiproject.nakamura.app-0.5 | grep -v grep
    echo Killing Process $running 
    kill \$running
    echo Waiting for 10...  
    sleep 10
    ps aux | grep org.sakaiproject.nakamura.app-0.5 | grep -v grep
else
    echo Not running
fi

EOF


chmod 700 start.sh
chmod 700 stop.sh

