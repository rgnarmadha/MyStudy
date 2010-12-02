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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.api.site.SiteService.PARAM_COPY_FROM;
import static org.sakaiproject.nakamura.api.site.SiteService.PARAM_MOVE_FROM;
import static org.sakaiproject.nakamura.api.site.SiteService.PARAM_SITE_PATH;
import static org.sakaiproject.nakamura.api.site.SiteService.SAKAI_IS_SITE_TEMPLATE;
import static org.sakaiproject.nakamura.api.site.SiteService.SAKAI_SITE_TEMPLATE;
import static org.sakaiproject.nakamura.api.site.SiteService.SAKAI_SITE_TYPE;
import static org.sakaiproject.nakamura.api.site.SiteService.SITES_CONTAINER_RESOURCE_TYPE;
import static org.sakaiproject.nakamura.api.site.SiteService.SITE_RESOURCE_TYPE;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.site.SiteServiceImpl;

import java.security.Principal;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;

/**
 *
 */
@RunWith(value = MockitoJUnitRunner.class)
public class TestCreateSiteServlet {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SlingHttpServletRequest request;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SlingHttpServletResponse response;

  @Mock
  private SlingRepository slingRepository;
  @Mock
  private EventAdmin eventAdmin;
  @Mock
  private ResourceResolver resourceResolver;
  @Mock
  private Resource resource;
  @Mock
  private JackrabbitSession session;
  @Mock
  private Workspace workspace;
  @Mock
  private UserManager userManager;
  @Mock
  private PrincipalManager principalManager;
  @Mock
  private AccessControlManager accessControlManager;
  @Mock
  private Authorizable authorizable;
  @Mock
  private Principal principal;
  @Mock
  private Node rootNode;
  @Mock
  private Node node;
  @Mock
  private Node subNode;
  @Mock
  private Node templateNode;
  @Mock
  private Node siteNode;
  @Mock
  private Privilege allPrivilege;
  @Mock
  private JackrabbitAccessControlList acl;
  @Mock
  private JackrabbitAccessControlEntry ace;

  private CreateSiteServlet servlet;
  private SiteService siteService;

  private static final String USER = "aUser";

  @Before
  public void setUp() throws Exception {
    servlet = new CreateSiteServlet();
    siteService = new SiteServiceImpl();
    servlet.bindSiteService(siteService);
    servlet.slingRepository = slingRepository;
    servlet.eventAdmin = eventAdmin;

    when(slingRepository.loginAdministrative(anyString())).thenReturn(session);

    when(request.getResource()).thenReturn(resource);
    when(request.getResourceResolver()).thenReturn(resourceResolver);
    when(request.getRemoteUser()).thenReturn(USER);

    when(resourceResolver.adaptTo(Session.class)).thenReturn(session);

    when(session.getWorkspace()).thenReturn(workspace);
    when(session.getUserManager()).thenReturn(userManager);
    when(session.getPrincipalManager()).thenReturn(principalManager);
    when(session.getAccessControlManager()).thenReturn(accessControlManager);
    when(session.getUserID()).thenReturn(USER);
    when(session.getRootNode()).thenReturn(rootNode);

    when(node.addNode(isA(String.class))).thenReturn(subNode);

    when(userManager.getAuthorizable(isA(String.class))).thenReturn(authorizable);

    when(authorizable.getID()).thenReturn(USER);
    when(authorizable.getPrincipal()).thenReturn(principal);

    when(accessControlManager.privilegeFromName("jcr:all")).thenReturn(allPrivilege);

    when(allPrivilege.getName()).thenReturn("all");

    when(accessControlManager.getPolicies(isA(String.class))).thenReturn(
        new AccessControlPolicy[] { acl });

    when(acl.getAccessControlEntries()).thenReturn(new AccessControlEntry[] { ace });

    when(ace.getPrincipal()).thenReturn(principal);
    when(ace.isAllow()).thenReturn(true);
    when(ace.getPrivileges()).thenReturn(new Privilege[0]);
  }

  @After
  public void tearDown() throws Exception {
    servlet.unbindSiteService(siteService);
  }

