===============================
org.sakaiproject.nakamura.ojdbc
===============================
-----------
Description
-----------
This bundle is for providing an Oracle JDBC for jackrabbit clustering.

There are 2 ways to build this bundle.

-------------
Build Options
-------------
Build with Local Oracle JDBC File
---------------------------------
Copy the ojdbc14.jar to this directory and run

  mvn clean install


Build with Oracle JDBC File in Maven Repo
-----------------------------------------
The jdbc file can be copied manually or the installed using the following maven command:

  mvn install:install-file -Dfile=path-to-your-artifact-jar \
                           -DgroupId=oracle \
                           -DartifactId=oracle.jdbc \
                           -Dversion=0.1 \
                           -Dpackaging=jar

Once the Oracle JDBC file is in a maven repo, run 

  mvn clean install
