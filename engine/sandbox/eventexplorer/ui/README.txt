
This is the view page for Sakai Event Explorer project users.

Requirements:
Tomcat 6.0

Following are the directions about how to configure things before usage:

- Place the cass folder from the classes folder into the local classes directory of the WEB-INF folder. The User.java and the Base.java are
the .java versions of the classes in the cass package.
- Add the jar files from lib to the lib folder of tomcat and WEB-INF to provide classes for usage to JSP and the Base class.
- Add simile.jsp to the root folder of the tomcat.
- Currently the default host for Cassandra installation is set to 'localhost' in the Base.java. You can either change the host name of local 
  machine to 'localhost' or recompile the Base.java after making the changes to the hostname in the class. (For easy change, an eclipse project
  is added for quick modification. Add it to your workspace and edit the hostname and then add the compiled class back to WEB-INF/classes/cass folder.

  
  Note: Before running make sure you have completed the data addition to cassandra otherwise the timeline will be empty.