  @Test
  public void noAnonAccess() throws Exception {
    reset(session);
    when(session.getUserID()).thenReturn(UserConstants.ANON_USERID);

    servlet.doPost(request, response);

    verify(response).sendError(SC_FORBIDDEN);
  }

  @Test
  public void unhandledResourceType() throws Exception {
    servlet.doPost(request, response);

    verifyZeroInteractions(response);
  }

  @Test
  public void nullOrEmptyRelativePath() throws Exception {
    when(resource.getResourceType()).thenReturn(SITES_CONTAINER_RESOURCE_TYPE);
    when(request.getRequestParameter(PARAM_SITE_PATH)).thenReturn(null);

    // test for null
    servlet.doPost(request, response);

    verify(response).sendError(eq(SC_BAD_REQUEST), isA(String.class));

    // test for empty
    reset(response);

    RequestParameter sitePath = mock(RequestParameter.class);
    when(sitePath.getString()).thenReturn("");
    when(request.getRequestParameter(SiteService.PARAM_SITE_PATH)).thenReturn(sitePath);
    servlet.doPost(request, response);

    verify(response).sendError(eq(SC_BAD_REQUEST), isA(String.class));
  }

  @Test
  public void createSiteWithoutTemplate() throws Exception {
    String sitePath = "/site/path";
    String somePath = "somePath";

    when(resource.getResourceType()).thenReturn(SITES_CONTAINER_RESOURCE_TYPE);
    when(request.getRequestPathInfo().getResourcePath()).thenReturn(sitePath);
    when(request.getRequestParameter(SAKAI_SITE_TEMPLATE)).thenReturn(null);
    when(request.getRequestParameter(PARAM_MOVE_FROM)).thenReturn(null);
    when(request.getRequestParameter(PARAM_COPY_FROM)).thenReturn(null);
    when(request.getRequestParameter(SAKAI_SITE_TYPE)).thenReturn(null);

    String path = sitePath + "/" + somePath;
    when(session.itemExists(path)).thenReturn(true);
    when(session.getItem(path)).thenReturn(node);
    when(node.getPath()).thenReturn(path);

    RequestParameter sitePathParam = mock(RequestParameter.class);
    when(sitePathParam.getString()).thenReturn(somePath);
    when(request.getRequestParameter(PARAM_SITE_PATH)).thenReturn(sitePathParam);

    when(session.hasPendingChanges()).thenReturn(true);

    servlet.doPost(request, response);

    verify(eventAdmin).postEvent(isA(Event.class));
  }

  @Test
  public void createSiteWithNonexistentTemplate() throws Exception {
    String templatePath = "/site/template";
    String sitePath = "/site/path";
    String somePath = "somePath";

    when(resource.getResourceType()).thenReturn(SITES_CONTAINER_RESOURCE_TYPE);
    when(request.getRequestPathInfo().getResourcePath()).thenReturn(sitePath);

    when(request.getRequestParameter(SAKAI_SITE_TEMPLATE).getString()).thenReturn(
        templatePath);
    when(session.itemExists(templatePath)).thenReturn(false);

    RequestParameter sitePathParam = mock(RequestParameter.class);
    when(sitePathParam.getString()).thenReturn(somePath);
    when(request.getRequestParameter(PARAM_SITE_PATH)).thenReturn(sitePathParam);
    servlet.doPost(request, response);

    verify(response).sendError(eq(SC_BAD_REQUEST), isA(String.class));
  }

  @Test
  public void createSiteWithNonTemplate() throws Exception {
    String templatePath = "/site/template";
    String sitePath = "/site/path";
    String somePath = "somePath";

    when(resource.getResourceType()).thenReturn(SITES_CONTAINER_RESOURCE_TYPE);
    when(request.getRequestPathInfo().getResourcePath()).thenReturn(sitePath);
    when(request.getRequestParameter(SAKAI_SITE_TEMPLATE).getString()).thenReturn(
        templatePath);
    when(session.itemExists(templatePath)).thenReturn(true);
    when(session.getItem(templatePath)).thenReturn(templateNode);

    when(templateNode.hasProperty(SAKAI_IS_SITE_TEMPLATE)).thenReturn(true);
    Property slingProp = mock(Property.class);
    when(slingProp.getBoolean()).thenReturn(false);
    when(templateNode.getProperty(SAKAI_IS_SITE_TEMPLATE)).thenReturn(slingProp);

    RequestParameter sitePathParam = mock(RequestParameter.class);
    when(sitePathParam.getString()).thenReturn(somePath);
    when(request.getRequestParameter(PARAM_SITE_PATH)).thenReturn(sitePathParam);

    servlet.doPost(request, response);

    verify(response).sendError(eq(SC_BAD_REQUEST), isA(String.class));
  }

