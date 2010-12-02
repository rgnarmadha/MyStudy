package org.sakaiproject.nakamura.user.owner;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.sakaiproject.nakamura.api.user.UserConstants.JCR_CREATED_BY;

import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

public class OwnerPrincipalManagerTest extends AbstractEasyMockTest {

  @Test
  public void testOwnerNoCreatorProp() throws Exception {
    OwnerPrincipalManagerImpl opm = new OwnerPrincipalManagerImpl();

    Node contextNode = createMock(Node.class);
    expect(contextNode.hasProperty(JCR_CREATED_BY)).andReturn(false);
    expect(contextNode.getPath()).andReturn("").anyTimes();

    Node aclNode = createMock(Node.class);
    expect(aclNode.getPath()).andReturn("").anyTimes();

    replay();
    assertFalse(opm.hasPrincipalInContext("owner", aclNode, contextNode, "ian"));
    verify();
  }

  @Test
  public void testOwnerCreator() throws Exception {
    OwnerPrincipalManagerImpl opm = new OwnerPrincipalManagerImpl();

    Property owner = createMock(Property.class);
    expect(owner.getString()).andReturn("foo");

    Node contextNode = createMock(Node.class);
    expect(contextNode.hasProperty(JCR_CREATED_BY)).andReturn(true);
    expect(contextNode.getProperty(JCR_CREATED_BY)).andReturn(owner);
    expect(contextNode.getPath()).andReturn("/path").anyTimes();

    Node aclNode = createMock(Node.class);

    replay();
    assertTrue(opm.hasPrincipalInContext("owner", aclNode, contextNode, "foo"));
    verify();
  }

  @Test
  public void testOwnerNotCreator() throws Exception {
    OwnerPrincipalManagerImpl opm = new OwnerPrincipalManagerImpl();

    Property owner = createMock(Property.class);
    expect(owner.getString()).andReturn("foo");


    Node contextNode = createMock(Node.class);
    expect(contextNode.hasProperty(JCR_CREATED_BY)).andReturn(true);
    expect(contextNode.getProperty(JCR_CREATED_BY)).andReturn(owner);
    expect(contextNode.getPath()).andReturn("/path").anyTimes();

    Node aclNode = createMock(Node.class);

    replay();
    assertFalse(opm.hasPrincipalInContext("owner", aclNode, contextNode, "bar"));
    verify();
  }

  @Test
  public void testRepositoryExceptionHandling() throws Exception {
    OwnerPrincipalManagerImpl opm = new OwnerPrincipalManagerImpl();

    Node contextNode = createMock(Node.class);
    expect(contextNode.hasProperty(JCR_CREATED_BY)).andThrow(new RepositoryException());
    expect(contextNode.getPath()).andReturn("").anyTimes();
    Node aclNode = createMock(Node.class);
    expect(aclNode.getPath()).andReturn("").anyTimes();

    replay();
    assertFalse(opm.hasPrincipalInContext("owner", aclNode, contextNode, "bar"));
    verify();
  }
}
