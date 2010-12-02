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

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import junit.framework.Assert;

import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.junit.Test;
import org.sakaiproject.nakamura.api.site.SiteException;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.site.SiteServiceImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public class TestGetServlet extends AbstractSiteNodeTest {

  @Test
  public void testRenderSite() throws RepositoryException, IOException,
      ServletException {
    goodSiteNodeSetup();
    expect(node.hasProperty(eq(SiteService.SAKAI_SKIN))).andReturn(false);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    Resource resource = createMock(Resource.class);
    expect(resourceResolver.getResource(eq(SiteServiceImpl.DEFAULT_SITE)))
        .andReturn(resource);
    response.setContentType(eq("text/html"));
    expect(response.getOutputStream()).andReturn(null);
    expect(resource.adaptTo(eq(InputStream.class))).andReturn(
        new InputStream() {
          @Override
          public int read() throws IOException {
            return -1;
          }
        });
    response.setStatus(eq(HttpServletResponse.SC_OK));
    makeRequest();
  }

  private void setupEmptyExtension() {
    RequestPathInfo requestPathInfo = createMock(RequestPathInfo.class);
    expect(request.getRequestPathInfo()).andReturn(requestPathInfo).anyTimes();
    expect(requestPathInfo.getExtension()).andReturn(null).anyTimes();
  }

  private void setupJsonExtension() {
    RequestPathInfo requestPathInfo = createMock(RequestPathInfo.class);
    expect(request.getRequestPathInfo()).andReturn(requestPathInfo).anyTimes();
    expect(requestPathInfo.getExtension()).andReturn("json").anyTimes();
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
  }

  @Test
  public void testSiteException() throws RepositoryException, IOException,
      ServletException, SiteException {
    goodSiteNodeSetup();
    SiteService siteService = createMock(SiteService.class);
    expect(siteService.isSite(isA(Item.class))).andReturn(true);
    expect(siteService.getSiteSkin(isA(Node.class))).andThrow(
        new SiteException(1, "Doom"));
    response.sendError(eq(1), isA(String.class));

    setupEmptyExtension();
    preRequest();
    SiteGetServlet servlet = new SiteGetServlet();
    servlet.bindSiteService(siteService);
    servlet.doGet(request, response);
    servlet.unbindSiteService(siteService);
    postRequest();
  }

  //@Test
  public void testSiteAsJSON() throws RepositoryException, IOException,
      JSONException, ServletException {
    goodSiteNodeSetup();
    SiteService siteService = createMock(SiteService.class);
    expect(siteService.isSite(isA(Item.class))).andReturn(true);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(baos);
    expect(response.getWriter()).andReturn(writer);
    setupJsonExtension();

    Property propSlingResourceType = createMock(Property.class);
    PropertyDefinition propSlingResourceTypeDef = createMock(PropertyDefinition.class);
    Value propSlingResourceTypeValue = createMock(Value.class);
    expect(propSlingResourceTypeValue.getType()).andReturn(PropertyType.STRING);
    expect(propSlingResourceTypeValue.getString()).andReturn("sakai/site");
    expect(propSlingResourceTypeDef.isMultiple()).andReturn(false);
    expect(propSlingResourceType.getName()).andReturn(
        JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY);
    expect(propSlingResourceType.getType()).andReturn(PropertyType.STRING);
    expect(propSlingResourceType.getDefinition()).andReturn(
        propSlingResourceTypeDef);
    expect(propSlingResourceType.getValue()).andReturn(
        propSlingResourceTypeValue);

    PropertyIterator propIterator = createMock(PropertyIterator.class);
    expect(propIterator.hasNext()).andReturn(true);
    expect(propIterator.nextProperty()).andReturn(propSlingResourceType);
    expect(propIterator.hasNext()).andReturn(false);
    expect(node.getProperties()).andReturn(propIterator);
    expect(node.hasProperty(isA(String.class))).andReturn(false);
    response.setStatus(HttpServletResponse.SC_OK);
    preRequest();
    SiteGetServlet servlet = new SiteGetServlet();
    servlet.bindSiteService(siteService);
    servlet.doGet(request, response);
    servlet.unbindSiteService(siteService);
    writer.flush();
    String s = baos.toString("UTF-8");
    JSONObject obj = new JSONObject(s);
    Assert.assertEquals("sakai/site", obj
        .get(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY));

  }

  protected void makeRequest() throws ServletException, IOException {
    setupEmptyExtension();
    preRequest();
    SiteGetServlet servlet = new SiteGetServlet();
    servlet.bindSiteService(siteService);
    servlet.doGet(request, response);
    servlet.unbindSiteService(siteService);
    postRequest();
  }

}