  @Test
  public void createSiteWithTemplate() throws Exception {
    String templatePath = "/site/template";
    String sitePath = "/site/path";
    String somePath = "somePath";

    when(resource.getResourceType()).thenReturn(SITES_CONTAINER_RESOURCE_TYPE);
    when(request.getRequestPathInfo().getResourcePath()).thenReturn(sitePath);
    when(request.getRequestParameter(SAKAI_SITE_TEMPLATE).getString()).thenReturn(
        templatePath);
    when(session.itemExists(templatePath)).thenReturn(true);
    when(session.getItem(templatePath)).thenReturn(templateNode);
    when(request.getRequestParameter(PARAM_MOVE_FROM)).thenReturn(null);
    when(request.getRequestParameter(PARAM_COPY_FROM)).thenReturn(null);
    when(request.getRequestParameter(SAKAI_SITE_TYPE)).thenReturn(null);

    when(templateNode.hasProperty(SAKAI_IS_SITE_TEMPLATE)).thenReturn(true);
    Property slingProp = mock(Property.class);
    when(slingProp.getBoolean()).thenReturn(true);
    when(templateNode.getProperty(SAKAI_IS_SITE_TEMPLATE)).thenReturn(slingProp);
    
    String path = sitePath + "/" + somePath;
    when(session.itemExists(sitePath)).thenReturn(true);
    when(session.getItem(sitePath)).thenReturn(siteNode);
    when(session.itemExists(path)).thenReturn(true);
    when(session.getItem(path)).thenReturn(node);
    when(node.getPath()).thenReturn(path);

    RequestParameter sitePathParam = mock(RequestParameter.class);
    when(sitePathParam.getString()).thenReturn(somePath);
    when(request.getRequestParameter(PARAM_SITE_PATH)).thenReturn(sitePathParam);

    servlet.doPost(request, response);

    verify(eventAdmin).postEvent(isA(Event.class));
  }

  @Test
  public void copyNonexistentSite() throws Exception {
    String sitePath = "/site/path";
    String somePath = "somePath";
    String copySitePath = "/site/copy";

    when(resource.getResourceType()).thenReturn(SITES_CONTAINER_RESOURCE_TYPE);
    when(request.getRequestPathInfo().getResourcePath()).thenReturn(sitePath);
    when(request.getRequestParameter(SAKAI_SITE_TEMPLATE)).thenReturn(null);
    when(request.getRequestParameter(PARAM_MOVE_FROM)).thenReturn(null);
    when(request.getRequestParameter(PARAM_COPY_FROM).getString()).thenReturn(
        copySitePath);
    when(request.getRequestParameter(SAKAI_SITE_TYPE)).thenReturn(null);

    String path = sitePath + "/" + somePath;
    when(session.itemExists(path)).thenReturn(true);
    when(session.getItem(path)).thenReturn(node);
    when(node.getPath()).thenReturn(path);

    when(session.itemExists(copySitePath)).thenReturn(false);

    RequestParameter sitePathParam = mock(RequestParameter.class);
    when(sitePathParam.getString()).thenReturn(somePath);
    when(request.getRequestParameter(PARAM_SITE_PATH)).thenReturn(sitePathParam);

    servlet.doPost(request, response);

    verify(response).sendError(eq(SC_BAD_REQUEST), isA(String.class));
  }

