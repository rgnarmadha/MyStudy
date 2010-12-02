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
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.api.site.SiteService.AUTHORIZABLE;
import static org.sakaiproject.nakamura.api.site.SiteService.PARAM_ADD_GROUP;
import static org.sakaiproject.nakamura.api.site.SiteService.PARAM_REMOVE_GROUP;
import static org.sakaiproject.nakamura.api.site.SiteService.SITES;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.site.SiteService;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 *
 */
@RunWith(value = MockitoJUnitRunner.class)
public class TestSiteAuthorizeServlet {
  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private SlingHttpServletRequest request;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private SlingHttpServletResponse response;

  @Mock
  private Resource resource;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Node site;

  @Mock
  private JackrabbitSession jackRabbitSession;

  @Mock
  private UserManager userManager;

  @Mock
  private SiteService siteService;

  @Mock
  private Authorizable authorizable;

  @Mock
  ValueFactory valueFactory;

  private SiteAuthorizeServlet servlet;

  private static final String SITE_ID = "dead-beef-cafe";
  private static final String SITE_PATH = "/some/test/path";

  @Before
  public void setUp() throws Exception {
    servlet = new SiteAuthorizeServlet();
    servlet.bindSiteService(siteService);

    when(request.getResource()).thenReturn(resource);
    when(resource.adaptTo(Node.class)).thenReturn(site);
    when(site.getSession()).thenReturn(jackRabbitSession);
    when(site.getPath()).thenReturn(SITE_PATH);
    when(site.getIdentifier()).thenReturn(SITE_ID);

    when(authorizable.getID()).thenReturn("someUser");

    when(jackRabbitSession.getUserManager()).thenReturn(userManager);
    when(jackRabbitSession.getValueFactory()).thenReturn(valueFactory);
  }

  @After
  public void teardown() {
    servlet.unbindSiteService(siteService);
  }

  @Test
  public void repositoryException() throws Exception {
    reset(resource);

    Node site = mock(Node.class);
    when(resource.adaptTo(Node.class)).thenReturn(site);
    when(site.getSession()).thenThrow(new RepositoryException());

    servlet.doPost(request, response);

    verify(response).sendError(eq(SC_INTERNAL_SERVER_ERROR), isA(String.class));
  }

  @Test
  public void nodeIsNull() throws Exception {
    servlet = new SiteAuthorizeServlet();
    servlet.bindSiteService(siteService);

    when(request.getResource()).thenReturn(resource);
    when(resource.adaptTo(Node.class)).thenReturn(null);
    servlet.doPost(request, response);

    verify(response).sendError(eq(SC_NOT_FOUND), isA(String.class));
  }

  @Test
  public void nodeIsNotASite() throws Exception {
    servlet.doPost(request, response);

    verify(response).sendError(eq(SC_BAD_REQUEST), isA(String.class));
  }

  @Test
  public void noGroupsSpecified() throws Exception {
    when(siteService.isSite(site)).thenReturn(true);

    servlet.doPost(request, response);

    verify(response).sendError(eq(SC_BAD_REQUEST), isA(String.class));

    reset(response);

    when(request.getParameterValues(PARAM_ADD_GROUP)).thenReturn(new String[0]);
    when(request.getParameterValues(PARAM_REMOVE_GROUP)).thenReturn(new String[0]);
    servlet.doPost(request, response);

    verify(response).sendError(eq(SC_BAD_REQUEST), isA(String.class));
  }

  @Test
  public void missingAuthorizableAddNonexistentGroup() throws Exception {
    when(siteService.isSite(site)).thenReturn(true);
    String[] addGroups = new String[] { "myGroup" };
    when(request.getParameterValues(PARAM_ADD_GROUP)).thenReturn(addGroups);

    servlet.doPost(request, response);

    verify(response).sendError(eq(SC_BAD_REQUEST), isA(String.class));
  }

