package org.sakaiproject.nakamura.message.listener;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sakaiproject.nakamura.api.message.MessageConstants;

import javax.jcr.Node;
import javax.jcr.Property;

public class MessageRoutesImplTest {

  @Test
  public void testConstructWithNode() throws Exception {
    Property prop = createMock(Property.class);
    expect(prop.getString()).andReturn("smtp:foo@localhost,smtp:bar@localhost");

    Node node = createMock(Node.class);
    expect(node.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(prop);
    expect(node.getPath()).andReturn("").anyTimes();
    expect(node.isNew()).andReturn(true).anyTimes();

    replay(node, prop);
    MessageRoutesImpl mri = new MessageRoutesImpl(node);
    assertEquals(2, mri.size());
  }
}