  @Test
  public void copyNonSite() throws Exception {
    String sitePath = "/site/path";
    String somePath = "somePath";
    String copySitePath = "/site/copy";

    when(resource.getResourceType()).thenReturn(SITES_CONTAINER_RESOURCE_TYPE);
    when(request.getRequestPathInfo().getResourcePath()).thenReturn(sitePath);
    when(request.getRequestParameter(SAKAI_SITE_TEMPLATE)).thenReturn(null);
    when(request.getRequestParameter(PARAM_MOVE_FROM)).thenReturn(null);
    when(request.getRequestParameter(PARAM_COPY_FROM).getString()).thenReturn(
        copySitePath);
    when(request.getRequestParameter(SAKAI_SITE_TYPE)).thenReturn(null);

    String path = sitePath + "/" + somePath;
    when(session.itemExists(path)).thenReturn(true);
    when(session.getItem(path)).thenReturn(node);
    when(node.getPath()).thenReturn(path);

    Node copySite = mock(Node.class);
    when(session.itemExists(copySitePath)).thenReturn(true);
    when(session.getItem(copySitePath)).thenReturn(copySite);

    RequestParameter sitePathParam = mock(RequestParameter.class);
    when(sitePathParam.getString()).thenReturn(somePath);
    when(request.getRequestParameter(PARAM_SITE_PATH)).thenReturn(sitePathParam);

    servlet.doPost(request, response);

    verify(response).sendError(eq(SC_BAD_REQUEST), isA(String.class));
  }

  @Test
  public void copySite() throws Exception {
    String sitePath = "/site/path";
    String somePath = "somePath";
    String copySitePath = "/site/copy";

    when(resource.getResourceType()).thenReturn(SITES_CONTAINER_RESOURCE_TYPE);
    when(request.getRequestPathInfo().getResourcePath()).thenReturn(sitePath);
    when(request.getRequestParameter(SAKAI_SITE_TEMPLATE)).thenReturn(null);
    when(request.getRequestParameter(PARAM_MOVE_FROM)).thenReturn(null);
    when(request.getRequestParameter(PARAM_COPY_FROM).getString()).thenReturn(
        copySitePath);
    when(request.getRequestParameter(SAKAI_SITE_TYPE)).thenReturn(null);

    String path = sitePath + "/" + somePath;
    when(session.itemExists(sitePath)).thenReturn(true);
    when(session.getItem(sitePath)).thenReturn(siteNode);
    when(session.itemExists(path)).thenReturn(true);
    when(session.getItem(path)).thenReturn(node);
    when(node.getPath()).thenReturn(path);

    Node copySite = mock(Node.class);
    when(copySite.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)).thenReturn(true);
    Property siteProp = mock(Property.class);
    when(siteProp.getString()).thenReturn(SITE_RESOURCE_TYPE);
    when(copySite.getProperty(SLING_RESOURCE_TYPE_PROPERTY)).thenReturn(siteProp);
    when(session.itemExists(copySitePath)).thenReturn(true);
    when(session.getItem(copySitePath)).thenReturn(copySite);

    RequestParameter sitePathParam = mock(RequestParameter.class);
    when(sitePathParam.getString()).thenReturn(somePath);
    when(request.getRequestParameter(PARAM_SITE_PATH)).thenReturn(sitePathParam);

    when(session.hasPendingChanges()).thenReturn(true);

    servlet.doPost(request, response);

