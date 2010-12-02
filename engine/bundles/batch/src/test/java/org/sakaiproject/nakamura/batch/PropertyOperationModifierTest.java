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
package org.sakaiproject.nakamura.batch;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
public class PropertyOperationModifierTest {

  @Test
  public void testAdd() throws PathNotFoundException, RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    ResourceResolver resolver = mock(ResourceResolver.class);
    Session session = mock(Session.class);

    // Session
    when(request.getResourceResolver()).thenReturn(resolver);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    // Request parameters
    String[] applyTo = new String[] { "/path/to/alfa", "/path/to/beta" };
    Map<String, String[]> parameterMap = new HashMap<String, String[]>();
    parameterMap.put(":operation", new String[] { "addProperty" });
    parameterMap.put("sling:resourceType", new String[] { "sakai/widget-settings" });
    parameterMap.put("sakai:widget-type", new String[] { "poll" });
    parameterMap.put(SlingPostConstants.RP_APPLY_TO, applyTo);
    RequestParameter operationParam = mock(RequestParameter.class);
    when(operationParam.getString()).thenReturn("addProperty");
    when(request.getParameter(":operation")).thenReturn("addProperty");
    when(request.getRequestParameter(":operation")).thenReturn(operationParam);
    when(request.getParameterValues(SlingPostConstants.RP_APPLY_TO)).thenReturn(applyTo);
    when(request.getParameterMap()).thenReturn(parameterMap);

    // Getting the nodes
    MockNode alfaNode = new MockNode("/path/to/alfa");
    alfaNode.setProperty("sakai:widget-type", "discussion");
    MockNode betaNode = new MockNode("/path/to/beta");
    when(session.getItem(alfaNode.getPath())).thenReturn(alfaNode);
    when(session.getItem(betaNode.getPath())).thenReturn(betaNode);

    // Saving
    when(session.hasPendingChanges()).thenReturn(true);

    // Changes
    List<Modification> changes = new ArrayList<Modification>();

    AddPropertyOperation operation = new AddPropertyOperation();
    operation.doRun(request, null, changes);

    assertEquals(2, alfaNode.getProperty("sakai:widget-type").getValues().length);
    verify(session).save();
  }

  @Test
  public void testSingle() throws RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    ResourceResolver resolver = mock(ResourceResolver.class);
    Session session = mock(Session.class);

    // Session
    when(request.getResourceResolver()).thenReturn(resolver);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    // Request parameters
    Map<String, String[]> parameterMap = new HashMap<String, String[]>();
    parameterMap.put(":operation", new String[] { "addProperty" });
    parameterMap.put("sling:resourceType", new String[] { "sakai/widget-settings" });
    parameterMap.put("sakai:widget-type", new String[] { "poll" });
    RequestParameter operationParam = mock(RequestParameter.class);
    when(operationParam.getString()).thenReturn("addProperty");
    when(request.getParameter(":operation")).thenReturn("addProperty");
    when(request.getRequestParameter(":operation")).thenReturn(operationParam);
    when(request.getParameterValues(SlingPostConstants.RP_APPLY_TO)).thenReturn(null);
    when(request.getParameterMap()).thenReturn(parameterMap);

    // Getting the node
    MockNode alfaNode = new MockNode("/path/to/alfa");
    alfaNode.setProperty("sakai:widget-type", "discussion");
    Resource resource = mock(Resource.class);
    when(resource.adaptTo(Item.class)).thenReturn(alfaNode);
    when(request.getResource()).thenReturn(resource);
    when(session.getItem(alfaNode.getPath())).thenReturn(alfaNode);
    when(session.itemExists(alfaNode.getPath())).thenReturn(true);

    // Saving
    when(session.hasPendingChanges()).thenReturn(true);

    // Changes
    List<Modification> changes = new ArrayList<Modification>();

    AddPropertyOperation operation = new AddPropertyOperation();
    operation.doRun(request, null, changes);

    assertEquals(2, alfaNode.getProperty("sakai:widget-type").getValues().length);
    verify(session).save();
  }

  @Test
  public void testRemove() throws PathNotFoundException, RepositoryException {
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    ResourceResolver resolver = mock(ResourceResolver.class);
    Session session = mock(Session.class);

    // Session
    when(request.getResourceResolver()).thenReturn(resolver);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    // Request parameters
    String[] applyTo = new String[] { "/path/to/alfa", "/path/to/beta" };
    Map<String, String[]> parameterMap = new HashMap<String, String[]>();
    parameterMap.put(":operation", new String[] { "removeProperty" });
    parameterMap.put("sling:resourceType", new String[] { "sakai/widget-settings" });
    parameterMap.put("sakai:widget-type", new String[] { "poll" });
    parameterMap.put(SlingPostConstants.RP_APPLY_TO, applyTo);
    RequestParameter operationParam = mock(RequestParameter.class);
    when(operationParam.getString()).thenReturn("removeProperty");
    when(request.getParameter(":operation")).thenReturn("removeProperty");
    when(request.getRequestParameter(":operation")).thenReturn(operationParam);
    when(request.getParameterValues(SlingPostConstants.RP_APPLY_TO)).thenReturn(applyTo);
    when(request.getParameterMap()).thenReturn(parameterMap);

    // Getting the nodes
    MockNode alfaNode = new MockNode("/path/to/alfa");
    alfaNode.setProperty("sakai:widget-type", "poll");
    MockNode betaNode = new MockNode("/path/to/beta");
    when(session.getItem(alfaNode.getPath())).thenReturn(alfaNode);
    when(session.getItem(betaNode.getPath())).thenReturn(betaNode);

    // Saving
    when(session.hasPendingChanges()).thenReturn(true);

    // Changes
    List<Modification> changes = new ArrayList<Modification>();

    RemovePropertyOperation operation = new RemovePropertyOperation();
    operation.doRun(request, null, changes);

    assertEquals(0, alfaNode.getProperty("sakai:widget-type").getValues().length);
    verify(session).save();
  }
}
