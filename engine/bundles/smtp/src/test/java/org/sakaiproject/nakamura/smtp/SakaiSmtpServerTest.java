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
package org.sakaiproject.nakamura.smtp;

import junit.framework.Assert;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.jcr.api.SlingRepository;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.ValueFactory;

/**
 *
 */
public class SakaiSmtpServerTest extends AbstractEasyMockTest {

  private static final String TESTMESSAGE = "SomeHeaders: Header\nSubject: testing\nHere is a message body";
  private static final String TESTMESSAGE_GOOD = ""
      + "Delivered-To: ianboston@googlemail.com \n"
      + "Received: by 10.204.61.70 with SMTP id s6cs59936bkh;\n"
      + "        Thu, 11 Feb 2010 05:18:41 -0800 (PST)\n"
      + "Received: by 10.150.194.13 with SMTP id r13mr175997ybf.81.1265894320608;\n"
      + "        Thu, 11 Feb 2010 05:18:40 -0800 (PST)\n"
      + "Return-Path: <k2-jira@sakaiproject.org>\n"
      + "Received: from mailex.atlas.pipex.net (mailex.atlas.pipex.net [194.154.164.61])\n"
      + "        by mx.google.com with ESMTP id 40si5206880ywh.30.2010.02.11.05.18.40;\n"
      + "        Thu, 11 Feb 2010 05:18:40 -0800 (PST)\n"
      + "Received-SPF: neutral (google.com: 194.154.164.61 is neither permitted nor denied by best guess record for domain of k2-jira@sakaiproject.org) client-ip=194.154.164.61;\n"
      + "Authentication-Results: mx.google.com; spf=neutral (google.com: 194.154.164.61 is neither permitted nor denied by best guess record for domain of k2-jira@sakaiproject.org) smtp.mail=k2-jira@sakaiproject.org\n"
      + "Received: from sjc-mail-2.wush.net ([208.83.222.205])\n"
      + "  by mail11.atlas.pipex.net with esmtp (Exim 4.63)\n"
      + "  (envelope-from <k2-jira@sakaiproject.org>)\n" + "  id 1NfYwI-0000dY-8H\n"
      + "  for ieb@tfd.co.uk; Thu, 11 Feb 2010 13:18:38 +0000\n"
      + "Received: from sjc-app-1.wush.net (unknown [208.83.222.212])\n"
      + "  by sjc-mail-2.wush.net (Postfix) with ESMTP id E2EE4D034B\n"
      + "  for <ieb@tfd.co.uk>; Thu, 11 Feb 2010 05:18:35 -0800 (PST)\n"
      + "Received: from sjc-app-1.wush.net (localhost.localdomain [127.0.0.1])\n"
      + "  by sjc-app-1.wush.net (Postfix) with ESMTP id 46F7810001AD\n"
      + "  for <ieb@tfd.co.uk>; Thu, 11 Feb 2010 05:18:35 -0800 (PST)\n"
      + "Message-ID: <2056926400.1265894315267.JavaMail.sakai@sjc-app-1.wush.net>\n"
      + "Date: Thu, 11 Feb 2010 05:18:35 -0800 (PST)\n"
      + "From: \"Lance Speelmon (JIRA)\" <k2-jira@sakaiproject.org>\n"
      + "To: ieb@tfd.co.uk\n"
      + "Subject: [Sakai Jira] Commented: (KERN-631) Expose Sakai 2 tools in a Sakai\n"
      + " 3 environment detached from a Sakai 2 rendered site\n" + "MIME-Version: 1.0\n"
      + "Content-Type: text/plain; charset=UTF-8\n" + "Content-Transfer-Encoding: 7bit\n"
      + "Precedence: bulk\n" + "Auto-Submitted: auto-generated\n"
      + "X-JIRA-FingerPrint: 43079b93228ea120d4bc89f05c6f1356\n\n"
      + "Here is a message body";



  private static final String SUBJECT_TEST = "[Sakai Jira] Commented: (KERN-631) Expose Sakai 2 tools in a Sakai\r\n"
  + " 3 environment detached from a Sakai 2 rendered site";
  private static final String MULTIPART_SUBJECT_TEST = "Breaking News Extra: Husband of Accused Huntsville Killer Says She Was Bitter Over Tenure";
  private static final String MULTIPART_SUBJECT_TEST2 = "TestBinary Mesage";
  private static final Logger LOGGER = LoggerFactory.getLogger(SakaiSmtpServerTest.class);


