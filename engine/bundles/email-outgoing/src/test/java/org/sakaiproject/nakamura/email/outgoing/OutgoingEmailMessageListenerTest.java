package org.sakaiproject.nakamura.email.outgoing;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.activemq.ActiveMQConnectionFactoryService;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.subethamail.wiser.Wiser;
import org.subethamail.wiser.WiserMessage;

import java.io.ByteArrayInputStream;
import java.net.BindException;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.mail.internet.MimeMultipart;

public class OutgoingEmailMessageListenerTest {
  private static final String NODE_PATH_PROPERTY = "nodePath";
  private static final String PATH = "/foo";

  private ActiveMQConnectionFactoryService connFactoryService;
  private OutgoingEmailMessageListener oeml;
  private Session adminSession;
  private Node messageNode;
  private static Wiser wiser;
  private static int smtpPort;

  @Before
  public void setup() throws Exception {
    connFactoryService = new ActiveMQConnectionFactoryService();
    oeml = new OutgoingEmailMessageListener(connFactoryService);

    Properties props = new Properties();
    props.put(ActiveMQConnectionFactoryService.BROKER_URL, "tcp://localhost:61616");
    props.put("email.out.queueName", "sakai.email.outgoing");
    props.put("sakai.smtp.server", "localhost");
    props.put("sakai.smtp.port", smtpPort);
    props.put("sakai.email.maxRetries", 240);
    props.put("sakai.email.retryIntervalMinutes", 30);

    ComponentContext ctx = createMock(ComponentContext.class);
    expect(ctx.getProperties()).andReturn(props).anyTimes();

    adminSession = createMock(Session.class);

    messageNode = createMock(Node.class);

    SlingRepository repository = createMock(SlingRepository.class);
    expect(repository.loginAdministrative(null)).andReturn(adminSession);

    Resource res = createMock(Resource.class);
    expect(res.adaptTo(Node.class)).andReturn(messageNode);

    ResourceResolver rr = createMock(ResourceResolver.class);
    expect(rr.getResource(PATH)).andReturn(res);

    ResourceResolverFactory rrf = createMock(ResourceResolverFactory.class);
    expect(rrf.getResourceResolver(isA(Map.class))).andReturn(rr);

    oeml.bindResourceResolverFactory(rrf);
    oeml.bindRepository(repository);

    replay(ctx, adminSession, res, rr, rrf, repository);

    connFactoryService.activateForTest(ctx);
    oeml.activate(ctx);
    wiser.getMessages().clear();
  }

  @BeforeClass
  public static void startWiser() {
    wiser = new Wiser();
    smtpPort = 8025;
    boolean started = false;
    while (!started) {
      wiser.setPort(smtpPort);
      try {
        wiser.start();
        started = true;
      } catch (RuntimeException re) {
        if (re.getCause() instanceof BindException) {
          smtpPort++;
        }
      }
    }
  }

  @AfterClass
  public static void stopWiser() {
    wiser.stop();
  }

