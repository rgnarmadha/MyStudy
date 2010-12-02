/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.events;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.apache.activemq.command.ActiveMQMessage;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.activemq.ConnectionFactoryService;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;

/**
 * Unit test for bridging events from OSGi to JMS.
 */
public class OsgiJmsBridgeTest {
  private Hashtable<Object, Object> compProps;
  private ComponentContext ctx;
  private ConnectionFactoryService connFactoryService;
  private ConnectionFactory connFactory;
  private Connection conn;
  private Session sess;
  private Topic topic;
  private MessageProducer prod;
  private Message message;
  private OsgiJmsBridge bridge;
  private Event event;
  private ClusterTrackingService clusterTrackingService;

  @Before
  public void setUp() {
    // construct the default component properties
    compProps = buildComponentProperties();

    // mock the context and expect a call to get the properties
    ctx = createMock(ComponentContext.class);
    expect(ctx.getProperties()).andReturn(compProps);

    // mock a connection factory
    connFactory = createMock(ConnectionFactory.class);

    // mock a connection factory service
    connFactoryService = createMock(ConnectionFactoryService.class);
    expect(connFactoryService.getDefaultPooledConnectionFactory()).andReturn(connFactory).anyTimes();
  }

  /**
   * Tests the default constructor. Nothing goes on in the default constructor
   * so this test is really just for test coverage completeness.
   */
  @Test
  public void testDefaultConstructor() {
    new OsgiJmsBridge();
  }


  /**
   * Test handling an event with full processing.
   *
   * @throws JMSException
   */
  @SuppressWarnings("rawtypes")
  @Test
  public void testHandleEvent() throws Exception {
    // setup to do full processing
    setUpFullProcess(true);
    
    clusterTrackingService = createMock(ClusterTrackingService.class);
    expect(clusterTrackingService.getCurrentServerId()).andReturn("CurrentServerID");
    // start the mocks
    replay(ctx, connFactory, connFactoryService, conn, sess, topic, prod, clusterTrackingService);

    // construct and send the message
    Dictionary<Object, Object> props = buildEventProperties();
    sendMessage(props);
    bridge.deactivate(ctx);

    // verify that all expected calls were made.
    verify(ctx, connFactory, conn, sess, topic, prod, clusterTrackingService);

    int namesCount = 0;
    Enumeration names = message.getPropertyNames();
    while (names.hasMoreElements()) {
      names.nextElement();
      namesCount++;
    }

    // there should be an entry for each property plus the name of the topics
    assertEquals(props.size() + 2, namesCount);
  }

  /**
   * Test handling an event with full processing.
   *
   * @throws JMSException
   */
  @SuppressWarnings("rawtypes")
  @Test
  public void testHandleEventExceptionClosing() throws Exception {
    // setup to do full processing
    setUpFullProcess(false);

    clusterTrackingService = createMock(ClusterTrackingService.class);
    expect(clusterTrackingService.getCurrentServerId()).andReturn("CurrentServerID");
    // expect to have exceptions when closing the session and connection
    //sess.close();
    //expectLastCall().andThrow(new JMSException("can't close session"));
    conn.close();
    expectLastCall().andThrow(new JMSException("can't close connection"));

    // start the mocks
    replay(ctx, connFactory, connFactoryService, conn, sess, topic, prod, clusterTrackingService);

    // construct and send the message
    Dictionary<Object, Object> props = buildEventProperties();
    sendMessage(props);
    bridge.deactivate(ctx);

    // verify that all expected calls were made.
    verify(ctx, connFactory, conn, sess, topic, prod, clusterTrackingService);

    int namesCount = 0;
    Enumeration names = message.getPropertyNames();
    while (names.hasMoreElements()) {
      names.nextElement();
      namesCount++;
    }

    // there should be an entry for each property plus the name of the topics
    assertEquals(props.size() + 2, namesCount);
  }

  @Test
  public void testJmsExceptionWhenCreatingConnection() throws Exception {
    // expect the connection factory to thrown an exception. this is the
    // earliest an exception can be thrown and causes extra checks in the
    // exception handling.
    connFactory = createMock(ConnectionFactory.class);
    clusterTrackingService = createMock(ClusterTrackingService.class);
    expect(clusterTrackingService.getCurrentServerId()).andReturn("CurrentServerID");

    connFactoryService = createMock(ConnectionFactoryService.class);
    expect(connFactoryService.getDefaultPooledConnectionFactory()).andReturn(connFactory).anyTimes();

    expect(connFactory.createConnection()).andThrow(new JMSException("can't create connection"));

    // start the mocks
    replay(ctx, connFactoryService, connFactory, clusterTrackingService);

    // construct and send the message
    Dictionary<Object, Object> props = buildEventProperties();
    sendMessage(props);
    // should log the message, but not fail
    verify(ctx, connFactory, clusterTrackingService);
  }