  @Test
  public void testBadFormatMessage() throws Exception {

    ComponentContext componentContext = createNiceMock(ComponentContext.class);
    SlingRepository slingRepository = createNiceMock(SlingRepository.class);
    JackrabbitSession session = createMock(JackrabbitSession.class);
    MessagingService messagingService = createMock(MessagingService.class);
    Node myMessageNode = createMock(Node.class);
    Property property = createMock(Property.class);
    Binary binary = createMock(Binary.class);
    ValueFactory valueFactory = createMock(ValueFactory.class);

    Dictionary<String, Object> properties = new Hashtable<String, Object>();

    session.logout();
    EasyMock.expectLastCall().anyTimes();

    EasyMock.expect(componentContext.getProperties()).andReturn(properties).anyTimes();
    int port = getSafePort(8025);
    properties.put("smtp.port", Integer.valueOf(port));
    EasyMock.expect(slingRepository.loginAdministrative(null)).andReturn(session)
        .anyTimes();
    List<String> recipents = new ArrayList<String>();
    recipents.add("alice");
    EasyMock.expect(messagingService.expandAliases("alice")).andReturn(recipents)
        .anyTimes();
    EasyMock.expect(messagingService.getFullPathToStore("alice", session)).andReturn(
        "/messagestore/alice").anyTimes();
    List<String> senders = new ArrayList<String>();
    senders.add("bob");
    EasyMock.expect(messagingService.expandAliases("bob")).andReturn(senders).anyTimes();
    EasyMock.expect(messagingService.getFullPathToStore("bob", session)).andReturn(
        "/messagestore/bob").anyTimes();
    System.setProperty("org.sakaiproject.nakamura.SMTPServerPort", "9025");
    InputStream dataStream = new ByteArrayInputStream(TESTMESSAGE.getBytes("UTF-8"));


    Capture<Map<String, Object>> mapProperties2 = new Capture<Map<String, Object>>();
    Capture<Session> sessionCapture2 = new Capture<Session>();
    EasyMock.expect(session.getValueFactory()).andReturn(valueFactory);
    EasyMock.expect(valueFactory.createBinary(dataStream)).andReturn(binary);
    EasyMock.expect(
        messagingService.create(EasyMock.capture(sessionCapture2), EasyMock
            .capture(mapProperties2))).andReturn(myMessageNode);
    EasyMock.expect(myMessageNode.setProperty("sakai:body", binary)).andReturn(null);
    session.save();
    EasyMock.expectLastCall();

    EasyMock.expect(myMessageNode.getPath()).andReturn("/messagestore/bob/messagenode");
    EasyMock.expect(myMessageNode.getProperty("message-id")).andReturn(property);
    EasyMock.expect(property.getString()).andReturn("messageid");
    EasyMock.expect(session.hasPendingChanges()).andReturn(true);
    session.save();
    EasyMock.expectLastCall();

    replay();
    SakaiSmtpServer sakaiSmtpServer = new SakaiSmtpServer();
    sakaiSmtpServer.slingRepository = slingRepository;
    sakaiSmtpServer.messagingService = messagingService;

    sakaiSmtpServer.activate(componentContext);

    sakaiSmtpServer.accept("bob@localhost", "alice@localhost");
    sakaiSmtpServer.deliver("bob@localhost", "alice@localhost", dataStream);


    // call to messageService.create
    Assert.assertTrue(mapProperties2.hasCaptured());
    Assert.assertTrue(sessionCapture2.hasCaptured());

    Assert.assertEquals(session, sessionCapture2.getValue());

    sakaiSmtpServer.deactivate(componentContext);

    verify();
  }

  @Test
  public void testSafePort() throws IOException {
    ServerSocket s1 = null;
    try {
      s1 = new ServerSocket(8025);
    } catch (IOException e1) {
    }
    ServerSocket s2 = null;
    try {
      s2 = new ServerSocket(8026);
    } catch (IOException e2) {
    }
    ServerSocket s3 = null;
    try {
      s3 = new ServerSocket(8027);
    } catch (IOException e1) {
    }
    ServerSocket s4 = null;
    try {
      s4 = new ServerSocket(8028);
    } catch (IOException e1) {
    }

    int port = getSafePort(8025);
    Assert.assertTrue(port>8028);
    ServerSocket ss = new ServerSocket(port);
    Assert.assertTrue(ss.isBound());
    ss.close();

    try {

    } finally {
      try {
        s1.close();
      } catch (Exception e) {
      }
      try {
        s2.close();
      } catch (Exception e) {
      }
      try {
        s3.close();
      } catch (Exception e) {
      }
      try {
        s4.close();
      } catch (Exception e) {
      }
    }

  }

