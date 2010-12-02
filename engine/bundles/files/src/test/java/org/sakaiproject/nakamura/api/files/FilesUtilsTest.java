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
package org.sakaiproject.nakamura.api.files;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.api.files.FileUtils.resolveNode;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockProperty;
import org.apache.sling.commons.testing.jcr.MockPropertyIterator;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Test;
import org.sakaiproject.nakamura.api.site.SiteService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;

/**
 *
 */
public class FilesUtilsTest {

  @Test
  public void testWriteFileNode() throws JSONException, RepositoryException,
      UnsupportedEncodingException, IOException {
    Session session = mock(Session.class);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Writer w = new PrintWriter(baos);
    JSONWriter write = new JSONWriter(w);
    SiteService siteService = mock(SiteService.class);

    Node node = createFileNode();

    FileUtils.writeFileNode(node, session, write, siteService);

    w.flush();
    String s = baos.toString("UTF-8");
    JSONObject j = new JSONObject(s);

    assertFileNodeInfo(j);
  }

  /**
   * @throws JSONException
   * 
   */
  private void assertFileNodeInfo(JSONObject j) throws JSONException {
    assertEquals("/path/to/file.doc", j.getString("jcr:path"));
    assertEquals("file.doc", j.getString("jcr:name"));
    assertEquals("bar", j.getString("foo"));
    assertEquals("text/plain", j.getString("jcr:mimeType"));
    assertEquals(12345, j.getLong("jcr:data"));
  }

  private Node createFileNode() throws ValueFormatException, RepositoryException {
    Calendar cal = Calendar.getInstance();

    Node contentNode = mock(Node.class);
    Property dateProp = mock(Property.class);
    when(dateProp.getDate()).thenReturn(cal);
    Property lengthProp = mock(Property.class);
    when(lengthProp.getLength()).thenReturn(12345L);

    Property mimetypeProp = mock(Property.class);
    when(mimetypeProp.getString()).thenReturn("text/plain");

    when(contentNode.getProperty(JcrConstants.JCR_LASTMODIFIED)).thenReturn(dateProp);
    when(contentNode.getProperty(JcrConstants.JCR_MIMETYPE)).thenReturn(mimetypeProp);
    when(contentNode.getProperty(JcrConstants.JCR_DATA)).thenReturn(lengthProp);
    when(contentNode.hasProperty(JcrConstants.JCR_DATA)).thenReturn(true);

    Node node = mock(Node.class);

    Property fooProp = new MockProperty("foo");
    fooProp.setValue("bar");
    List<Property> propertyList = new ArrayList<Property>();
    propertyList.add(fooProp);
    MockPropertyIterator propertyIterator = new MockPropertyIterator(
        propertyList.iterator());

    when(node.getProperties()).thenReturn(propertyIterator);
    when(node.hasNode("jcr:content")).thenReturn(true);
    when(node.getNode("jcr:content")).thenReturn(contentNode);
    when(node.getPath()).thenReturn("/path/to/file.doc");
    when(node.getName()).thenReturn("file.doc");

    return node;
  }

  @Test
  public void testWriteLinkNode() throws JSONException, RepositoryException, IOException {
    Session session = mock(Session.class);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Writer w = new PrintWriter(baos);
    JSONWriter write = new JSONWriter(w);
    SiteService siteService = mock(SiteService.class);

    Node node = new MockNode("/path/to/link");
    node.setProperty(FilesConstants.SAKAI_LINK, "uuid");
    node.setProperty("foo", "bar");
    Node fileNode = createFileNode();
    when(session.getNodeByIdentifier("uuid")).thenReturn(fileNode);

    FileUtils.writeLinkNode(node, session, write, siteService);
    w.flush();
    String s = baos.toString("UTF-8");
    JSONObject j = new JSONObject(s);

    assertEquals("/path/to/link", j.getString("jcr:path"));
    assertEquals("bar", j.getString("foo"));
    assertFileNodeInfo(j.getJSONObject("file"));

  }

  @Test
  public void testIsTag() throws RepositoryException {
    Node node = new MockNode("/path/to/tag");
    node.setProperty(SLING_RESOURCE_TYPE_PROPERTY, FilesConstants.RT_SAKAI_TAG);
    boolean result = FileUtils.isTag(node);
    assertEquals(true, result);

    node.setProperty(SLING_RESOURCE_TYPE_PROPERTY, "foobar");
    result = FileUtils.isTag(node);
    assertEquals(false, result);
  }