  @Test
  public void hasAuthorizableWithoutSitesAddGroup() throws Exception {
    when(siteService.isSite(site)).thenReturn(true);

    String[] addGroups = new String[] { "myGroup" };
    when(request.getParameterValues(PARAM_ADD_GROUP)).thenReturn(addGroups);

    when(userManager.getAuthorizable(isA(String.class))).thenReturn(authorizable);

    when(jackRabbitSession.hasPendingChanges()).thenReturn(true);

    servlet.doPost(request, response);

    ArgumentCaptor<String[]> siteAuthzAnswer = ArgumentCaptor.forClass(String[].class);
    ArgumentCaptor<Value[]> authzSitesAnswer = ArgumentCaptor.forClass(Value[].class);

    verify(site, never()).getProperty(AUTHORIZABLE);
    verify(site, atLeastOnce()).setProperty(eq(AUTHORIZABLE), siteAuthzAnswer.capture());
    verify(authorizable, never()).getProperty(SITES);
    verify(authorizable, atLeastOnce())
        .setProperty(eq(SITES), authzSitesAnswer.capture());
    verify(valueFactory, atLeastOnce()).createValue(isA(String.class));

    verify(jackRabbitSession).save();


    assertEquals(1, authzSitesAnswer.getValue().length);
    assertEquals(1, siteAuthzAnswer.getValue().length);
  }

  @Test
  public void hasAuthorizableWithSitesAddGroup() throws Exception {
    when(siteService.isSite(site)).thenReturn(true);

    String[] addGroups = new String[] { "myGroup" };
    when(request.getParameterValues(PARAM_ADD_GROUP)).thenReturn(addGroups);

    when(userManager.getAuthorizable(isA(String.class))).thenReturn(authorizable);

    Value[] siteVals = new Value[1];
    Value siteVal = mock(Value.class);
    when(siteVal.getString()).thenReturn(SITE_ID + "xx");
    siteVals[0] = siteVal;
    when(authorizable.hasProperty(SITES)).thenReturn(true);
    when(authorizable.getProperty(SITES)).thenReturn(siteVals);

    when(jackRabbitSession.hasPendingChanges()).thenReturn(true);

    servlet.doPost(request, response);

    ArgumentCaptor<String[]> siteAuthzAnswer = ArgumentCaptor.forClass(String[].class);
    ArgumentCaptor<Value[]> authzSitesAnswer = ArgumentCaptor.forClass(Value[].class);

    verify(site, never()).getProperty(AUTHORIZABLE);
    verify(site, atLeastOnce()).setProperty(eq(AUTHORIZABLE), siteAuthzAnswer.capture());
    verify(authorizable, atLeastOnce()).getProperty(SITES);
    verify(authorizable, atLeastOnce())
        .setProperty(eq(SITES), authzSitesAnswer.capture());
    verify(valueFactory, atLeastOnce()).createValue(isA(String.class));

    verify(jackRabbitSession).save();

    assertEquals(2, authzSitesAnswer.getValue().length);
    assertEquals(1, siteAuthzAnswer.getValue().length);
  }

  @Test
  public void hasAuthorizableWithSitesAddExistingGroup() throws Exception {
    when(siteService.isSite(site)).thenReturn(true);

    Value[] authzVals = new Value[1];
    Value authzVal = mock(Value.class);
    when(authzVal.getString()).thenReturn(SITE_ID);
    authzVals[0] = authzVal;

    when(site.hasProperty(AUTHORIZABLE)).thenReturn(true);
    when(site.getProperty(AUTHORIZABLE).getValues()).thenReturn(authzVals);

    String[] addGroups = new String[] { SITE_ID };
    when(request.getParameterValues(PARAM_ADD_GROUP)).thenReturn(addGroups);

    when(userManager.getAuthorizable(isA(String.class))).thenReturn(authorizable);

    Value[] siteVals = new Value[1];
    Value siteVal = mock(Value.class);
    when(siteVal.getString()).thenReturn(SITE_ID);
    siteVals[0] = siteVal;
    when(authorizable.hasProperty(SITES)).thenReturn(true);
    when(authorizable.getProperty(SITES)).thenReturn(siteVals);

    when(jackRabbitSession.hasPendingChanges()).thenReturn(true);

    servlet.doPost(request, response);

    verify(valueFactory, never()).createValue(isA(String.class));
    verify(authorizable, never()).setProperty(eq(SITES), isA(Value[].class));
    verify(jackRabbitSession).save();
  }