  /**
   * @param i
   * @return
   */
  private int getSafePort(int i) {
    for ( int p = i; p < i+500; p++ ) {
      ServerSocket serverSocket = null;
      try {
        serverSocket = new ServerSocket(p);
        LOGGER.info("Got socket at {} ", p);
        return p;
      } catch (IOException e) {
        LOGGER.info("Failed to get socket at {} ", p);
      } finally {
        try {
          serverSocket.close();
        } catch (Exception e) {
          LOGGER.debug("Failed to close socket at {}, safe to ignore ", p);
        }
      }
    }
    return 0;
  }

  @Test
  public void testGoodFormatMessage() throws Exception {

    ComponentContext componentContext = createNiceMock(ComponentContext.class);
    SlingRepository slingRepository = createNiceMock(SlingRepository.class);
    JackrabbitSession session = createMock(JackrabbitSession.class);
    MessagingService messagingService = createMock(MessagingService.class);
    Node myMessageNode = createMock(Node.class);
    Property property = createMock(Property.class);
    Binary binary = createMock(Binary.class);
    ValueFactory valueFactory = createMock(ValueFactory.class);

    Dictionary<String, Object> properties = new Hashtable<String, Object>();

    session.logout();
    EasyMock.expectLastCall().anyTimes();

    int port = getSafePort(8025);
    properties.put("smtp.port", Integer.valueOf(port));

    EasyMock.expect(componentContext.getProperties()).andReturn(properties).anyTimes();
    EasyMock.expect(slingRepository.loginAdministrative(null)).andReturn(session)
        .anyTimes();
    List<String> recipents = new ArrayList<String>();
    recipents.add("alice");
    EasyMock.expect(messagingService.expandAliases("alice")).andReturn(recipents)
        .anyTimes();
    EasyMock.expect(messagingService.getFullPathToStore("alice", session)).andReturn(
        "/messagestore/alice").anyTimes();
    List<String> senders = new ArrayList<String>();
    senders.add("bob");
    EasyMock.expect(messagingService.expandAliases("bob")).andReturn(senders).anyTimes();
    EasyMock.expect(messagingService.getFullPathToStore("bob", session)).andReturn(
        "/messagestore/bob").anyTimes();
    InputStream dataStream = new ByteArrayInputStream(TESTMESSAGE_GOOD.getBytes("UTF-8"));


    Capture<Map<String, Object>> mapProperties2 = new Capture<Map<String, Object>>();
    Capture<Session> sessionCapture2 = new Capture<Session>();
    EasyMock.expect(
        messagingService.create(EasyMock.capture(sessionCapture2), EasyMock
            .capture(mapProperties2))).andReturn(myMessageNode);

    EasyMock.expect(session.getValueFactory()).andReturn(valueFactory);
    EasyMock.expect(valueFactory.createBinary(dataStream)).andReturn(binary);
    EasyMock.expect(myMessageNode.setProperty("sakai:body", binary)).andReturn(null);
    session.save();
    EasyMock.expectLastCall();

    EasyMock.expect(myMessageNode.getPath()).andReturn("/messagestore/bob/messagenode");
    EasyMock.expect(myMessageNode.getProperty("message-id")).andReturn(property);
    EasyMock.expect(property.getString()).andReturn("messageid");
    EasyMock.expect(session.hasPendingChanges()).andReturn(true);
    session.save();
    EasyMock.expectLastCall();

    replay();
    SakaiSmtpServer sakaiSmtpServer = new SakaiSmtpServer();
    sakaiSmtpServer.slingRepository = slingRepository;
    sakaiSmtpServer.messagingService = messagingService;

    sakaiSmtpServer.activate(componentContext);

    sakaiSmtpServer.accept("bob@localhost", "alice@localhost");
    sakaiSmtpServer.deliver("bob@localhost", "alice@localhost", dataStream);


    // call to messageService.create
    Assert.assertTrue(mapProperties2.hasCaptured());
    Assert.assertTrue(sessionCapture2.hasCaptured());

    Assert.assertEquals(session, sessionCapture2.getValue());

    Map<String,Object> headers = mapProperties2.getValue();
    // check multi line parsing of headers
    Assert.assertEquals(SUBJECT_TEST, headers.get("sakai:subject"));

    // check multi header parsing.
    String[] recieved = (String[]) headers.get("sakai:received");
    Assert.assertNotNull(recieved);
    Assert.assertEquals(recieved.length, 6);

    sakaiSmtpServer.deactivate(componentContext);

    verify();
  }


