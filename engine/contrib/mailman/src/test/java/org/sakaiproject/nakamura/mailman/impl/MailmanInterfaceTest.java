package org.sakaiproject.nakamura.mailman.impl;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sakaiproject.nakamura.api.proxy.ProxyClientService;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;
import org.sakaiproject.nakamura.testutils.http.DummyServer;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

public class MailmanInterfaceTest extends AbstractEasyMockTest {

  private MailmanManagerImpl mailMan = new MailmanManagerImpl();
  private static DummyServer dummyServer;
  
  @BeforeClass
  public static void staticSetup() {
    dummyServer = new DummyServer();
  }
  
  @Before
  public void setUp() throws Exception {
    super.setUp();
    mailMan.setServer("localhost:" + dummyServer.getPort());
    mailMan.setMailmanPath("/cgi-bin/mailman");
    ProxyClientService proxyClientService = createMock(ProxyClientService.class);
    HttpConnectionManager connectionManager = new SimpleHttpConnectionManager();
    expect(proxyClientService.getHttpConnectionManager()).andReturn(connectionManager).anyTimes();
    mailMan.setProxyClientService(proxyClientService);
  }

  @AfterClass
  public static void staticTeardown() {
    dummyServer.close();
  }
  
  @Test
  public void testEmptyListList() throws IOException, MailmanException, SAXException {
    dummyServer.setResponseBodyFromFile("nolists.html");
    replay();
    List<String> lists = mailMan.getLists();
    assertNotNull("Expected lists to be returned", lists);
    assertEquals("Expected no lists to be found", 0, lists.size());
    verify();
  }

  @Test
  public void testTwoListList() throws IOException, MailmanException, SAXException {
    dummyServer.setResponseBodyFromFile("twolists.html");
    replay();
    List<String> lists = mailMan.getLists();
    assertNotNull("Expected lists to be returned", lists);
    assertEquals("Expected two lists to be found", 2, lists.size());
    assertEquals("Expected list name to match", "Testlist", lists.get(0));
    assertEquals("Expected list name to match", "Testlist2", lists.get(1));
    verify();
  }
  
  @Test
  public void testFindMemberFails() throws Exception {
    dummyServer.setResponseBodyFromFile("nomemberfound.html");
    replay();
    boolean isMember = mailMan.listHasMember("testlist", "testpassword", "user@example.com");
    assertFalse("Expected list not to have member", isMember);
    verify();
  }

  @Test
  public void testFindMemberSucceeds() throws Exception {
    dummyServer.setResponseBodyFromFile("findmember.html");
    replay();
    boolean isMember = mailMan.listHasMember("testlist", "testpassword", "user@example.com");
    assertTrue("Expected list to have member", isMember);
    verify();
  }

  @Test
  public void testSubscribeSucceeds() throws Exception {
    dummyServer.setResponseBodyFromFile("useradded.html");
    replay();
    boolean succeeded = mailMan.addMember("testlist", "testpassword", "user@example.com");
    assertTrue("Expected list addition to succeed", succeeded);
    verify();
  }

  @Test
  public void testSubscribeFails() throws Exception {
    dummyServer.setResponseBodyFromFile("usernotadded.html");
    replay();
    boolean succeeded = mailMan.addMember("testlist", "testpassword", "user@example.com");
    assertFalse("Expected list addition to fail", succeeded);
    verify();
  }

  @Test
  public void testUnsubscribeSucceeds() throws Exception {
    dummyServer.setResponseBodyFromFile("userremoved.html");
    replay();
    boolean succeeded = mailMan.removeMember("testlist", "testpassword", "user@example.com");
    assertTrue("Expected list removal to succeed", succeeded);
    verify();
  }

  @Test
  public void testUnsubscribeFails() throws Exception {
    dummyServer.setResponseBodyFromFile("usernotremoved.html");
    replay();
    boolean succeeded = mailMan.removeMember("testlist", "testpassword", "user@example.com");
    assertFalse("Expected list removal to fail", succeeded);
    verify();
  }

}
