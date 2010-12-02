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

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.easymock.EasyMock;
import org.junit.Before;
import org.sakaiproject.nakamura.site.AbstractSiteServiceTest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;

public abstract class AbstractSiteServiceServletTest extends AbstractSiteServiceTest {

  protected SlingHttpServletRequest request;
  protected SlingHttpServletResponse response;
  protected JackrabbitSession session;

  protected abstract void makeRequest() throws ServletException, IOException;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    request = createMock(SlingHttpServletRequest.class);
    response = createNiceMock(SlingHttpServletResponse.class);
    session = createMock(JackrabbitSession.class);
    expect(session.getUserManager()).andReturn(userManager).anyTimes();
    expect(slingRepository.loginAdministrative((String) eq(null))).andReturn(session).anyTimes();
    session.logout();
    EasyMock.expectLastCall().anyTimes();
    expect(session.hasPendingChanges()).andReturn(true).anyTimes();
    session.save();
    EasyMock.expectLastCall().anyTimes();
  }

  public byte[] makeGetRequestReturningBytes() throws IOException, ServletException
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(baos);
    expect(response.getWriter()).andReturn(writer);    
    makeRequest();
    writer.flush();
    return baos.toByteArray();
  }
  
  public JSONArray makeGetRequestReturningJSONresults() throws IOException, ServletException, JSONException
  {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    String jsonString = new String(makeGetRequestReturningBytes());
    JSONObject obj = new JSONObject(jsonString);
    return obj.getJSONArray("results");
  }

}
