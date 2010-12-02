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
package org.sakaiproject.nakamura.activity;

import static org.easymock.EasyMock.expect;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.PARAM_APPLICATION_ID;
import static org.sakaiproject.nakamura.api.activity.ActivityConstants.PARAM_TEMPLATE_ID;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.junit.Assert;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.IOException;

import javax.jcr.Node;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class ActivityCreateServletTest extends AbstractEasyMockTest {
  @Test
  public void testDummyTest() {
  }

  @Test
  public void testRequestPathInfo() {
    ActivityCreateServlet servlet = new ActivityCreateServlet();
    final String path = "/foo/bar/activity/2010/01/21/10/111223235453";

    final RequestPathInfo requestPathInfo = new RequestPathInfo() {

      public String getSuffix() {
        return "suffix";
      }

      public String[] getSelectors() {
        String[] selectors = new String[] { "foo", "activity", "bar" };
        return selectors;
      }

      public String getSelectorString() {
        return ".foo.activity.bar";
      }

      public String getResourcePath() {
        return path;
      }

      public String getExtension() {
        return "html";
      }
    };

    RequestPathInfo pathInfo = servlet.createRequestPathInfo(requestPathInfo,
        path);
    Assert.assertEquals(pathInfo.getSelectorString(), ".foo.bar");
    Assert.assertEquals(pathInfo.getExtension(), "html");
    Assert.assertEquals(pathInfo.getResourcePath(), requestPathInfo
        .getResourcePath());
    Assert.assertEquals(pathInfo.getSuffix(), "suffix");
    Assert.assertEquals(pathInfo.getSelectors()[0], "foo");
    Assert.assertEquals(pathInfo.getSelectors()[1], "bar");
  }

  @Test
  public void testParamaterApplication() throws ServletException, IOException {

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    expect(request.getRequestParameter(PARAM_APPLICATION_ID)).andReturn(null);

    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
        "The applicationId parameter must not be null");

    replay();
    ActivityCreateServlet servlet = new ActivityCreateServlet();
    servlet.doPost(request, response);
  }

  @Test
  public void testParamaterTemplate() throws ServletException, IOException {

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    addStringRequestParameter(request, PARAM_APPLICATION_ID, "foo");
    expect(request.getRequestParameter(PARAM_TEMPLATE_ID)).andReturn(null);

    response.sendError(HttpServletResponse.SC_BAD_REQUEST,
        "The templateId parameter must not be null");

    replay();
    ActivityCreateServlet servlet = new ActivityCreateServlet();
    servlet.doPost(request, response);
  }

  @Test
  public void testAnonUser() throws IOException, ServletException {

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    addStringRequestParameter(request, PARAM_APPLICATION_ID, "foo");
    addStringRequestParameter(request, PARAM_TEMPLATE_ID, "foo");
    expect(request.getRemoteUser()).andReturn(null);

    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
        "CurrentUser could not be determined, user must be identifiable");

    replay();
    ActivityCreateServlet servlet = new ActivityCreateServlet();
    servlet.doPost(request, response);
  }

  @Test
  public void testNullResource() throws ServletException, IOException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    addStringRequestParameter(request, PARAM_APPLICATION_ID, "foo");
    addStringRequestParameter(request, PARAM_TEMPLATE_ID, "bar");
    Resource resource = createMock(Resource.class);
    expect(resource.adaptTo(Node.class)).andReturn(null);
    expect(request.getResource()).andReturn(resource);
    expect(request.getRemoteUser()).andReturn("jack");

    response.sendError(HttpServletResponse.SC_NOT_FOUND);

    replay();
    ActivityCreateServlet servlet = new ActivityCreateServlet();
    servlet.doPost(request, response);
  }

}
