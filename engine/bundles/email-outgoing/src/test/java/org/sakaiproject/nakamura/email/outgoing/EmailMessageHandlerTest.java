package org.sakaiproject.nakamura.email.outgoing;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.message.listener.MessageRoutesImpl;

import javax.jcr.Node;
import javax.jcr.Property;

public class EmailMessageHandlerTest {

  private EmailMessageHandler emh;

  @Before
  public void setup() {
    EventAdmin eventAdmin = createMock(EventAdmin.class);
    eventAdmin.postEvent(isA(Event.class));
    expectLastCall();

    replay(eventAdmin);

    emh = new EmailMessageHandler();
    emh.bindEventAdmin(eventAdmin);
  }

  @Test
  public void testSend() throws Exception {
    Property prop = createMock(Property.class);
    expect(prop.getString()).andReturn("smtp:foo@localhost");

    Node node = createMock(Node.class);
    expect(node.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(prop);
    expect(node.getPath()).andReturn("/foo");

    replay(prop, node);
    MessageRoutes routes = new MessageRoutesImpl(node);

    emh.send(routes, null, node);
    verify(prop, node);
  }

  @Test
  public void testGetType() {
    assertEquals(MessageConstants.TYPE_SMTP, emh.getType());
  }
}
