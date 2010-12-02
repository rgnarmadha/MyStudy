package org.sakaiproject.nakamura.mailman.impl;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.sling.jcr.api.SlingRepository;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.personal.PersonalConstants;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.user.AuthorizableEvent;
import org.sakaiproject.nakamura.api.user.AuthorizableEvent.Operation;
import org.sakaiproject.nakamura.mailman.MailmanManager;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

public class MailmanGroupManagerTest extends AbstractEasyMockTest {

  private MailmanManager mailmanManager;
  private SlingRepository slingRepository;
  private MailmanGroupManager groupManager;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    mailmanManager = createMock(MailmanManager.class);
    slingRepository = createMock(SlingRepository.class);
    groupManager = new MailmanGroupManager(mailmanManager, slingRepository);
  }

  @Test
  public void testHandleGroupAdd() throws MailmanException {
    String groupName = "g-testgroup";
    String topic = "org/apache/sling/jackrabbit/usermanager/event/create";
    Dictionary<String,Object> properties = new Hashtable<String, Object>();
    properties.put(AuthorizableEvent.OPERATION, Operation.create);
    properties.put(AuthorizableEvent.PRINCIPAL_NAME, groupName);
    properties.put(AuthorizableEvent.TOPIC, topic);
    Event event = new Event(topic, properties);

    mailmanManager.createList(groupName, groupName + "@example.com", null);
    replay();
    groupManager.handleEvent(event);
    verify();
  }

  @Test
  public void testHandleGroupRemove() throws MailmanException {
    String groupName = "g-testgroup";
    String topic = "org/apache/sling/jackrabbit/usermanager/event/delete";
    Dictionary<String,Object> properties = new Hashtable<String, Object>();
    properties.put(AuthorizableEvent.OPERATION, Operation.delete);
    properties.put(AuthorizableEvent.PRINCIPAL_NAME, groupName);
    properties.put(AuthorizableEvent.TOPIC, topic);
    Event event = new Event(topic, properties);

    mailmanManager.deleteList(groupName, null);
    replay();
    groupManager.handleEvent(event);
    verify();
  }

  @Test
  public void testHandleGroupJoin() throws MailmanException, RepositoryException {
    String groupName = "g-testgroup";
    Group dummyGroup = createDummyGroup(groupName);
    String user = "testuser";
    User dummyUser = createDummyUser(user);
    EasyMock.replay(dummyUser);
    String testAddress = "test@test.com";
    addUserEmailExpectation(dummyUser, testAddress);
    String topic = "org/apache/sling/jackrabbit/usermanager/event/join";
    Dictionary<String,Object> properties = new Hashtable<String, Object>();
    properties.put(AuthorizableEvent.OPERATION, Operation.join);
    properties.put(AuthorizableEvent.PRINCIPAL_NAME, groupName);
    properties.put(AuthorizableEvent.TOPIC, topic);
    properties.put(AuthorizableEvent.USER, dummyUser);
    properties.put(AuthorizableEvent.GROUP, dummyGroup);
    Event event = new Event(topic, properties);

    expect(mailmanManager.addMember(groupName, null, testAddress)).andReturn(true);
    replay();
    groupManager.handleEvent(event);
    verify();
  }

  @Test
  public void testHandleGroupPart() throws MailmanException, RepositoryException {
    String groupName = "g-testgroup";
    Group dummyGroup = createDummyGroup(groupName);
    String user = "testuser";
    User dummyUser = createDummyUser(user);
    EasyMock.replay(dummyUser);
    String testAddress = "test@test.com";
    addUserEmailExpectation(dummyUser, testAddress);
    String topic = "org/apache/sling/jackrabbit/usermanager/event/part";
    Dictionary<String,Object> properties = new Hashtable<String, Object>();
    properties.put(AuthorizableEvent.OPERATION, Operation.part);
    properties.put(AuthorizableEvent.PRINCIPAL_NAME, groupName);
    properties.put(AuthorizableEvent.TOPIC, topic);
    properties.put(AuthorizableEvent.USER, dummyUser);
    properties.put(AuthorizableEvent.GROUP, dummyGroup);
    Event event = new Event(topic, properties);

    expect(mailmanManager.removeMember(groupName, null, testAddress)).andReturn(true);
    replay();
    groupManager.handleEvent(event);
    verify();
  }

  private void addUserEmailExpectation(User user, String testAddress) throws RepositoryException {
    String profileNodePath = PersonalUtils.getProfilePath(user);
    Session session = createMock(Session.class);
    Node node = createMock(Node.class);
    Property property = createMock(Property.class);
    expect(slingRepository.loginAdministrative(null)).andReturn(session);
    expect(session.getItem(eq(profileNodePath))).andReturn(node);
    expect(node.hasProperty(eq(PersonalConstants.EMAIL_ADDRESS))).andReturn(true).times(2);
    expect(node.getProperty(eq(PersonalConstants.EMAIL_ADDRESS))).andReturn(property);

    PropertyDefinition propDef = createMock(PropertyDefinition.class);
    expect(propDef.isMultiple()).andReturn(false);
    expect(property.getDefinition()).andReturn(propDef);

    Value value = createMock(Value.class);
    expect(value.getString()).andReturn(testAddress);
    expect(property.getValue()).andReturn(value);

    session.logout();
  }

  private Group createDummyGroup(String groupName) throws RepositoryException {
    Group group = createMock(Group.class);
    expect(group.getID()).andReturn(groupName).anyTimes();
    expect(group.isGroup()).andReturn(true).anyTimes();
    return group;
  }

  private User createDummyUser(String userName) throws RepositoryException {
    User user = EasyMock.createNiceMock(User.class);
    expect(user.getID()).andReturn(userName).anyTimes();
    expect(user.isGroup()).andReturn(false).anyTimes();
    return user;
  }
}
