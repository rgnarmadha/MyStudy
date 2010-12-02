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
package org.sakaiproject.nakamura.files.servlets;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockPropertyDefinition;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.files.pool.CreateContentPoolServlet;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;

/**
 *
 */
public class TagOperationTest {

  private TagOperation operation;
  private SlingRepository slingRepo;
  private Session session;

  @Before
  public void setUp() {
    operation = new TagOperation();
    slingRepo = mock(SlingRepository.class);
    session = mock(Session.class);
    operation.slingRepository = slingRepo;
  }

  @Test
  public void testAnonRequest() throws RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn(UserConstants.ANON_USERID);
    HtmlResponse response = new HtmlResponse();
    operation.doRun(request, response, null);
    assertEquals(403, response.getStatusCode());
  }

  @Test
  public void testMissingResource() throws RepositoryException {
    // This shouldn't really happen, but hey!
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    HtmlResponse response = new HtmlResponse();

    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    Resource resource = mock(Resource.class);
    when(resource.adaptTo(Node.class)).thenReturn(null);
    when(request.getResource()).thenReturn(resource);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(request.getRemoteUser()).thenReturn("john");

    operation.doRun(request, response, null);

    assertEquals(400, response.getStatusCode());
  }

  @Test
  public void testMissingParams() throws RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    HtmlResponse response = new HtmlResponse();

    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    Resource resource = mock(Resource.class);
    Node node = new MockNode("/bla/bla");
    when(resource.adaptTo(Node.class)).thenReturn(node);

    when(request.getResource()).thenReturn(resource);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(request.getRequestParameter("key")).thenReturn(null);
    when(request.getRemoteUser()).thenReturn("john");

    operation.doRun(request, response, null);

    assertEquals(400, response.getStatusCode());
  }

  @Test
  public void testProperUUID() throws ItemNotFoundException, RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    HtmlResponse response = new HtmlResponse();

    ResourceResolver resolver = mock(ResourceResolver.class);

    String uuid = "cafede-cafede-cafede-cafede-cafede";
    Resource resource = mock(Resource.class);

    // The tagnode
    Node tagNode = new MockNode("/path/to/tag");
    tagNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, FilesConstants.RT_SAKAI_TAG);
    tagNode.setProperty(FilesConstants.SAKAI_TAG_NAME, "urban");

    // The file we want to tag.
    Node fileNode = mock(Node.class);
    when(fileNode.getPath()).thenReturn("/path/to/file");
    NodeType type = mock(NodeType.class);
    when(type.getName()).thenReturn("foo");
    when(fileNode.getMixinNodeTypes()).thenReturn(new NodeType[] { type });
    Property tagsProp = mock(Property.class);
    MockPropertyDefinition tagsPropDef = new MockPropertyDefinition(false);
    Value v = new MockValue("uuid-to-other-tag");
    when(tagsProp.getDefinition()).thenReturn(tagsPropDef);
    when(tagsProp.getValue()).thenReturn(v);
    when(fileNode.getProperty(FilesConstants.SAKAI_TAGS)).thenReturn(tagsProp);
    when(fileNode.hasProperty(FilesConstants.SAKAI_TAGS)).thenReturn(true);

    // Stuff to check if this is a correct request
    when(session.getNodeByIdentifier(uuid)).thenReturn(tagNode);
    RequestParameter uuidParam = mock(RequestParameter.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);
    when(resource.adaptTo(Node.class)).thenReturn(fileNode);
    when(uuidParam.getString()).thenReturn(uuid);
    when(request.getResource()).thenReturn(resource);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(request.getRequestParameter("key")).thenReturn(uuidParam);
    when(request.getRemoteUser()).thenReturn("john");

    // Actual tagging procedure
    Session adminSession = mock(Session.class);
    when(adminSession.getItem(fileNode.getPath())).thenReturn(fileNode);
    ValueFactory valueFactory = mock(ValueFactory.class);
    Value newValue = new MockValue("uuid-of-tag");
    when(valueFactory.createValue(Mockito.anyString(), Mockito.anyInt())).thenReturn(
        newValue).thenReturn(newValue);
    when(adminSession.getValueFactory()).thenReturn(valueFactory)
        .thenReturn(valueFactory);
    when(slingRepo.loginAdministrative(null)).thenReturn(adminSession);

    when(adminSession.hasPendingChanges()).thenReturn(true);
    operation.doRun(request, response, null);

    assertEquals(200, response.getStatusCode());
    verify(adminSession).save();
    verify(adminSession).logout();

  }

  @Test
  public void testProperPath() throws ItemNotFoundException, RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    HtmlResponse response = new HtmlResponse();

    ResourceResolver resolver = mock(ResourceResolver.class);

    String path = "/path/to/tag";
    Resource resource = mock(Resource.class);

    // The tagnode
    Node tagNode = new MockNode(path);
    tagNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, FilesConstants.RT_SAKAI_TAG);
    tagNode.setProperty(FilesConstants.SAKAI_TAG_NAME, "urban");

    // The file we want to tag.
    Node fileNode = mock(Node.class);
    when(fileNode.getPath()).thenReturn("/path/to/file");
    NodeType type = mock(NodeType.class);
    when(type.getName()).thenReturn("foo");
    when(fileNode.getMixinNodeTypes()).thenReturn(new NodeType[] { type });
    Property tagsProp = mock(Property.class);
    MockPropertyDefinition tagsPropDef = new MockPropertyDefinition(false);
    Value v = new MockValue("uuid-to-other-tag");
    when(tagsProp.getDefinition()).thenReturn(tagsPropDef);
    when(tagsProp.getValue()).thenReturn(v);
    when(fileNode.getProperty(FilesConstants.SAKAI_TAGS)).thenReturn(tagsProp);
    when(fileNode.hasProperty(FilesConstants.SAKAI_TAGS)).thenReturn(true);

    // Stuff to check if this is a correct request
    when(session.getNode(path)).thenReturn(tagNode);
    RequestParameter pathParam = mock(RequestParameter.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);
    when(resource.adaptTo(Node.class)).thenReturn(fileNode);
    when(pathParam.getString()).thenReturn(path);
    when(request.getResource()).thenReturn(resource);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(request.getRequestParameter("key")).thenReturn(pathParam);
    when(request.getRemoteUser()).thenReturn("john");

    // Actual tagging procedure
    Session adminSession = mock(Session.class);
    when(adminSession.getItem(fileNode.getPath())).thenReturn(fileNode);
    ValueFactory valueFactory = mock(ValueFactory.class);
    Value newValue = new MockValue("uuid-of-tag");
    when(valueFactory.createValue(Mockito.anyString(), Mockito.anyInt())).thenReturn(
        newValue).thenReturn(newValue);
    when(adminSession.getValueFactory()).thenReturn(valueFactory)
        .thenReturn(valueFactory);
    when(slingRepo.loginAdministrative(null)).thenReturn(adminSession);

    when(adminSession.hasPendingChanges()).thenReturn(true);
    operation.doRun(request, response, null);

    assertEquals(200, response.getStatusCode());
    verify(adminSession).save();
    verify(adminSession).logout();

  }

  @Test
  public void testProperPoolId() throws ItemNotFoundException, RepositoryException,
      NoSuchAlgorithmException, UnsupportedEncodingException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    HtmlResponse response = new HtmlResponse();

    ResourceResolver resolver = mock(ResourceResolver.class);

    String poolId = "foobarbaz";
    Resource resource = mock(Resource.class);

    // The tagnode
    Node tagNode = new MockNode("/path/to/tag");
    tagNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, FilesConstants.RT_SAKAI_TAG);
    tagNode.setProperty(FilesConstants.SAKAI_TAG_NAME, "urban");

    // The file we want to tag.
    Node fileNode = mock(Node.class);
    when(fileNode.getPath()).thenReturn("/path/to/file");
    NodeType type = mock(NodeType.class);
    when(type.getName()).thenReturn("foo");
    when(fileNode.getMixinNodeTypes()).thenReturn(new NodeType[] { type });
    Property tagsProp = mock(Property.class);
    MockPropertyDefinition tagsPropDef = new MockPropertyDefinition(false);
    Value v = new MockValue("uuid-to-other-tag");
    when(tagsProp.getDefinition()).thenReturn(tagsPropDef);
    when(tagsProp.getValue()).thenReturn(v);
    when(fileNode.getProperty(FilesConstants.SAKAI_TAGS)).thenReturn(tagsProp);
    when(fileNode.hasProperty(FilesConstants.SAKAI_TAGS)).thenReturn(true);

    // Stuff to check if this is a correct request
    when(session.getNode(CreateContentPoolServlet.hash(poolId))).thenReturn(tagNode);
    RequestParameter poolIdParam = mock(RequestParameter.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);
    when(resource.adaptTo(Node.class)).thenReturn(fileNode);
    when(poolIdParam.getString()).thenReturn(poolId);
    when(request.getResource()).thenReturn(resource);
    when(request.getResourceResolver()).thenReturn(resolver);
    when(request.getRequestParameter("key")).thenReturn(poolIdParam);
    when(request.getRemoteUser()).thenReturn("john");

    // Actual tagging procedure
    Session adminSession = mock(Session.class);
    when(adminSession.getItem(fileNode.getPath())).thenReturn(fileNode);
    ValueFactory valueFactory = mock(ValueFactory.class);
    Value newValue = new MockValue("uuid-of-tag");
    when(valueFactory.createValue(Mockito.anyString(), Mockito.anyInt())).thenReturn(
        newValue).thenReturn(newValue);
    when(adminSession.getValueFactory()).thenReturn(valueFactory)
        .thenReturn(valueFactory);
    when(slingRepo.loginAdministrative(null)).thenReturn(adminSession);

    when(adminSession.hasPendingChanges()).thenReturn(true);
    operation.doRun(request, response, null);

    assertEquals(200, response.getStatusCode());
    verify(adminSession).save();
    verify(adminSession).logout();

  }
}