    verify(workspace).copy(copySitePath, path);
    verify(eventAdmin).postEvent(isA(Event.class));
  }

  @Test
  public void moveNonexistentSite() throws Exception {
    String sitePath = "/site/path";
    String somePath = "somePath";
    String copySitePath = "/site/move";

    when(resource.getResourceType()).thenReturn(SITES_CONTAINER_RESOURCE_TYPE);
    when(request.getRequestPathInfo().getResourcePath()).thenReturn(sitePath);
    when(request.getRequestParameter(SAKAI_SITE_TEMPLATE)).thenReturn(null);
    when(request.getRequestParameter(PARAM_COPY_FROM)).thenReturn(null);
    when(request.getRequestParameter(PARAM_MOVE_FROM).getString()).thenReturn(
        copySitePath);
    when(request.getRequestParameter(SAKAI_SITE_TYPE)).thenReturn(null);

    String path = sitePath + "/" + somePath;
    when(session.itemExists(path)).thenReturn(true);
    when(session.getItem(path)).thenReturn(node);
    when(node.getPath()).thenReturn(path);

    when(session.itemExists(copySitePath)).thenReturn(false);

    RequestParameter sitePathParam = mock(RequestParameter.class);
    when(sitePathParam.getString()).thenReturn(somePath);
    when(request.getRequestParameter(PARAM_SITE_PATH)).thenReturn(sitePathParam);

    servlet.doPost(request, response);

    verify(response).sendError(eq(SC_BAD_REQUEST), isA(String.class));
  }

  @Test
  public void moveNonSite() throws Exception {
    String sitePath = "/site/path";
    String somePath = "somePath";
    String copySitePath = "/site/move";

    when(resource.getResourceType()).thenReturn(SITES_CONTAINER_RESOURCE_TYPE);
    when(request.getRequestPathInfo().getResourcePath()).thenReturn(sitePath);
    when(request.getRequestParameter(SAKAI_SITE_TEMPLATE)).thenReturn(null);
    when(request.getRequestParameter(PARAM_COPY_FROM)).thenReturn(null);
    when(request.getRequestParameter(PARAM_MOVE_FROM).getString()).thenReturn(
        copySitePath);
    when(request.getRequestParameter(SAKAI_SITE_TYPE)).thenReturn(null);

    String path = sitePath + "/" + somePath;
    when(session.itemExists(path)).thenReturn(true);
    when(session.getItem(path)).thenReturn(node);
    when(node.getPath()).thenReturn(path);

    Node copySite = mock(Node.class);
    when(session.itemExists(copySitePath)).thenReturn(true);
    when(session.getItem(copySitePath)).thenReturn(copySite);

    RequestParameter sitePathParam = mock(RequestParameter.class);
    when(sitePathParam.getString()).thenReturn(somePath);
    when(request.getRequestParameter(PARAM_SITE_PATH)).thenReturn(sitePathParam);

    servlet.doPost(request, response);

    verify(response).sendError(eq(SC_BAD_REQUEST), isA(String.class));
  }

  @Test
  public void moveSite() throws Exception {
    String sitePath = "/site/path";
    String somePath = "somePath";
    String moveSitePath = "/site/move";

    when(resource.getResourceType()).thenReturn(SITES_CONTAINER_RESOURCE_TYPE);
    when(request.getRequestPathInfo().getResourcePath()).thenReturn(sitePath);
    when(request.getRequestParameter(SAKAI_SITE_TEMPLATE)).thenReturn(null);
    when(request.getRequestParameter(PARAM_MOVE_FROM).getString()).thenReturn(
        moveSitePath);
    when(request.getRequestParameter(PARAM_COPY_FROM)).thenReturn(null);
    when(request.getRequestParameter(SAKAI_SITE_TYPE)).thenReturn(null);

    String path = sitePath + "/" + somePath;
    when(session.itemExists(sitePath)).thenReturn(true);
    when(session.getItem(sitePath)).thenReturn(siteNode);
    when(session.itemExists(path)).thenReturn(true);
    when(session.getItem(path)).thenReturn(node);
    when(node.getPath()).thenReturn(path);

    Node copySite = mock(Node.class);
    when(copySite.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)).thenReturn(true);
    Property siteProp = mock(Property.class);
    when(siteProp.getString()).thenReturn(SITE_RESOURCE_TYPE);
    when(copySite.getProperty(SLING_RESOURCE_TYPE_PROPERTY)).thenReturn(siteProp);
    
    when(session.itemExists(moveSitePath)).thenReturn(true);
    when(session.getItem(moveSitePath)).thenReturn(copySite);


    RequestParameter sitePathParam = mock(RequestParameter.class);
    when(sitePathParam.getString()).thenReturn(somePath);
    when(request.getRequestParameter(PARAM_SITE_PATH)).thenReturn(sitePathParam);

    when(session.hasPendingChanges()).thenReturn(true);

    servlet.doPost(request, response);

    verify(workspace).move(moveSitePath, path);
    verify(eventAdmin).postEvent(isA(Event.class));
  }
}