  @Test
  public void testNoBoxParam() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);
    expect(message.getObjectProperty(OutgoingEmailMessageListener.RECIPIENTS)).andReturn(
        "xx@123.com,yyy@345.com");
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        false);
    expect(
        messageNode
            .setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, "Not an outbox"))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        true);

    replay(message, messageNode);

    oeml.onMessage(message);
  }

  @Test
  public void testNotOutBox() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);
    expect(message.getObjectProperty(OutgoingEmailMessageListener.RECIPIENTS)).andReturn(
        "xx@123.com,yyy@345.com");

    Property boxName = createMock(Property.class);
    expect(boxName.getString()).andReturn(MessageConstants.BOX_INBOX);

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        true);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        boxName);
    expect(
        messageNode
            .setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, "Not an outbox"))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        true);

    replay(message, messageNode, boxName);

    oeml.onMessage(message);
  }

  @Test
  public void testNoTo() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);
    expect(message.getObjectProperty(OutgoingEmailMessageListener.RECIPIENTS)).andReturn(
        "xx@123.com,yyy@345.com");

    Property boxName = createMock(Property.class);
    expect(boxName.getString()).andReturn(MessageConstants.BOX_OUTBOX);

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        true);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        boxName);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, (String) null))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(false);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR,
            "Message must have a to and from set")).andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        true).times(2);

    replay(message, messageNode, boxName);

    oeml.onMessage(message);
  }

  @Test
  public void testNoFrom() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);
    expect(message.getObjectProperty(OutgoingEmailMessageListener.RECIPIENTS)).andReturn(
        "xx@123.com,yyy@345.com");

    Property boxName = createMock(Property.class);
    expect(boxName.getString()).andReturn(MessageConstants.BOX_OUTBOX);

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        true);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        boxName);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, (String) null))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(true);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(false);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR,
            "Message must have a to and from set")).andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        true).times(2);

    replay(message, messageNode, boxName);

    oeml.onMessage(message);
  }

  @Test
  public void testSingleTo() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);
    expect(message.getObjectProperty(OutgoingEmailMessageListener.RECIPIENTS)).andReturn(
        "tonobody@example.com");

    Property boxName = createMock(Property.class);
    expect(boxName.getString()).andReturn(MessageConstants.BOX_OUTBOX);

    Property toProp = createMock(Property.class);
    expect(toProp.getString()).andReturn("tonobody@example.com");

    Property fromProp = createMock(Property.class);
    expect(fromProp.getString()).andReturn("fromnobody@example.com");

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        true);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        boxName);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, (String) null))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(true).times(2);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(true).times(2);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        false).times(2);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(toProp);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(fromProp);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_BODY)).andReturn(false);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_SUBJECT)).andReturn(false);
    expect(messageNode.hasNodes()).andReturn(false);
    expect(messageNode.getSession()).andReturn(null);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
            MessageConstants.BOX_SENT)).andReturn(null);

    replay(message, messageNode, boxName, toProp, fromProp);

    oeml.onMessage(message);

    for (WiserMessage m : wiser.getMessages()) {
      assertEquals("tonobody@example.com", m.getEnvelopeReceiver());
      assertEquals("fromnobody@example.com", m.getEnvelopeSender());
    }
  }

  @Test
  public void testMultiTo() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);
    expect(message.getObjectProperty(OutgoingEmailMessageListener.RECIPIENTS)).andReturn(
        "tonobody0@example.com,tonobody1@example.com");

    Property boxName = createMock(Property.class);
    expect(boxName.getString()).andReturn(MessageConstants.BOX_OUTBOX);

    Value[] toAddresses = new Value[2];
    for (int i = 0; i < toAddresses.length; i++) {
      toAddresses[i] = createMock(Value.class);
      expect(toAddresses[i].getString()).andReturn("tonobody" + i + "@example.com");
      replay(toAddresses[i]);
    }

    Property toProp = createMock(Property.class);
    expect(toProp.getString()).andThrow(new ValueFormatException());
    expect(toProp.getValues()).andReturn(toAddresses);

    Property fromProp = createMock(Property.class);
    expect(fromProp.getString()).andReturn("fromnobody@example.com");

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        true);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        boxName);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, (String) null))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(true).times(2);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(true);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        false).times(2);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(toProp)
        .times(2);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(fromProp);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_BODY)).andReturn(false);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_SUBJECT)).andReturn(false);
    expect(messageNode.hasNodes()).andReturn(false);
    expect(messageNode.getSession()).andReturn(null);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
            MessageConstants.BOX_SENT)).andReturn(null);

    replay(message, messageNode, boxName, toProp, fromProp);

    oeml.onMessage(message);

    int i = 0;
    for (WiserMessage m : wiser.getMessages()) {
      assertEquals("tonobody" + i++ + "@example.com", m.getEnvelopeReceiver());
      assertEquals("fromnobody@example.com", m.getEnvelopeSender());
    }
  }

  @Test
  public void testBody() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);
    expect(message.getObjectProperty(OutgoingEmailMessageListener.RECIPIENTS)).andReturn(
        "tonobody@example.com");

    Property boxName = createMock(Property.class);
    expect(boxName.getString()).andReturn(MessageConstants.BOX_OUTBOX);

    Property toProp = createMock(Property.class);
    expect(toProp.getString()).andReturn("tonobody@example.com");

    Property fromProp = createMock(Property.class);
    expect(fromProp.getString()).andReturn("fromnobody@example.com");

    Property bodyProp = createMock(Property.class);
    expect(bodyProp.getString()).andReturn("Message body looks like this.");

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        true);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        boxName);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, (String) null))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(true).times(2);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(true).times(2);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        false).times(2);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(toProp);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(fromProp);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_BODY)).andReturn(true);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_SUBJECT)).andReturn(false);
    expect(messageNode.hasNodes()).andReturn(false);
    expect(messageNode.getSession()).andReturn(null);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
            MessageConstants.BOX_SENT)).andReturn(null);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_BODY)).andReturn(bodyProp);

    replay(message, messageNode, boxName, toProp, fromProp, bodyProp);

    oeml.onMessage(message);

    for (WiserMessage m : wiser.getMessages()) {
      assertEquals("tonobody@example.com", m.getEnvelopeReceiver());
      assertEquals("fromnobody@example.com", m.getEnvelopeSender());
      MimeMultipart content = (MimeMultipart) m.getMimeMessage().getContent();
      assertEquals("Message body looks like this.", content.getBodyPart(0).getContent());
    }
  }

  @Test
  public void testSubject() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);
    expect(message.getObjectProperty(OutgoingEmailMessageListener.RECIPIENTS)).andReturn(
        "tonobody@example.com");

    Property boxName = createMock(Property.class);
    expect(boxName.getString()).andReturn(MessageConstants.BOX_OUTBOX);

    Property toProp = createMock(Property.class);
    expect(toProp.getString()).andReturn("tonobody@example.com");

    Property fromProp = createMock(Property.class);
    expect(fromProp.getString()).andReturn("fromnobody@example.com");

    Property subjProp = createMock(Property.class);
    expect(subjProp.getString()).andReturn("Message subject looks like this.");

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        true);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        boxName);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, (String) null))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(true).times(2);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(true).times(2);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        false).times(2);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(toProp);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(fromProp);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_BODY)).andReturn(false);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_SUBJECT)).andReturn(true);
    expect(messageNode.hasNodes()).andReturn(false);
    expect(messageNode.getSession()).andReturn(null);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
            MessageConstants.BOX_SENT)).andReturn(null);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_SUBJECT)).andReturn(
        subjProp);

    replay(message, messageNode, boxName, toProp, fromProp, subjProp);

    oeml.onMessage(message);

    for (WiserMessage m : wiser.getMessages()) {
      assertEquals("tonobody@example.com", m.getEnvelopeReceiver());
      assertEquals("fromnobody@example.com", m.getEnvelopeSender());
      assertEquals("Message subject looks like this.", m.getMimeMessage().getSubject());
    }
  }

  @Test
  public void testJMSExceptionHandling() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andThrow(
        new JMSException("Test JMS Exception"));

    replay(message);

    oeml.onMessage(message);
  }

  @Test
  public void testRepoExceptionHandling() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);
    expect(message.getObjectProperty(OutgoingEmailMessageListener.RECIPIENTS)).andReturn(
        "xx@123.com,yyy@345.com");

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andThrow(
        new RepositoryException("Test Repository Exception"));
    replay(message, messageNode);

    oeml.onMessage(message);
  }

  @Test
  public void testAttachment() throws Exception {
    Message message = createMock(Message.class);
    expect(message.getStringProperty(NODE_PATH_PROPERTY)).andReturn(PATH);
    expect(message.getObjectProperty(OutgoingEmailMessageListener.RECIPIENTS)).andReturn(
        "tonobody@example.com");

    Property boxName = createMock(Property.class);
    expect(boxName.getString()).andReturn(MessageConstants.BOX_OUTBOX);

    Property toProp = createMock(Property.class);
    expect(toProp.getString()).andReturn("tonobody@example.com");

    Property fromProp = createMock(Property.class);
    expect(fromProp.getString()).andReturn("fromnobody@example.com");

    Property descProp = createMock(Property.class);
    expect(descProp.getString()).andReturn("Digital signature");

    Property contentProp = createMock(Property.class);
    Binary contentBin = createMock(Binary.class);
    expect(contentBin.getStream()).andReturn(
        new ByteArrayInputStream("----BEGIN FAKE PGP SIGNATURE----".getBytes()))
        .anyTimes();
    expect(contentProp.getBinary()).andReturn(contentBin).anyTimes();

    Property ctProp = createMock(Property.class);
    expect(ctProp.getString()).andReturn("applica/pgp-signat").anyTimes();

    NodeType nodeType = createMock(NodeType.class);
    expect(nodeType.getName()).andReturn(JcrConstants.NT_BASE).anyTimes();

    Node childNode = createMock(Node.class);
    expect(childNode.hasProperty(MessageConstants.PROP_SAKAI_ATTACHMENT_DESCRIPTION))
        .andReturn(true);
    expect(childNode.getProperty(MessageConstants.PROP_SAKAI_ATTACHMENT_DESCRIPTION))
        .andReturn(descProp);
    expect(childNode.getName()).andReturn("signature.asc").times(2);
    expect(childNode.getPrimaryNodeType()).andReturn(nodeType).anyTimes();
    expect(childNode.getProperty(MessageConstants.PROP_SAKAI_ATTACHMENT_CONTENT))
        .andReturn(contentProp).anyTimes();
    expect(childNode.hasProperty(MessageConstants.PROP_SAKAI_CONTENT_TYPE)).andReturn(
        true).anyTimes();
    expect(childNode.getProperty(MessageConstants.PROP_SAKAI_CONTENT_TYPE)).andReturn(
        ctProp).anyTimes();

    NodeIterator nodeIterator = createMock(NodeIterator.class);
    expect(nodeIterator.hasNext()).andReturn(true);
    expect(nodeIterator.nextNode()).andReturn(childNode);
    expect(nodeIterator.hasNext()).andReturn(false);

    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        true);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX)).andReturn(
        boxName);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR, (String) null))
        .andReturn(null);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(true).times(2);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(true).times(2);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_MESSAGEERROR)).andReturn(
        false).times(2);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_TO)).andReturn(toProp);
    expect(messageNode.getProperty(MessageConstants.PROP_SAKAI_FROM)).andReturn(fromProp);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_BODY)).andReturn(false);
    expect(messageNode.hasProperty(MessageConstants.PROP_SAKAI_SUBJECT)).andReturn(false);
    expect(messageNode.hasNodes()).andReturn(true);
    expect(messageNode.getSession()).andReturn(null);
    expect(messageNode.getNodes()).andReturn(nodeIterator);
    expect(
        messageNode.setProperty(MessageConstants.PROP_SAKAI_MESSAGEBOX,
            MessageConstants.BOX_SENT)).andReturn(null);

    replay(message, messageNode, boxName, toProp, fromProp, descProp, contentProp,
        contentBin, ctProp, childNode, nodeType, nodeIterator);

    oeml.onMessage(message);

    for (WiserMessage m : wiser.getMessages()) {
      assertEquals("tonobody@example.com", m.getEnvelopeReceiver());
      assertEquals("fromnobody@example.com", m.getEnvelopeSender());
    }
  }
}
