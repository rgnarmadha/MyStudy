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
package org.sakaiproject.nakamura.docproxy.disk;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_LOCATION;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.RT_EXTERNAL_REPOSITORY_DOCUMENT;

import junit.framework.Assert;

import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.apache.sling.commons.testing.osgi.MockBundle;
import org.apache.sling.commons.testing.osgi.MockComponentContext;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.docproxy.DocProxyConstants;
import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata;
import org.sakaiproject.nakamura.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;

/**
 *
 */
public class DiskProcessorTest {

  public static final String TEST_STRING = "K2 docProxy test resource";
  private DiskProcessor diskProcessor;
  private String currPath;
  private MockNode proxyNode;

  @Before
  public void setUp() throws Exception {
    // Start with a new processor.
    diskProcessor = new DiskProcessor();

    String readmePath = getClass().getClassLoader().getResource("README").getPath();
    currPath = readmePath.substring(0, readmePath.lastIndexOf("/"));

    proxyNode = new MockNode("/docproxy/disk");
    proxyNode.setProperty(REPOSITORY_LOCATION, currPath);
  }

  @Test
  public void testGet() throws RepositoryException, UnsupportedEncodingException,
      IOException, DocProxyException {
    // Create the proxy Node.
    Node proxyNode = createMock(Node.class);
    Property locationProp = createMock(Property.class);
    expect(locationProp.getString()).andReturn(currPath).anyTimes();
    expect(proxyNode.getProperty(DocProxyConstants.REPOSITORY_LOCATION)).andReturn(
        locationProp);
    replay(locationProp, proxyNode);
    ExternalDocumentResult result = diskProcessor.getDocument(proxyNode, "README");
    InputStream in = result.getDocumentInputStream(0, "zach");
    String content = IOUtils.readFully(in, "UTF-8");
    Assert.assertEquals(TEST_STRING, content);

    // Properties from .json
    Map<String, Object> props = result.getProperties();
    Assert.assertEquals("bar", props.get("foo"));
    Assert.assertEquals(123, props.get("num"));
  }

  @Test
  public void testUpdateDocument() throws DocProxyException, PathNotFoundException,
      RepositoryException, IOException {

    // Create a new file.
    String path = "README-copy";
    Node newNode = createFile(diskProcessor, proxyNode, path, TEST_STRING);

    // Get the file
    ExternalDocumentResult result = diskProcessor.getDocument(newNode, path);
    InputStream in = result.getDocumentInputStream(0, "zach");

    // Read content
    String content = IOUtils.readFully(in, "UTF-8");
    Assert.assertEquals(content, TEST_STRING);
  }

  @Test
  public void testSearch() throws PathNotFoundException, UnsupportedEncodingException,
      RepositoryException, DocProxyException {
    // Mock proxyNode
    Node proxyNode = createMock(Node.class);
    Property locationProp = createMock(Property.class);
    expect(locationProp.getString()).andReturn(currPath).anyTimes();
    expect(proxyNode.getProperty(DocProxyConstants.REPOSITORY_LOCATION)).andReturn(
        locationProp).anyTimes();
    replay(locationProp, proxyNode);

    // Create a couple of files.
    createFile(diskProcessor, proxyNode, "test-disk-search-1-foo", "alfa");
    createFile(diskProcessor, proxyNode, "test-disk-search-2-foo", "beta");
    createFile(diskProcessor, proxyNode, "test-disk-search-3-foo", "charlie");
    createFile(diskProcessor, proxyNode, ".FOOBAR-foo", "");

    // The disk search only matches filenames starting with a term..
    Map<String, Object> searchProperties = new HashMap<String, Object>();
    searchProperties.put("starts-with", "test-disk-search-");
    searchProperties.put("ends-with", "foo");

    // Perform actual search.
    Iterator<ExternalDocumentResult> results = diskProcessor.search(proxyNode,
        searchProperties);

    // Quickly loop over the results to get a count
    int size = 0;
    while (results.hasNext()) {
      results.next();
      size++;
    }

    Assert.assertEquals(3, size);
  }

  @Test
  public void testContentType() throws PathNotFoundException,
      UnsupportedEncodingException, RepositoryException, DocProxyException {
    Node textNode = createFile(diskProcessor, proxyNode, "test-contentType.txt",
        TEST_STRING);
    ExternalDocumentResultMetadata meta = diskProcessor.getDocumentMetadata(textNode,
        "test-contentType.txt");
    Assert.assertEquals("text/plain", meta.getContentType());

    Node htmlNode = createFile(diskProcessor, proxyNode, "test-contentType.html",
        "<html><head><title>foo</title></head></html>");
    ExternalDocumentResultMetadata htmlMeta = diskProcessor.getDocumentMetadata(htmlNode,
        "test-contentType.html");
    Assert.assertEquals("text/html", htmlMeta.getContentType());

  }

  @Test
  public void testFaultyRepositoryLocation() throws ValueFormatException,
      RepositoryException, UnsupportedEncodingException, IOException {
    Node proxyNode = createMock(Node.class);
    Property locationProp = createMock(Property.class);
    expect(locationProp.getString()).andReturn(currPath).anyTimes();
    expect(proxyNode.getProperty(DocProxyConstants.REPOSITORY_LOCATION)).andThrow(
        new RepositoryException());
    replay(locationProp, proxyNode);
    try {
      diskProcessor.getDocument(proxyNode, "README");
    } catch (DocProxyException e) {
      assertEquals(500, e.getCode());
      assertEquals("Unable to read from node property.", e.getMessage());
    }
  }

  @Test
  public void testWritingtoJCR() throws PathNotFoundException,
      UnsupportedEncodingException, RepositoryException, DocProxyException {
    MockBundle bundle = new MockBundle(111);
    MockComponentContext context = new MockComponentContext(bundle);
    context.setProperty("createJCRNodes", true);
    diskProcessor.activate(context);

    String docPath = "path/to/document";

    MockNode documentNode = new MockNode(proxyNode.getPath() + "/" + docPath);
    Session session = createMock(Session.class);
    expect(session.itemExists(proxyNode.getPath() + "/" + docPath)).andReturn(true);
    expect(session.getItem(proxyNode.getPath() + "/" + docPath)).andReturn(documentNode);

    ValueFactory factory = createMock(ValueFactory.class);
    expect(factory.createValue(isA(String.class))).andReturn(new MockValue("someuuid"));

    expect(session.getValueFactory()).andReturn(factory);
    expect(session.hasPendingChanges()).andReturn(true);
    session.save();

    replay(factory);
    replay(session);
    proxyNode.setSession(session);

    createFile(diskProcessor, proxyNode, docPath, "random content");

    assertEquals(documentNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString(),
        RT_EXTERNAL_REPOSITORY_DOCUMENT);
  }

  public static Node createFile(DiskProcessor processor, Node proxyNode, String path,
      String content) throws PathNotFoundException, RepositoryException,
      UnsupportedEncodingException, DocProxyException {
    ByteArrayInputStream stream = new ByteArrayInputStream(content.getBytes("UTF-8"));
    return createFile(processor, proxyNode, path, stream);
  }

  public static Node createFile(DiskProcessor processor, Node proxyNode, String path,
      InputStream documentStream) throws PathNotFoundException, RepositoryException,
      DocProxyException {
    processor.updateDocument(proxyNode, path, null, documentStream, -1);

    return proxyNode;

  }

}
