Apache Felix File Install with K2 and Sling
-------------------------------------------

Felix FileInstall links OSGi bundle management to a directory (or
set of directories) in the file system. When a new JAR is added to the
directory, it loads the bundle into the container and starts it. When an
existing JAR file is updated, it refreshes the bundle. For more, see:

http://felix.apache.org/site/apache-felix-file-install.html

Possible uses include:

* Clean administrative separation of the K2/Sling core from locally
maintained components such as integration libraries.

* Easy inclusion of loosely-coupled add-on components outside the
standard K2/Sling build and launchpad, using a single mechanism
whether K2/Sling is currently running or not. (For example, in my
development environment I keep the org.apache.sling.extensions.explorer
sample installed to help me find my way around new builds of K2/Sling.)

* Faster development by supporting a WAR-style refresh-on-redeploy
model.

This directory provides a simple demonstration of these features.

QUICK START

# Removes all traces of any current K2/Sling installation, and
# stores a simple add-on bundle in the "working-fileinstall"
# directory.
mvn clean install

# Starts Sling.
cd slingrunner
mvn -Dsling.start verify

# Go to http://localhost:8080/hellofromfileinstall/index.html
# and take a look...

# Modify the add-on bundle:
cd ../hellofromfileinstall
mvn -Dsample.message="Something else" clean install

# Return to http://localhost:8080/hellofromfileinstall/index.html
# for another look...

# Stops Sling.
cd ../slingrunner
mvn -Dsling.stop verify

# Cleans out current K2/Sling installation and starts fresh...
mvn -Dsling.start clean verify

# Return to http://localhost:8080/hellofromfileinstall/index.html
# and find your outside-of-K2 component still there.
