package org.sakaiproject.nakamura.message.listener;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRouterManager;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.message.MessageTransport;

import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;

public class MessageSentListenerTest {
  private MessageSentListener msl;
  private MessageRouterManager messageRouterManager;
  private SlingRepository slingRepository;
  private static final String PATH = "/foo/bar";
  private Session session;
  private Node msgNode;

  @Before
  public void setup() throws Exception {
    Property prop = createMock(Property.class);
    expect(prop.getString()).andReturn(":admin");

    Node node = createMock(Node.class);
    expect(node.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(prop);
    expect(node.getPath()).andReturn("").anyTimes();
    expect(node.isNew()).andReturn(true).anyTimes();

    replay(prop, node);

    messageRouterManager = createMock(MessageRouterManager.class);
    expect(messageRouterManager.getMessageRouting(isA(Node.class))).andReturn(
        new MessageRoutesImpl(node));

    replay(messageRouterManager);

    session = createMock(Session.class);

    Property msgProp = createMock(Property.class);
    expect(msgProp.getString()).andReturn(MessageConstants.SAKAI_MESSAGE_RT);

    msgNode = createMock(Node.class);
    expect(msgNode.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))
        .andReturn(msgProp);

    expect(session.getItem(PATH)).andReturn(msgNode);

    slingRepository = createMock(SlingRepository.class);
    expect(slingRepository.loginAdministrative(null)).andReturn(session);
    session.logout();
    expectLastCall();
    replay(msgProp, msgNode, session, slingRepository);

    msl = new MessageSentListener(messageRouterManager, slingRepository);
  }

  @Test
  public void testHandleEvent() throws Exception {

    Properties eventProps = new Properties();
    eventProps.put(MessageConstants.EVENT_LOCATION, PATH);
    Event event = new Event("myTopic", eventProps);

    MessageTransport transport = createMock(MessageTransport.class);
    transport.send(isA(MessageRoutes.class), eq(event), eq(msgNode));
    expectLastCall();

    replay(transport);

    msl.addTransport(transport);
    msl.handleEvent(event);
    msl.removeTransport(transport);
  }
}
