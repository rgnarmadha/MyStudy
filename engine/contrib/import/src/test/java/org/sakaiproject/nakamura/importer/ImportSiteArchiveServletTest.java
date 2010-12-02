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
package org.sakaiproject.nakamura.importer;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_SMART_NULLS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Session;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ImportSiteArchiveServletTest {
  private ImportSiteArchiveServlet importSiteArchiveServlet;
  @Mock
  ServletConfig servletConfig;

  @Before
  public void setUp() throws Exception {
    importSiteArchiveServlet = new ImportSiteArchiveServlet();
    try {
      importSiteArchiveServlet.init(servletConfig);
    } catch (Exception e) {
      assertNull("init method should not throw any exceptions", e);
    }
  }

  @Test
  public void testDoPostNoSiteParam() {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class,
        withSettings().defaultAnswer(RETURNS_SMART_NULLS));
    when(request.getRequestParameter("site")).thenReturn(null);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    try {
      importSiteArchiveServlet.doPost(request, response);
      verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST),
          anyString());
    } catch (Exception e) {
      e.printStackTrace();
      assertNull("doPost method should not throw any exceptions", e);
    }
  }

  @Test
  public void testDoPostBadSiteParam() {
    RequestParameter siteParam = mock(RequestParameter.class, withSettings()
        .defaultAnswer(RETURNS_SMART_NULLS).name("siteParam"));
    when(siteParam.getString()).thenReturn("site/foo");
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class,
        withSettings().defaultAnswer(RETURNS_SMART_NULLS));
    when(request.getRequestParameter("site")).thenReturn(siteParam);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    when(response.isCommitted()).thenReturn(false);
    try {
      importSiteArchiveServlet.doPost(request, response);
      verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST),
          anyString());
    } catch (Exception e) {
      e.printStackTrace();
      assertNull("doPost method should not throw any exceptions", e);
    }
  }

  @Test
  public void testDoPostNoFileData() {
    RequestParameter siteParam = mock(RequestParameter.class, withSettings()
        .defaultAnswer(RETURNS_SMART_NULLS).name("siteParam"));
    when(siteParam.getString()).thenReturn("/site/foo");
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class,
        withSettings().defaultAnswer(RETURNS_SMART_NULLS));
    when(request.getRequestParameter("site")).thenReturn(siteParam);
    SlingHttpServletResponse response = mock(SlingHttpServletResponse.class);
    when(response.isCommitted()).thenReturn(false);
    // mock ResourceResolver which Request will return
    Session userSession = mock(Session.class);
    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(userSession);
    when(request.getResourceResolver()).thenReturn(resolver);
    try {
      importSiteArchiveServlet.doPost(request, response);
      verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST),
          anyString());
    } catch (Exception e) {
      e.printStackTrace();
      assertNull("doPost method should not throw any exceptions", e);
    }
  }

  // @Test
  // public void testDoPost() throws Exception {
  // // mock RequestParameter which returns a valid siteParam
  // RequestParameter siteParam = mock(RequestParameter.class, withSettings()
  // .defaultAnswer(RETURNS_SMART_NULLS).name("siteParam"));
  // when(siteParam.getString()).thenReturn("/site/foo");
  // // mock Request that returns valid siteParam
  // SlingHttpServletRequest request = mock(SlingHttpServletRequest.class,
  // withSettings().defaultAnswer(RETURNS_SMART_NULLS));
  // when(request.getRequestParameter("site")).thenReturn(siteParam);
  // // mock param which stubs a real InputStream from archive.zip test file
  // RequestParameter fileParam = mock(RequestParameter.class, withSettings()
  // .defaultAnswer(RETURNS_SMART_NULLS).name("fileParam"));
  // when(fileParam.getInputStream()).thenAnswer(new Answer<InputStream>() {
  // public InputStream answer(InvocationOnMock invocation) {
  // FileInputStream fis = null;
  // try {
  // File file = new File("archive.zip");
  // System.out.println("\nPATH:" + file.getAbsolutePath());
  // fis = new FileInputStream(file);
  // } catch (FileNotFoundException e) {
  // throw new Error(e);
  // }
  // return fis;
  // }
  // });
  // // Request returns a valid FileData RequestParameter[]
  // RequestParameter[] files = { fileParam };
  // when(request.getRequestParameters("Filedata")).thenReturn(files);
  // // mock Session that always claims itemExists == true
  // Session userSession = mock(Session.class, withSettings().defaultAnswer(
  // RETURNS_SMART_NULLS).name("userSession"));
  // when(userSession.itemExists(anyString())).thenReturn(true);
  // // mock Property that returns a valid SAKAI_FILENAME
  // Property sakaiFileNameProperty = mock(Property.class, withSettings()
  // .defaultAnswer(RETURNS_SMART_NULLS).name("sakaiFileNameProperty"));
  // when(sakaiFileNameProperty.getString()).thenReturn("foo");
  // // mock Property that returns a valid SLING_RESOURCE_TYPE_PROPERTY
  // Property slingResourceTypeProperty = mock(Property.class, withSettings()
  // .defaultAnswer(RETURNS_SMART_NULLS).name("slingResourceTypeProperty"));
  // when(slingResourceTypeProperty.getString()).thenReturn(
  // FilesConstants.RT_FILE_STORE);
  // // mock Property that returns a valid JCR_PRIMARYTYPE
  // Property jcrPrimaryTypeProperty = mock(Property.class, withSettings()
  // .defaultAnswer(RETURNS_SMART_NULLS).name("jcrPrimaryTypeProperty"));
  // when(jcrPrimaryTypeProperty.getString()).thenReturn(
  // JcrConstants.NT_UNSTRUCTURED);
  // // mock child Node to parent fileNode
  // Node contentNode = mock(Node.class, withSettings().defaultAnswer(
  // RETURNS_SMART_NULLS).name("contentNode"));
  // // mock fileNode that returns the mock Property, Session, and getPath
  // Node fileNode = mock(Node.class, withSettings().defaultAnswer(
  // RETURNS_SMART_NULLS).name("fileNode"));
  // when(fileNode.getProperty(FilesConstants.SAKAI_FILENAME)).thenReturn(
  // sakaiFileNameProperty);
  // when(
  // fileNode.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))
  // .thenReturn(true);
  // when(
  // fileNode.getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))
  // .thenReturn(slingResourceTypeProperty);
  // when(fileNode.getProperty(JcrConstants.JCR_PRIMARYTYPE)).thenReturn(
  // jcrPrimaryTypeProperty);
  // when(fileNode.getSession()).thenReturn(userSession);
  // when(fileNode.getPath()).thenReturn("/foo");
  // when(fileNode.getNode(JcrConstants.JCR_CONTENT)).thenReturn(contentNode);
  // when(userSession.getItem(anyString())).thenReturn(fileNode);
  // // mock ResourceResolver which Request will return
  // ResourceResolver resolver = mock(ResourceResolver.class);
  // when(resolver.adaptTo(Session.class)).thenReturn(userSession);
  // when(request.getResourceResolver()).thenReturn(resolver);
  // // inject mock ClusterTrackingService
  // ClusterTrackingService clusterTrackingService = mock(
  // ClusterTrackingService.class, withSettings().defaultAnswer(
  // RETURNS_SMART_NULLS));
  // when(clusterTrackingService.getClusterUniqueId()).thenReturn(
  // UUID.randomUUID().toString());
  // importSiteArchiveServlet.clusterTrackingService = clusterTrackingService;
  // // inject mock SlingRepository
  // SlingRepository slingRepository = mock(SlingRepository.class,
  // withSettings().defaultAnswer(RETURNS_SMART_NULLS));
  // Session adminSession = mock(Session.class, withSettings().defaultAnswer(
  // RETURNS_SMART_NULLS).name("adminSession"));
  // when(slingRepository.loginAdministrative(null)).thenReturn(adminSession);
  // when(adminSession.getNodeByUUID(anyString())).thenReturn(fileNode);
  // importSiteArchiveServlet.slingRepository = slingRepository;
  // // mock Response mainly for verify
  // SlingHttpServletResponse response = mock(SlingHttpServletResponse.class,
  // withSettings().defaultAnswer(RETURNS_SMART_NULLS));
  // when(response.isCommitted()).thenReturn(false);
  // try {
  // importSiteArchiveServlet.doPost(request, response);
  // verify(response).sendError(eq(HttpServletResponse.SC_OK), anyString());
  // } catch (Exception e) {
  // e.printStackTrace();
  // assertNull("doPost method should not throw any exceptions", e);
  // }
  // }
}
