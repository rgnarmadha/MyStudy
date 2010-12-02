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
package org.sakaiproject.nakamura.site.servlet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.api.site.SiteConstants.SAKAI_SKIN;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.site.ACMEGroupStructure;
import org.sakaiproject.nakamura.site.SiteServiceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 *
 */
public class SiteGetServletTest extends AbstractSiteServletTest {

  private Session adminSession;
  private SiteGetServlet servlet;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    adminSession = loginAdministrative();
  }

  
  public void testGoodSiteJSONManager() throws Exception {
    long time = System.currentTimeMillis();
    ACMEGroupStructure acme = createAcmeStructure("" + time);
    Node siteNode = createGoodSite(adminSession);

    // The managers will be maintainers of the site node.
    // The others will be viewers.
    List<Authorizable> siteManagers = new ArrayList<Authorizable>();
    siteManagers.add(acme.acmeManagers);
    setManagers(siteNode, siteManagers);
    
    // Retrieve the site node trough a Manager.
    Session session = login(acme.userTopManager);
    siteNode = session.getNode(siteNode.getPath());

    Resource resource = mock(Resource.class);
    when(resource.adaptTo(Node.class)).thenReturn(siteNode);
    when(request.getResource()).thenReturn(resource);

    setupJsonExtension();
    String s = makeGetRequestReturningString();
    JSONObject o = new JSONObject(s);

    verify(response).setContentType("application/json");
    verify(response).setCharacterEncoding("UTF-8");

    assertEquals(o.get(SiteGetServlet.SITE_IS_USER_MAINTAINER_PROPERTY), true);
  }

  
  public void testGoodSiteJSONNoManager() throws Exception {
    long time = System.currentTimeMillis();
    ACMEGroupStructure acme = createAcmeStructure("" + time);
    Node siteNode = createGoodSite(adminSession);

    // The managers will be maintainers of the site node.
    // The others will be viewers.
    List<Authorizable> siteManagers = new ArrayList<Authorizable>();
    siteManagers.add(acme.acmeManagers);
    setManagers(siteNode, siteManagers);

    // Retrieve the site node trough a Manager.
    Session session = login(acme.userDeveloper);
    siteNode = session.getNode(siteNode.getPath());

    Resource resource = mock(Resource.class);
    when(resource.adaptTo(Node.class)).thenReturn(siteNode);
    when(request.getResource()).thenReturn(resource);

    setupJsonExtension();
    String s = makeGetRequestReturningString();
    JSONObject o = new JSONObject(s);

    verify(response).setContentType("application/json");
    verify(response).setCharacterEncoding("UTF-8");

    assertEquals(o.get(SiteGetServlet.SITE_IS_USER_MAINTAINER_PROPERTY), false);
  }

  
  public void testRenderSiteAsHTML() throws Exception {
    long time = System.currentTimeMillis();
    ACMEGroupStructure acme = createAcmeStructure("" + time);
    Node siteNode = createGoodSite(adminSession);

    // Retrieve the site node trough a Manager.
    Session session = login(acme.userDeveloper);
    siteNode = session.getNode(siteNode.getPath());

    String skinPath = "/path/to/skin.html";
    siteNode.setProperty(SAKAI_SKIN, skinPath);

    Resource resource = mock(Resource.class);
    when(resource.adaptTo(Node.class)).thenReturn(siteNode);
    when(request.getResource()).thenReturn(resource);

    ResourceResolver resolver = mock(ResourceResolver.class);
    Resource skinResource = createSkinResource();
    when(resolver.getResource(skinPath)).thenReturn(skinResource);
    when(request.getResourceResolver()).thenReturn(resolver);

    setupNoExtension();
    makeRequest();

    verify(response).setContentType("text/html");
  }

  
  public void testRenderSiteAsDefaultHTML() throws Exception {
    long time = System.currentTimeMillis();
    ACMEGroupStructure acme = createAcmeStructure("" + time);
    Node siteNode = createGoodSite(adminSession);

    // Retrieve the site node trough a Manager.
    Session session = login(acme.userDeveloper);
    siteNode = session.getNode(siteNode.getPath());
    String skinPath = "/path/to/non/existing/skin.html";
    siteNode.setProperty(SAKAI_SKIN, skinPath);

    // Site node
    Resource resource = mock(Resource.class);
    when(resource.adaptTo(Node.class)).thenReturn(siteNode);
    when(request.getResource()).thenReturn(resource);

    // The skin on the site node doesn't exist.
    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.getResource(skinPath)).thenReturn(null);

    // Skin
    Resource skinResource = createSkinResource();
    when(resolver.getResource(SiteServiceImpl.DEFAULT_SITE)).thenReturn(skinResource);

    when(request.getResourceResolver()).thenReturn(resolver);

    setupNoExtension();
    makeRequest();

    verify(response).setContentType("text/html");
  }

  /**
   * @return
   */
  private Resource createSkinResource() {
    Resource skinResource = mock(Resource.class);
    // Mock streaming of skin.
    when(skinResource.adaptTo(InputStream.class)).thenReturn(new InputStream() {

      @Override
      public int read() throws IOException {
        return -1;
      }
    });
    return skinResource;
  }

  private void setupJsonExtension() {
    RequestPathInfo requestPathInfo = mock(RequestPathInfo.class);
    when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
    when(requestPathInfo.getExtension()).thenReturn("json");
  }

  private void setupNoExtension() {
    RequestPathInfo requestPathInfo = mock(RequestPathInfo.class);
    when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
    when(requestPathInfo.getExtension()).thenReturn("");
  }

  public void makeRequest() throws Exception {
    servlet = new SiteGetServlet();
    servlet.bindSiteService(siteService);
    servlet.doGet(request, response);
    servlet.unbindSiteService(siteService);

  }

}