  @Test
  public void hasAuthorizableRemoveGroup() throws Exception {
    when(siteService.isSite(site)).thenReturn(true);

    Value[] authzVals = new Value[2];
    Value authzVal = mock(Value.class);
    when(authzVal.getString()).thenReturn(SITE_ID);
    authzVals[0] = authzVal;
    authzVal = mock(Value.class);
    when(authzVal.getString()).thenReturn("someGroup");
    authzVals[1] = authzVal;

    when(site.hasProperty(AUTHORIZABLE)).thenReturn(true);
    when(site.getProperty(AUTHORIZABLE).getValues()).thenReturn(authzVals);

    String[] removeGroups = new String[] { SITE_ID };
    when(request.getParameterValues(PARAM_REMOVE_GROUP)).thenReturn(removeGroups);

    when(userManager.getAuthorizable(isA(String.class))).thenReturn(authorizable);

    Value[] siteVals = new Value[2];
    Value siteVal = mock(Value.class);
    when(siteVal.getString()).thenReturn(SITE_ID);
    siteVals[0] = siteVal;
    siteVal = mock(Value.class);
    when(siteVal.getString()).thenReturn("someGroup");
    siteVals[1] = siteVal;
    when(authorizable.hasProperty(SITES)).thenReturn(true);
    when(authorizable.getProperty(SITES)).thenReturn(siteVals);

    when(jackRabbitSession.hasPendingChanges()).thenReturn(true);

    servlet.doPost(request, response);

    ArgumentCaptor<String[]> siteAuthzAnswer = ArgumentCaptor.forClass(String[].class);
    ArgumentCaptor<Value[]> authzSitesAnswer = ArgumentCaptor.forClass(Value[].class);

    verify(site, atLeastOnce()).getProperty(AUTHORIZABLE);
    verify(site, atLeastOnce()).setProperty(eq(AUTHORIZABLE), siteAuthzAnswer.capture());
    verify(authorizable, atLeastOnce()).getProperty(SITES);
    verify(authorizable, atLeastOnce())
        .setProperty(eq(SITES), authzSitesAnswer.capture());

    verify(jackRabbitSession).save();

    assertEquals(1, siteAuthzAnswer.getValue().length);
    assertEquals(1, authzSitesAnswer.getValue().length);
  }

  @Test
  public void hasAuthorizableRemoveOnlyGroup() throws Exception {
    when(siteService.isSite(site)).thenReturn(true);

    Value[] authzVals = new Value[1];
    Value authzVal = mock(Value.class);
    when(authzVal.getString()).thenReturn(SITE_ID);
    authzVals[0] = authzVal;

    when(site.hasProperty(AUTHORIZABLE)).thenReturn(true);
    when(site.getProperty(AUTHORIZABLE).getValues()).thenReturn(authzVals);

    String[] removeGroups = new String[] { SITE_ID };
    when(request.getParameterValues(PARAM_REMOVE_GROUP)).thenReturn(removeGroups);

    when(userManager.getAuthorizable(isA(String.class))).thenReturn(authorizable);

    Value[] siteVals = new Value[1];
    Value siteVal = mock(Value.class);
    when(siteVal.getString()).thenReturn(SITE_ID);
    siteVals[0] = siteVal;
    when(authorizable.hasProperty(SITES)).thenReturn(true);
    when(authorizable.getProperty(SITES)).thenReturn(siteVals);

    when(jackRabbitSession.hasPendingChanges()).thenReturn(true);

    servlet.doPost(request, response);

    ArgumentCaptor<String[]> siteAuthzAnswer = ArgumentCaptor.forClass(String[].class);
    ArgumentCaptor<Value[]> authzSitesAnswer = ArgumentCaptor.forClass(Value[].class);

    verify(site, atLeastOnce()).getProperty(AUTHORIZABLE);
    verify(site, atLeastOnce()).setProperty(eq(AUTHORIZABLE), siteAuthzAnswer.capture());
    verify(authorizable, atLeastOnce()).getProperty(SITES);
    verify(authorizable, atLeastOnce())
        .setProperty(eq(SITES), authzSitesAnswer.capture());

    verify(jackRabbitSession).save();

    assertEquals(0, siteAuthzAnswer.getValue().length);
    assertEquals(0, authzSitesAnswer.getValue().length);
  }
}