  @Test
  public void testGoodFormatMultipartMessage() throws Exception {

    ComponentContext componentContext = createNiceMock(ComponentContext.class);
    SlingRepository slingRepository = createNiceMock(SlingRepository.class);
    JackrabbitSession session = createMock(JackrabbitSession.class);
    MessagingService messagingService = createMock(MessagingService.class);
    Node part0Node = createNiceMock(Node.class);
    Node myMessageNode = createMock(Node.class);
    Property property = createMock(Property.class);
    ValueFactory valueFactory = createNiceMock(ValueFactory.class);

    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    int port = getSafePort(8025);
    properties.put("smtp.port", Integer.valueOf(port));

    session.logout();
    EasyMock.expectLastCall().anyTimes();

    EasyMock.expect(session.getValueFactory()).andReturn(valueFactory).anyTimes();

    EasyMock.expect(componentContext.getProperties()).andReturn(properties).anyTimes();
    EasyMock.expect(slingRepository.loginAdministrative(null)).andReturn(session)
        .anyTimes();
    List<String> recipents = new ArrayList<String>();
    recipents.add("alice");
    EasyMock.expect(messagingService.expandAliases("alice")).andReturn(recipents)
        .anyTimes();
    EasyMock.expect(messagingService.getFullPathToStore("alice", session)).andReturn(
        "/messagestore/alice").anyTimes();
    List<String> senders = new ArrayList<String>();
    senders.add("bob");
    EasyMock.expect(messagingService.expandAliases("bob")).andReturn(senders).anyTimes();
    EasyMock.expect(messagingService.getFullPathToStore("bob", session)).andReturn(
        "/messagestore/bob").anyTimes();
    InputStream dataStream = this.getClass().getResourceAsStream("testmultipartgood.txt");


    Capture<Map<String, Object>> mapProperties = new Capture<Map<String, Object>>();
    Capture<Session> sessionCapture = new Capture<Session>();
    Capture<String> messageId = new Capture<String>();
    Capture<String> path = new Capture<String>();
    EasyMock.expect(
        messagingService.create(EasyMock.capture(sessionCapture), EasyMock
            .capture(mapProperties), EasyMock.capture(messageId), EasyMock.capture(path))).andReturn(myMessageNode);

    EasyMock.expect(myMessageNode.addNode("part000")).andReturn(part0Node);
    EasyMock.expect(myMessageNode.addNode("part001")).andReturn(part0Node);


    EasyMock.expect(myMessageNode.getPath()).andReturn("/messagestore/bob/messagenode");
    EasyMock.expect(myMessageNode.getProperty("message-id")).andReturn(property);
    EasyMock.expect(property.getString()).andReturn("messageid");
    EasyMock.expect(session.hasPendingChanges()).andReturn(true);
    session.save();
    EasyMock.expectLastCall().anyTimes();

    replay();
    SakaiSmtpServer sakaiSmtpServer = new SakaiSmtpServer();
    sakaiSmtpServer.slingRepository = slingRepository;
    sakaiSmtpServer.messagingService = messagingService;


    sakaiSmtpServer.activate(componentContext);

    sakaiSmtpServer.accept("bob@localhost", "alice@localhost");
    sakaiSmtpServer.deliver("bob@localhost", "alice@localhost", dataStream);


    // call to messageService.create
    Assert.assertTrue(mapProperties.hasCaptured());
    Assert.assertTrue(sessionCapture.hasCaptured());
    Assert.assertTrue(messageId.hasCaptured());
    Assert.assertTrue(path.hasCaptured());

    Assert.assertEquals(session, sessionCapture.getValue());
    Assert.assertEquals("/messagestore/alice", path.getValue());

    Map<String,Object> headers = mapProperties.getValue();
    // check multi line parsing of headers
    Assert.assertEquals(MULTIPART_SUBJECT_TEST, headers.get("sakai:subject"));

    // check multi header parsing.
    String[] recieved = (String[]) headers.get("sakai:received");
    Assert.assertNotNull(recieved);
    Assert.assertEquals(recieved.length, 6);

    sakaiSmtpServer.deactivate(componentContext);

    verify();
  }


