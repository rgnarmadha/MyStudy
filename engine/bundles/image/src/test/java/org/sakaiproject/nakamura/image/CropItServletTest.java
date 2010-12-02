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
package org.sakaiproject.nakamura.image;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sanselan.util.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.easymock.EasyMock;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;
import org.sakaiproject.nakamura.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class CropItServletTest extends AbstractEasyMockTest {

  private CropItServlet servlet;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest#setUp()
   */
  @Override
  public void setUp() throws Exception {
    // TODO Auto-generated method stub
    super.setUp();

    servlet = new CropItServlet();
  }

  @Test
  public void testProperPost() throws ServletException, IOException, RepositoryException,
      JSONException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    // Provide parameters
    String[] dimensions = new String[] { "16x16", "32x32" };
    addStringRequestParameter(request, "img", "/~johndoe/people.png");
    addStringRequestParameter(request, "save", "/~johndoe/breadcrumbs");
    addStringRequestParameter(request, "x", "10");
    addStringRequestParameter(request, "y", "10");
    addStringRequestParameter(request, "width", "70");
    addStringRequestParameter(request, "height", "70");
    addStringRequestParameter(request, "dimensions", StringUtils.join(dimensions, 0, ';'));
    expect(request.getRemoteUser()).andReturn("johndoe");

    // Session stuff
    JackrabbitSession session = createMock(JackrabbitSession.class);
    ResourceResolver resolver = createMock(ResourceResolver.class);
    expect(resolver.adaptTo(Session.class)).andReturn(session);
    expect(request.getResourceResolver()).andReturn(resolver);

    ValueFactory valueFactory = createMock(ValueFactory.class);
    expect(session.getValueFactory()).andReturn(valueFactory).anyTimes();

    // Base image stream

    // Item retrieval stuff
    Node imgNode = createMock(Node.class);
    Node imgContentNode = createMock(Node.class);
    expect(imgContentNode.isNodeType("nt:resource")).andReturn(true);
    Property imgContentData = createMock(Property.class);
    Property imgContentType = createMock(Property.class);

    expect(imgNode.isNodeType("nt:file")).andReturn(true);
    expect(imgNode.getName()).andReturn("people.png");
    expect(imgNode.getPath()).andReturn("/path/to/the/file/people.png");
    expect(imgNode.hasNode("jcr:content")).andReturn(true);
    expect(imgNode.getNode("jcr:content")).andReturn(imgContentNode);

    Binary bin = createMock(Binary.class);
    expect(imgContentData.getBinary()).andReturn(bin);
    byte[] b = IOUtils.getInputStreamBytes(getClass().getClassLoader().getResourceAsStream("people.png"));
    expect(bin.getSize()).andReturn((long)b.length);
    expect(bin.getStream()).andReturn(new ByteArrayInputStream(b));
    expect(imgContentType.getString()).andReturn("image/png");
    expect(imgContentNode.getProperty("jcr:data")).andReturn(imgContentData);
    expect(imgContentNode.hasProperty("jcr:mimeType")).andReturn(true);
    expect(imgContentNode.getProperty("jcr:mimeType")).andReturn(imgContentType);
    
    // user resolution
    UserManager userManager = createMock(UserManager.class);
    expect(session.getUserManager()).andReturn(userManager).anyTimes();
    User user = createMock(User.class);
    expect(userManager.getAuthorizable("johndoe")).andReturn(user).anyTimes();
    ItemBasedPrincipal principal = createMock(ItemBasedPrincipal.class);
    expect(user.getPrincipal()).andReturn(principal).anyTimes();
    expect(user.isGroup()).andReturn(false).anyTimes();
    expect(principal.getPath()).andReturn("/rep:security/rep:authorizables/rep:users/j/jo/jon/johndoe").anyTimes();
    String userPath = "/_user/j/jo/jon/johndoe";
    
    for (String s : dimensions) {
      Node breadCrumbNode = EasyMock.createMock(Node.class);
      Node breadCrumbContentNode = EasyMock.createMock(Node.class);
      expect(valueFactory.createBinary(isA(InputStream.class))).andReturn(bin);
      expect(breadCrumbContentNode.setProperty(eq("jcr:data"), eq(bin)))
          .andReturn(null);
      expect(breadCrumbContentNode.setProperty("jcr:mimeType", "image/png")).andReturn(
          null);
      expect(
          breadCrumbContentNode.setProperty(eq("jcr:lastModified"), EasyMock
              .isA(Calendar.class))).andReturn(null);

      expect(breadCrumbNode.hasNode("jcr:content")).andReturn(true);
      expect(breadCrumbNode.getNode("jcr:content")).andReturn(breadCrumbContentNode);

      EasyMock.replay(breadCrumbContentNode, breadCrumbNode);

      expect(session.itemExists(userPath+"/breadcrumbs/" + s + "_people.png")).andReturn(true);
      expect(session.getItem(userPath+"/breadcrumbs/" + s + "_people.png")).andReturn(
          breadCrumbNode);

      expect(session.hasPendingChanges()).andReturn(true);
      session.save();

    }
    expect(session.itemExists(userPath+"/people.png")).andReturn(true);
    expect(session.getItem(userPath+"/people.png")).andReturn(imgNode);
    expect(session.itemExists(userPath+"/breadcrumbs")).andReturn(true);

    // Capture output.
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter write = new PrintWriter(baos);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    expect(response.getWriter()).andReturn(write);

    replay();
    servlet.doPost(request, response);
    write.flush();

    String s = baos.toString("UTF-8");
    JSONObject o = new JSONObject(s);

    JSONArray files = o.getJSONArray("files");
    assertEquals(2, files.length());
    for (int i = 0; i < files.length(); i++) {
      String url = files.getString(i);
      assertEquals("/~johndoe/breadcrumbs/" + dimensions[i] + "_people.png", url);
    }
  }

  @Test
  public void testAnon() throws IOException, ServletException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    expect(request.getRemoteUser()).andReturn("anonymous");
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
        "Anonymous user cannot crop images.");
    replay();
    servlet.doPost(request, response);
  }

  @Test
  public void testMissingParameters() throws IOException, ServletException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);

    expect(request.getRequestParameter("img")).andReturn(null);
    expect(request.getRemoteUser()).andReturn("johndoe");
    addStringRequestParameter(request, "save", null);
    addStringRequestParameter(request, "x", null);
    addStringRequestParameter(request, "y", null);
    addStringRequestParameter(request, "width", null);
    addStringRequestParameter(request, "height", null);
    addStringRequestParameter(request, "dimensions", "");

    response
        .sendError(HttpServletResponse.SC_BAD_REQUEST,
            "The following parameters are required: img, save, x, y, width, height, dimensions");

    replay();

    servlet.doPost(request, response);
  }

  @Test
  public void testImageException() {
    ImageException e = new ImageException(500, "foo");
    assertEquals(500, e.getCode());
    assertEquals("foo", e.getMessage());

    e = new ImageException();
    e.setCode(500);
    assertEquals(500, e.getCode());

  }

  @Test
  public void testCheck() {
    int val = servlet.checkIntBiggerThanZero(5, 1);
    assertEquals(5, val);

    val = servlet.checkIntBiggerThanZero(0, 1);
    assertEquals(0, val);

    val = servlet.checkIntBiggerThanZero(-5, 1);
    assertEquals(1, val);
  }

}