  @Test
  public void testCreateLinkNode() throws AccessDeniedException, RepositoryException {

    Node fileNode = createFileNode();
    Session session = mock(Session.class);
    Session adminSession = mock(Session.class);
    SlingRepository slingRepository = mock(SlingRepository.class);
    String linkPath = "/path/to/link";

    when(session.getUserID()).thenReturn("alice");
    when(fileNode.getSession()).thenReturn(session);
    NodeType[] nodeTypes = new NodeType[0];
    when(fileNode.getMixinNodeTypes()).thenReturn(nodeTypes);

    when(session.getItem(fileNode.getPath())).thenReturn(fileNode);
    when(adminSession.getItem(fileNode.getPath())).thenReturn(fileNode);
    when(slingRepository.loginAdministrative(null)).thenReturn(adminSession);
    when(adminSession.hasPendingChanges()).thenReturn(true);
    when(session.hasPendingChanges()).thenReturn(true);

    // link
    Node linkNode = mock(Node.class);
    when(session.itemExists(linkPath)).thenReturn(true);
    when(session.getItem(linkPath)).thenReturn(linkNode);
    NodeType nodeType = mock(NodeType.class);
    when(linkNode.getPrimaryNodeType()).thenReturn(nodeType);
    when(nodeType.getName()).thenReturn("nt:unstructured");

    FileUtils.createLink(fileNode, linkPath, null, slingRepository);

    verify(fileNode).addMixin(FilesConstants.REQUIRED_MIXIN);
    verify(session).save();
    verify(adminSession).save();
    verify(adminSession).logout();
  }

  @Test
  public void testWriteSiteInfo() throws JSONException, RepositoryException, IOException {
    Node siteNode = new MockNode("/sites/foo");
    SiteService siteService = mock(SiteService.class);
    when(siteService.getMemberCount(siteNode)).thenReturn(11);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Writer w = new PrintWriter(baos);
    JSONWriter write = new JSONWriter(w);

    FileUtils.writeSiteInfo(siteNode, write, siteService);
    w.flush();

    String s = baos.toString("UTF-8");
    JSONObject o = new JSONObject(s);
    assertEquals("11", o.get("member-count"));
    assertEquals(siteNode.getPath(), o.get("jcr:path"));

  }

  @Test
  public void testResolveNode() throws RepositoryException {
    Session session = mock(Session.class);

    // test IllegalArgumentException for null pathOrIdentifier
    try {
      @SuppressWarnings("unused")
      Node node = resolveNode(null, session);
    } catch (IllegalArgumentException e) {
      assertNotNull(
          "IllegalArgumentException should be thrown for null pathOrIdentifier", e);
    }

    // test IllegalArgumentException for empty pathOrIdentifier
    try {
      @SuppressWarnings("unused")
      Node node = resolveNode("", session);
    } catch (IllegalArgumentException e) {
      assertNotNull(
          "IllegalArgumentException should be thrown for empty pathOrIdentifier", e);
    }

    // test IllegalArgumentException for null Session
    try {
      @SuppressWarnings("unused")
      Node node = resolveNode("/foo", null);
    } catch (IllegalArgumentException e) {
      assertNotNull("IllegalArgumentException should be thrown for null Session", e);
    }

    // test path not found (i.e. fully qualified path use case)
    when(session.getNode(anyString())).thenThrow(new PathNotFoundException());
    try {
      Node node = resolveNode("/foo/bar", session);
      assertEquals("Node should resolve to null", node, null);
    } catch (Throwable e) {
      assertEquals("No exception should be thrown for PathNotFoundException", e, null);
    }

    // test item not found (i.e. uuid or poolId use case)
    when(session.getNodeByIdentifier(anyString())).thenThrow(new ItemNotFoundException());
    try {
      Node node = resolveNode("UUID1234", session);
      assertEquals("Node should resolve to null", node, null);
    } catch (Throwable e) {
      assertEquals("No exception should be thrown for ItemNotFoundException", e, null);
    }

    // test path found fully qualified
    session = mock(Session.class);
    Node fullyQualifiedNode = mock(Node.class, "fullyQualifiedNode");
    when(session.getNode(startsWith("/"))).thenReturn(fullyQualifiedNode);
    try {
      Node node = resolveNode("/should/exist", session);
      assertEquals("Node should resolve to modelNode", node, fullyQualifiedNode);
    } catch (Throwable e) {
      assertEquals("No exception should be thrown", e, null);
    }

    // test path found UUID
    Node uuidNode = mock(Node.class, "uuidNode");
    when(session.getNodeByIdentifier("UUID1234")).thenReturn(uuidNode);
    try {
      Node node = resolveNode("UUID1234", session);
      assertEquals("Node should resolve to modelNode", node, uuidNode);
    } catch (Throwable e) {
      assertEquals("No exception should be thrown", e, null);
    }

    // test path found poolId
    Node poolIdNode = mock(Node.class, "poolIdNode");
    // see CreateContentPoolServlet.generatePoolId() method
    when(session.getNode("/_p/k/dg/dd/nr/poolId1234")).thenReturn(poolIdNode);
    try {
      Node node = resolveNode("poolId1234", session);
      assertEquals("Node should resolve to modelNode", node, poolIdNode);
    } catch (Throwable e) {
      assertEquals("No exception should be thrown", e, null);
    }
  }

}