  @Test
  public void testGoodFormatMultipartBinaryMessage() throws Exception {

    ComponentContext componentContext = createNiceMock(ComponentContext.class);
    SlingRepository slingRepository = createNiceMock(SlingRepository.class);
    JackrabbitSession session = createMock(JackrabbitSession.class);
    MessagingService messagingService = createMock(MessagingService.class);
    Node part0Node = createNiceMock(Node.class);
    Node myMessageNode = createMock(Node.class);
    Property property = createMock(Property.class);
    ValueFactory valueFactory = createNiceMock(ValueFactory.class);

    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    int port = getSafePort(8025);
    properties.put("smtp.port", Integer.valueOf(port));

    session.logout();
    EasyMock.expectLastCall().anyTimes();

    EasyMock.expect(componentContext.getProperties()).andReturn(properties).anyTimes();
    EasyMock.expect(slingRepository.loginAdministrative(null)).andReturn(session)
        .anyTimes();
    List<String> recipents = new ArrayList<String>();
    recipents.add("alice");
    EasyMock.expect(messagingService.expandAliases("alice")).andReturn(recipents)
        .anyTimes();
    EasyMock.expect(messagingService.getFullPathToStore("alice", session)).andReturn(
        "/messagestore/alice").anyTimes();
    List<String> senders = new ArrayList<String>();
    senders.add("bob");
    EasyMock.expect(messagingService.expandAliases("bob")).andReturn(senders).anyTimes();
    EasyMock.expect(messagingService.getFullPathToStore("bob", session)).andReturn(
        "/messagestore/bob").anyTimes();
    InputStream dataStream = this.getClass().getResourceAsStream("testmultipartbinarygood.txt");


    Capture<Map<String, Object>> mapProperties = new Capture<Map<String, Object>>();
    Capture<Session> sessionCapture = new Capture<Session>();
    Capture<String> messageId = new Capture<String>();
    Capture<String> path = new Capture<String>();
    EasyMock.expect(
        messagingService.create(EasyMock.capture(sessionCapture), EasyMock
            .capture(mapProperties), EasyMock.capture(messageId), EasyMock.capture(path))).andReturn(myMessageNode);

    EasyMock.expect(myMessageNode.addNode("part000")).andReturn(part0Node);
    EasyMock.expect(myMessageNode.addNode("part001","nt:file")).andReturn(part0Node);
    EasyMock.expect(part0Node.addNode("jcr:content", "nt:resource")).andReturn(part0Node);
    EasyMock.expect(session.getValueFactory()).andReturn(valueFactory).anyTimes();


    EasyMock.expect(myMessageNode.getPath()).andReturn("/messagestore/bob/messagenode");
    EasyMock.expect(myMessageNode.getProperty("message-id")).andReturn(property);
    EasyMock.expect(property.getString()).andReturn("messageid");
    EasyMock.expect(session.hasPendingChanges()).andReturn(true);
    session.save();
    EasyMock.expectLastCall().anyTimes();

    replay();
    SakaiSmtpServer sakaiSmtpServer = new SakaiSmtpServer();
    sakaiSmtpServer.slingRepository = slingRepository;
    sakaiSmtpServer.messagingService = messagingService;


    sakaiSmtpServer.activate(componentContext);

    sakaiSmtpServer.accept("bob@localhost", "alice@localhost");
    sakaiSmtpServer.deliver("bob@localhost", "alice@localhost", dataStream);


    // call to messageService.create
    Assert.assertTrue(mapProperties.hasCaptured());
    Assert.assertTrue(sessionCapture.hasCaptured());
    Assert.assertTrue(messageId.hasCaptured());
    Assert.assertTrue(path.hasCaptured());

    Assert.assertEquals(session, sessionCapture.getValue());
    Assert.assertEquals("/messagestore/alice", path.getValue());

    Map<String,Object> headers = mapProperties.getValue();
    // check multi line parsing of headers
    Assert.assertEquals(MULTIPART_SUBJECT_TEST2, headers.get("sakai:subject"));

    // check multi header parsing.
    String recieved =  (String) headers.get("sakai:received");
    Assert.assertNotNull(recieved);

    sakaiSmtpServer.deactivate(componentContext);

    verify();
  }

}