  @Test
  public void testJmsExceptionWhenCreatingTopic() throws JMSException {
    setUpConnection(true);
    clusterTrackingService = createMock(ClusterTrackingService.class);
    expect(clusterTrackingService.getCurrentServerId()).andReturn("CurrentServerID");

    // mock a session to be returned by the connection and expect it to throw an
    // exception. this causes extra checking to happen in the exception
    // handling.
    sess = createMock(Session.class);
    expect(conn.createSession(false, Session.AUTO_ACKNOWLEDGE)).andReturn(sess);
    sess.run();
    expectLastCall();
    sess.close();
    expectLastCall();

    // mock a destination as a topic from the session and expect it
    expect(sess.createMessage()).andThrow(new JMSException("can't create topic"));

    // start the mocks
    replay(ctx, connFactoryService, connFactory, conn, sess, clusterTrackingService);

    // construct and send the message
    Dictionary<Object, Object> props = buildEventProperties();
    sendMessage(props);
    bridge.deactivate(ctx);

    // verify that all expected calls were made.
    verify(ctx, conn, connFactory, clusterTrackingService);
  }

  /**
   * Constructs the bridge, activates it, constructs a message with 2 properties
   * and calls the bridge to handle it.
   */
  private void sendMessage(Dictionary<Object, Object> dict) {
    bridge = new OsgiJmsBridge(connFactoryService);
    bridge.clusterTrackingService = clusterTrackingService;
    bridge.activate(ctx);

    event = new Event("test-event", dict);
    bridge.handleEvent(event);
  }

  /**
   * Build a dictionary of properties to be used in sending an event. All types
   * of properties that are checked for are represented.
   *
   * @return
   */
  private Dictionary<Object, Object> buildEventProperties() {
    Hashtable<Object, Object> dict = new Hashtable<Object, Object>();
    dict.put("byte", Byte.MAX_VALUE);
    dict.put("boolean", Boolean.TRUE);
    dict.put("integer", Integer.MAX_VALUE);
    dict.put("map", new HashMap<String, String>(2));
    dict.put("string", "tes");
    dict.put("list", new ArrayList<String>(2));
    return dict;
  }

  /**
   * Creates a dictionary of default values as found in the bridge.
   *
   * @return
   */
  private Hashtable<Object, Object> buildComponentProperties() {
    Hashtable<Object, Object> dict = new Hashtable<Object, Object>();
    dict.put(OsgiJmsBridge.ACKNOWLEDGE_MODE, Session.AUTO_ACKNOWLEDGE);
    dict.put(OsgiJmsBridge.CONNECTION_CLIENT_ID, "sakai.event.bridge");
    dict.put(OsgiJmsBridge.SESSION_TRANSACTED, false);
    dict.put(OsgiJmsBridge.TOPICS, "*");
    return dict;
  }

  /**
   * Setup the needed objects for handling an event with processing turned off.
   *
   * @throws JMSException
   */
  private void setUpConnection(boolean closeConnection) {
    try {
      // mock a connection for the factory to return and expect it
      conn = createMock(Connection.class);
      expect(connFactory.createConnection()).andReturn(conn);


      if (closeConnection) {
        // expect the connection to get closed
        conn.close();
      }
      
    } catch (JMSException e) {
      // this should never happen because the calls are on mock objects
    }
  }

  /**
   * Setup the needed objects for handling an event with processing turned on.
   *
   * @throws JMSException
   */
  private void setUpFullProcess(boolean closeConnection) {
    try {
      setUpConnection(closeConnection);


      // mock a session to be returned by the connection and expect it
      sess = createMock(Session.class);
      expect(conn.createSession(false, Session.AUTO_ACKNOWLEDGE)).andReturn(sess);

      // expect the session to get run
      //sess.run();
      //expectLastCall();

      // mock a destination as a topic from the session and expect it
      topic = createMock(Topic.class);
      expect(sess.createTopic((String) anyObject())).andReturn(topic);

      // mock a producer for the session to create and expect it
      prod = createMock(MessageProducer.class);
      expect(sess.createProducer(topic)).andReturn(prod);

      // mock the return of a mapped message
      message = new ActiveMQMessage();
      expect(sess.createMessage()).andReturn(message);

      // expect the message to be sent
      prod.send(message);
      

      sess.close();
    } catch (JMSException e) {
      // this should never happen because the calls are on mock objects
    }
  }
}
