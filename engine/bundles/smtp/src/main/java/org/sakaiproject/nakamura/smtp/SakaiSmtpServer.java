package org.sakaiproject.nakamura.smtp;


import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;
import org.subethamail.smtp.helper.SimpleMessageListenerAdapter;
import org.subethamail.smtp.server.SMTPServer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMultipart;

@Component(immediate = true, metatype = true)
public class SakaiSmtpServer implements SimpleMessageListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(SakaiSmtpServer.class);
  private static final int MAX_PROPERTY_SIZE = 32 * 1024;

  private SMTPServer server;

  @Reference
  protected MessagingService messagingService;

  @Reference
  protected SlingRepository slingRepository;

  @Property
  private static String LOCAL_DOMAINS = "smtp.localdomains";

  @Property(intValue=8025)
  private static String SMTP_SERVER_PORT = "smtp.port";

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  private Set<String> domains = new HashSet<String>();

  public void activate(ComponentContext context) throws Exception {
    Integer port = (Integer) context.getProperties().get(SMTP_SERVER_PORT);
    if ( port == null ) {
      port = 8025;
    }
    LOGGER.info("Starting SMTP server on port {}", port);
    server = new SMTPServer(new SimpleMessageListenerAdapter(this));
    server.setPort(port);
    server.start();
    String localDomains = (String) context.getProperties().get(LOCAL_DOMAINS);
    if (localDomains == null) {
      localDomains = "localhost";
    }
    domains.clear();
    for (String domain : StringUtils.split(localDomains, ';')) {
      domains.add(domain);
    }
  }

  public void deactivate(ComponentContext context) throws Exception {
    LOGGER.info("Stopping SMTP server");
    server.stop();
  }

  /**
   *
   * {@inheritDoc}
   *
   * @see org.subethamail.smtp.helper.SimpleMessageListener#accept(java.lang.String,
   *      java.lang.String)
   */
  public boolean accept(String from, String recipient) {
    Session session = null;
    try {
      session = slingRepository.loginAdministrative(null);
      List<String> paths = getLocalPath(session, recipient);
      return paths.size() > 0;
    } catch (Exception e) {
      LOGGER.error("Develier message with this handler ", e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
    return false;
  }

  /**
   * @param recipient
   * @return
   */
  private List<String> getLocalPath(Session session, String recipient) {
    // assume recipient is a fully qualified email address of the form xxx@foo.com
    String[] parts = StringUtils.split(recipient, '@');
    List<String> localPaths = new ArrayList<String>();
    if (domains.contains(parts[1])) {
      List<String> recipients = messagingService.expandAliases(parts[0]);
      for (String localRecipient : recipients) {
        try {
          String path = messagingService.getFullPathToStore(localRecipient, session);
          if (path != null && path.length() > 0) {
            localPaths.add(path);
          }
        } catch (Exception ex) {
          LOGGER.warn("Failed to expand recipient {} ", localRecipient, ex);
        }
      }
    }
    return localPaths;
  }

  public void deliver(String from, String recipient, InputStream data)
      throws TooMuchDataException, IOException {
    LOGGER.info("Got message FROM: " + from + " TO: " + recipient);
    Session session = null;
    try {
      session = slingRepository.loginAdministrative(null);

      List<String> paths = getLocalPath(session, recipient);
      if (paths.size() > 0) {
        Map<String, Object> mapProperties = new HashMap<String, Object>();
        mapProperties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            MessageConstants.SAKAI_MESSAGE_RT);
        mapProperties.put(MessageConstants.PROP_SAKAI_READ, false);
        mapProperties.put(MessageConstants.PROP_SAKAI_FROM, from);
        mapProperties.put(MessageConstants.PROP_SAKAI_MESSAGEBOX,
            MessageConstants.BOX_INBOX);
        Node createdMessage = writeMessage(session, mapProperties, data, paths.get(0));
        if (createdMessage != null) {
          String messagePath = createdMessage.getPath();
          String messageId = createdMessage.getProperty("message-id").getString();
          LOGGER.info("Created message {} at: {} ", messageId, messagePath);

          // we might want alias expansion
          for (int i = 1; i < paths.size(); i++) {
            String targetPath = paths.get(i);
            messagingService.copyMessageNode(createdMessage, targetPath);
          }
        }
        if (session.hasPendingChanges()) {
          session.save();
        }

      }
    } catch (RepositoryException e) {
      LOGGER.error("Unable to write message", e);
      throw new IOException("Message can not be written to repository");
    } catch (MessagingException e) {
      LOGGER.error("Unable to write message", e);
      throw new IOException("Message can not be written to repository");
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Node writeMessage(Session session, Map<String, Object> mapProperties,
      InputStream data, String storePath) throws MessagingException, RepositoryException, IOException {
    InternetHeaders internetHeaders = new InternetHeaders(data);
    // process the headers into a map.
    for ( Enumeration<Header> e = internetHeaders.getAllHeaders(); e.hasMoreElements(); ) {
      Header h = e.nextElement();
      String name = h.getName();
      String[] values = internetHeaders.getHeader(name);
      if ( values != null) {
        if ( values.length == 1 ) {
          mapProperties.put("sakai:"+name.toLowerCase(), values[0]);
        } else {
          mapProperties.put("sakai:"+name.toLowerCase(), values);
        }
      }
    }
    String[] contentType = internetHeaders.getHeader("content-type");
    if (contentType != null && contentType.length > 0
        && contentType[0].contains("boundary") && contentType[0].contains("multipart/")) {
        MimeMultipart multipart = new MimeMultipart(new SMTPDataSource(contentType[0],
            data));
        Node message = messagingService.create(session, mapProperties,
            (String) mapProperties.get("sakai:message-id"), storePath);
        writeMultipartToNode(session, message, multipart);
        return message;
    } else {
      Node node = messagingService.create(session, mapProperties);
      // set up to stream the body.
      ValueFactory valueFactory = session.getValueFactory();
      Binary bin = valueFactory.createBinary(data);
      node.setProperty(MessageConstants.PROP_SAKAI_BODY, bin);
      session.save();
      return node;
    }
  }

  private void writeMultipartToNode(Session session, Node message, MimeMultipart multipart)
      throws RepositoryException, MessagingException, IOException {
    int count = multipart.getCount();
    for (int i = 0; i < count; i++) {
      createChildNodeForPart(session, i, multipart.getBodyPart(i), message);
    }
  }

  private boolean isTextType(BodyPart part) throws MessagingException {
    return part.getSize() < MAX_PROPERTY_SIZE
        && part.getContentType().toLowerCase().startsWith("text/");
  }

  private void createChildNodeForPart(Session session, int index, BodyPart part,
      Node message) throws RepositoryException, MessagingException, IOException {
    String childName = String.format("part%1$03d", index);
    if (part.getContentType().toLowerCase().startsWith("multipart/")) {
      Node childNode = message.addNode(childName);
      writePartPropertiesToNode(part, childNode);
      MimeMultipart multi = new MimeMultipart(new SMTPDataSource(part.getContentType(),
          part.getInputStream()));
      writeMultipartToNode(session, childNode, multi);
      return;
    }

    if (!isTextType(part)) {
      writePartAsFile(session, part, childName, message);
      return;
    }

    Node childNode = message.addNode(childName);
    writePartPropertiesToNode(part, childNode);
    ValueFactory valueFactory = session.getValueFactory();
    Binary bin = valueFactory.createBinary(part.getInputStream());
    childNode.setProperty(MessageConstants.PROP_SAKAI_BODY, bin);
    session.save();
  }

  private void writePartAsFile(Session session, BodyPart part, String nodeName,
      Node parentNode) throws RepositoryException, MessagingException, IOException {
    Node fileNode = parentNode.addNode(nodeName, "nt:file");
    Node resourceNode = fileNode.addNode("jcr:content", "nt:resource");
    resourceNode.setProperty("jcr:mimeType", part.getContentType());
    resourceNode.setProperty("jcr:data",
        session.getValueFactory().createBinary(part.getInputStream()));
    resourceNode.setProperty("jcr:lastModified", Calendar.getInstance());
  }

  @SuppressWarnings("unchecked")
  private void writePartPropertiesToNode(BodyPart part, Node childNode)
      throws MessagingException, RepositoryException {
    Enumeration<Header> headers = part.getAllHeaders();
    while (headers.hasMoreElements()) {
      Header header = headers.nextElement();
      childNode.setProperty(header.getName(), header.getValue());
    }
  }

}
