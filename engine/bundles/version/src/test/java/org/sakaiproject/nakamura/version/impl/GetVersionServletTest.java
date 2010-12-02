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
package org.sakaiproject.nakamura.version.impl;

import junit.framework.Assert;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.IOException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Workspace;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 *
 */
public class GetVersionServletTest  extends AbstractEasyMockTest {

  private GetVersionServlet getVersionServlet;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    super.setUp();
    getVersionServlet = new GetVersionServlet();
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }


  @SuppressWarnings("unchecked")
  @Test
  public void test() throws ServletException, IOException, UnsupportedRepositoryOperationException, RepositoryException {
    SlingHttpServletRequest request = createNiceMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createNiceMock(SlingHttpServletResponse.class);
    RequestPathInfo requestPathInfo = createNiceMock(RequestPathInfo.class);
    Resource resource = createNiceMock(Resource.class);
    Node node = createNiceMock(Node.class);
    VersionHistory versionHistory = createNiceMock(VersionHistory.class);
    Version version = createNiceMock(Version.class);
    Node frozenNode = createNiceMock(Node.class);
    Property resourceTypeProperty = createNiceMock(Property.class);
    Capture<Resource> capturedResource = new Capture<Resource>();
    RequestDispatcher requestDispatcher = createNiceMock(RequestDispatcher.class);
    Capture<ServletRequest> capturedRequest = new Capture<ServletRequest>();
    Capture<ServletResponse> capturedResponse = new Capture<ServletResponse>();
    VersionManager versionManager = createNiceMock(VersionManager.class);
    Session session = createNiceMock(Session.class);
    Workspace workspace = createNiceMock(Workspace.class);

    EasyMock.expect(request.getRequestPathInfo()).andReturn(requestPathInfo);
    EasyMock.expect(requestPathInfo.getSelectorString()).andReturn("version.,1.1,.tidy.json");
    EasyMock.expect(request.getResource()).andReturn(resource);
    EasyMock.expect(resource.adaptTo(Node.class)).andReturn(node);
    EasyMock.expect(node.getPath()).andReturn("/foo");
    EasyMock.expect(node.getSession()).andReturn(session);
    EasyMock.expect(session.getWorkspace()).andReturn(workspace);
    EasyMock.expect(workspace.getVersionManager()).andReturn(versionManager);
    EasyMock.expect(versionManager.getVersionHistory("/foo")).andReturn(versionHistory);
    EasyMock.expect(versionHistory.getVersion("1.1")).andReturn(version);
    EasyMock.expect(version.getNode(JcrConstants.JCR_FROZENNODE)).andReturn(frozenNode);
    EasyMock.expect(frozenNode.getPath()).andReturn("/testnode");
    EasyMock.expect(frozenNode.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)).andReturn(true);
    EasyMock.expect(frozenNode.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)).andReturn(resourceTypeProperty);
    EasyMock.expect(resourceTypeProperty.getString()).andReturn("sakai/testing");
    EasyMock.expect(frozenNode.hasProperty(JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY)).andReturn(true);
    EasyMock.expect(frozenNode.getProperty(JcrResourceConstants.SLING_RESOURCE_SUPER_TYPE_PROPERTY)).andReturn(resourceTypeProperty);
    EasyMock.expect(resourceTypeProperty.getString()).andReturn("sakai/super-type-testing");
    EasyMock.expect(request.getRequestDispatcher(EasyMock.capture(capturedResource))).andReturn(requestDispatcher);
    requestDispatcher.forward(EasyMock.capture(capturedRequest), EasyMock.capture(capturedResponse));
    EasyMock.expectLastCall();


    replay();
    getVersionServlet.doGet(request, response);

    Assert.assertTrue(capturedRequest.hasCaptured());
    Assert.assertTrue(capturedResource.hasCaptured());
    Assert.assertTrue(capturedResponse.hasCaptured());

    Resource cresource = capturedResource.getValue();

    Node capturedNode = cresource.adaptTo(Node.class);
    Assert.assertEquals(capturedNode, frozenNode);

    Map<String, String> map = cresource.adaptTo(Map.class);
    Assert.assertNotNull(map);




    verify();
  }



}
