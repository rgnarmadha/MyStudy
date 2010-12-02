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
package org.sakaiproject.nakamura.authz.servlets;

import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.ISO8601Date;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.Principal;
import java.util.Vector;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import javax.servlet.ServletException;

/**
 *
 */
public class CreateRuleAclServletTest {

  
  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private SlingHttpServletResponse httpResponse;
  @Mock
  private Resource resource;
  @Mock
  private ResourceResolver resourceResolver;
  @Mock
  private SessionImpl session;
  @Mock
  private PrincipalManager principalManager;
  @Mock
  private Principal principal;
  @Mock
  private NodeImpl node;
  @Mock
  private ValueFactory valueFactory;
  @Mock
  private AccessControlManager accessControlManager;
  @Mock
  private Privilege privilege;
  @Mock
  private JackrabbitAccessControlList acl1;
 
  
  private CreateRuleAclServlet servlet;
  private StringWriter stringWriter;
 
  public CreateRuleAclServletTest() {
    MockitoAnnotations.initMocks(this);
  }
 
  @Before
  public void setup() throws IOException, RepositoryException {
    // basic setup
    servlet = new CreateRuleAclServlet();
    Mockito.when(request.getResource()).thenReturn(resource);
    Mockito.when(resource.getPath()).thenReturn("/mytestace-path");
    Mockito.when(request.getResourceResolver()).thenReturn(resourceResolver);
    Mockito.when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
    Mockito.when(session.getPrincipalManager()).thenReturn(principalManager);
    Mockito.when(session.getAccessControlManager()).thenReturn(accessControlManager);
    Mockito.when(accessControlManager.privilegeFromName(Mockito.anyString())).thenReturn(privilege);
    Mockito.when(accessControlManager.getPolicies("/mytestace-path")).thenReturn(new AccessControlPolicy[]{acl1});
    Mockito.when(acl1.getAccessControlEntries()).thenReturn(new AccessControlEntry[0]);
    Mockito.when(session.getValueFactory()).thenReturn(valueFactory);
    stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    Mockito.when(httpResponse.getWriter()).thenReturn(printWriter);
    
  }
 
  @Test
  public void testCreateACE() throws ServletException, IOException, RepositoryException {
    
    Mockito.when(request.getParameter("principalId")).thenReturn("ieb");
    Mockito.when(principalManager.getPrincipal("ieb")).thenReturn(principal);
    Mockito.when(principal.getName()).thenReturn("ieb");
    Mockito.when(resource.adaptTo(Item.class)).thenReturn(node);
    Mockito.when(session.getItem("/mytestace-path")).thenReturn(node);
    Mockito.when(node.getPath()).thenReturn("/mytestace-path");
    Vector<String> parameterNames = new Vector<String>();
    parameterNames.add("principalId");
    parameterNames.add("privilege@jcr:read");
    parameterNames.add("privilege@jcr:write");
    parameterNames.add("privilege@jcr:all");
    
    Mockito.when(request.getParameter(SlingPostConstants.RP_REDIRECT_TO)).thenReturn("somwhereelse*");
    
    Mockito.when(request.getParameterNames()).thenReturn(parameterNames.elements());
    Mockito.when(request.getParameter("privilege@jcr:read")).thenReturn("granted");
    Mockito.when(request.getParameter("privilege@jcr:write")).thenReturn("denied");
    Mockito.when(request.getParameter("privilege@jcr:all")).thenReturn("none");
    
    Mockito.when(request.getParameter("order")).thenReturn("2");
    ISO8601Date d1 = new ISO8601Date();
    d1.setTimeInMillis(System.currentTimeMillis());
    ISO8601Date d2 = new ISO8601Date();
    d2.setTimeInMillis(System.currentTimeMillis()+3600*2000);
    ISO8601Date d3 = new ISO8601Date();
    d3.setTimeInMillis(System.currentTimeMillis()+600*1000);
    ISO8601Date d4 = new ISO8601Date();
    d4.setTimeInMillis(System.currentTimeMillis()+600*2000);
    String[] timeActive = new String[] {
      d1.toString()+"/"+d2.toString(),
      d4.toString()+"/"+d2.toString()
    };
    String[] timeInactive = new String[] {
        d3.toString()+"/"+d4.toString(),    
    };
    Mockito.when(request.getParameterValues("active")).thenReturn(timeActive);
    Mockito.when(request.getParameterValues("inactive")).thenReturn(timeInactive);
    
    Value value = Mockito.mock(Value.class);
    Mockito.when(valueFactory.createValue((String)Mockito.anyObject())).thenReturn(value);
    
    
    servlet.doPost(request, httpResponse);
    
  }
}
