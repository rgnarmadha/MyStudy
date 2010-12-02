
These are the Consumer classes for the Sakai Events. These listen for the Sakai messages on the network and as soon as they recieve the message,
they process it and store it into the Cassandra database.

Requirements:
AMQ broker running on port 61616 of the host machine specified by 'hostname' in the SakaiConsumer.java

Following are the directions about how to configure things before usage:

- Copy the src folder into a local folder on your machine.
- Run the command 'mvn clean install' from the terminal.
- Start the Cassandra server
- Run the Consumer by 'mvn exec:java -Dexec.mainClass='amq.SakaiConsumer'

Procedure:

- The SakaiConsumer class connects to the AMQ broker and listens for messages on the SAMPLE_QUEUE destination.
- The SAMPLE_QUEUE destination is defined in the Sakai OsgiJmsBridge.java (refer to the patch in Cassandra folder)
- The special destination has been made for now so as to recieve all messages only from the Sakai instance. Otherwise
  the consumer will take messages from other application running o the AMQ broker. This destination can be eliminated
  once the essential Sakai destinations are decided which will be unique to the Sakai instance from other applications.
- These messages once recieved in the Listener.java are processed to obtain individual properties.
- These properties are then stored in the Cassandra database. It calls the Base.java for establishing connection with Cassandra.


