package org.sakaiproject.nakamura.email.outgoing;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.apache.jackrabbit.JcrConstants;
import org.junit.Test;
import org.sakaiproject.nakamura.api.message.MessageConstants;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

public class JcrEmailDataSourceTest {

  @Test
  public void testGetName() throws Exception {
    Node node = createMock(Node.class);
    expect(node.getName()).andReturn("foo");

    replay(node);

    JcrEmailDataSource jeds = new JcrEmailDataSource(node);
    assertEquals("foo", jeds.getName());
  }

  @Test
  public void testGetNameException() throws Exception {
    Node node = createMock(Node.class);
    expect(node.getName()).andThrow(new RepositoryException());

    replay(node);

    JcrEmailDataSource jeds = new JcrEmailDataSource(node);
    assertNull(jeds.getName());
  }

  @Test
  public void testGetOutputStream() {
    Node node = null;

    JcrEmailDataSource jeds = new JcrEmailDataSource(node);
    try {
      jeds.getOutputStream();
      fail();
    } catch (IOException e) {
      assertEquals("This data source is designed for read only.", e.getMessage());
    }
  }

  @Test
  public void testGetContentType() throws Exception {
    NodeType nodeType = createMock(NodeType.class);
    expect(nodeType.getName()).andReturn(JcrConstants.NT_BASE);

    Property contentType = createMock(Property.class);
    expect(contentType.getString()).andReturn("text/plain");

    Node node = createMock(Node.class);
    expect(node.getPrimaryNodeType()).andReturn(nodeType);
    expect(node.hasProperty(MessageConstants.PROP_SAKAI_CONTENT_TYPE)).andReturn(true);
    expect(node.getProperty(MessageConstants.PROP_SAKAI_CONTENT_TYPE)).andReturn(
        contentType);

    replay(nodeType, contentType, node);

    JcrEmailDataSource jeds = new JcrEmailDataSource(node);
    assertEquals("text/plain", jeds.getContentType());
  }

  @Test
  public void testNoContentType() throws Exception {
    NodeType nodeType = createMock(NodeType.class);
    expect(nodeType.getName()).andReturn(JcrConstants.NT_BASE);

    Property contentType = createMock(Property.class);
    expect(contentType.getString()).andReturn("text/plain");

    Node node = createMock(Node.class);
    expect(node.getPrimaryNodeType()).andReturn(nodeType);
    expect(node.hasProperty(MessageConstants.PROP_SAKAI_CONTENT_TYPE)).andReturn(false);

    replay(nodeType, contentType, node);

    JcrEmailDataSource jeds = new JcrEmailDataSource(node);
    assertEquals("application/octet-stream", jeds.getContentType());
  }

  @Test
  public void testGetContentTypeException() throws Exception {
    NodeType nodeType = createMock(NodeType.class);
    expect(nodeType.getName()).andReturn(JcrConstants.NT_BASE);

    Property contentType = createMock(Property.class);
    expect(contentType.getString()).andReturn("text/plain");

    Node node = createMock(Node.class);
    expect(node.getPrimaryNodeType()).andReturn(nodeType);
    expect(node.hasProperty(MessageConstants.PROP_SAKAI_CONTENT_TYPE)).andThrow(
        new RepositoryException());

    replay(nodeType, contentType, node);

    JcrEmailDataSource jeds = new JcrEmailDataSource(node);
    assertEquals("application/octet-stream", jeds.getContentType());
  }

  @Test
  public void testGetContentTypeFile() throws Exception {
    NodeType nodeType = createMock(NodeType.class);
    expect(nodeType.getName()).andReturn(JcrConstants.NT_FILE);

    Property contentType = createMock(Property.class);
    expect(contentType.getString()).andReturn("text/plain");

    Node content = createMock(Node.class);
    expect(content.getProperty(JcrConstants.JCR_MIMETYPE)).andReturn(contentType);

    Node node = createMock(Node.class);
    expect(node.getPrimaryNodeType()).andReturn(nodeType);
    expect(node.getNode(JcrConstants.JCR_CONTENT)).andReturn(content);

    replay(nodeType, content, contentType, node);

    JcrEmailDataSource jeds = new JcrEmailDataSource(node);
    assertEquals("text/plain", jeds.getContentType());
  }

  @Test
  public void testGetInputStream() throws Exception {
    NodeType nodeType = createMock(NodeType.class);
    expect(nodeType.getName()).andReturn(JcrConstants.NT_BASE);

    String messageText = "Lorem ipsum dolor sit amet.";

    Binary contentBin = createMock(Binary.class);
    Property content = createMock(Property.class);
    expect(content.getBinary()).andReturn(contentBin);
    expect(contentBin.getStream()).andReturn(
        new ByteArrayInputStream(messageText.getBytes()));

    Node node = createMock(Node.class);
    expect(node.getPrimaryNodeType()).andReturn(nodeType);
    expect(node.getProperty(MessageConstants.PROP_SAKAI_ATTACHMENT_CONTENT)).andReturn(
        content);

    replay(nodeType, contentBin, content, node);

    JcrEmailDataSource jeds = new JcrEmailDataSource(node);
    InputStream is = jeds.getInputStream();
    byte[] b = new byte[messageText.length()];
    assertEquals(messageText.length(), is.read(b, 0, messageText.length()));
    assertArrayEquals(messageText.getBytes(), b);
  }

  @Test
  public void testGetInputStreamFile() throws Exception {
    NodeType nodeType = createMock(NodeType.class);
    expect(nodeType.getName()).andReturn(JcrConstants.NT_FILE);

    String messageText = "Lorem ipsum dolor sit amet.";

    Binary contentBin = createMock(Binary.class);
    Property content = createMock(Property.class);
    expect(content.getBinary()).andReturn(contentBin);
    expect(contentBin.getStream()).andReturn(
        new ByteArrayInputStream(messageText.getBytes()));

    Node contentNode = createMock(Node.class);
    expect(contentNode.getProperty(JcrConstants.JCR_DATA)).andReturn(content);

    Node node = createMock(Node.class);
    expect(node.getPrimaryNodeType()).andReturn(nodeType);
    expect(node.getNode(JcrConstants.JCR_CONTENT)).andReturn(contentNode);

    replay(nodeType, contentBin, content, contentNode, node);

    JcrEmailDataSource jeds = new JcrEmailDataSource(node);
    InputStream is = jeds.getInputStream();
    byte[] b = new byte[messageText.length()];
    assertEquals(messageText.length(), is.read(b, 0, messageText.length()));
    assertArrayEquals(messageText.getBytes(), b);
  }

  @Test
  public void testGetInputStreamException() throws Exception {
    Node node = createMock(Node.class);
    expect(node.getPrimaryNodeType()).andThrow(new RepositoryException());

    replay(node);

    JcrEmailDataSource jeds = new JcrEmailDataSource(node);
    try {
      jeds.getInputStream();
      fail();
    } catch (IOException ioe) {
      assertEquals("javax.jcr.RepositoryException", ioe.getCause().toString());
    }
  }
}
