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
package org.sakaiproject.nakamura.profile.servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONException;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.profile.ProfileServiceImplTest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutionException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

/**
 *
 */
public class ProfileServletTest {


  private ProfileServiceImplTest profileServiceImpleTest;
  @Mock
  private SlingHttpServletRequest request;
  @Mock
  private SlingHttpServletResponse response;
  @Mock
  private Resource resource;

  /**
   *
   */
  public ProfileServletTest() {
   MockitoAnnotations.initMocks(this);
   profileServiceImpleTest = new ProfileServiceImplTest();
   MockitoAnnotations.initMocks(profileServiceImpleTest);
  }

  @Test
  public void testGet() throws ServletException, RepositoryException, InterruptedException, ExecutionException, IOException, JSONException {
    ProfileServlet profileServlet = new ProfileServlet();
    profileServlet.profileService = profileServiceImpleTest.setupProfileService();
    profileServlet.init();
    StringWriter w = new StringWriter();
    Mockito.when(response.getWriter()).thenReturn(new PrintWriter(w));
    Mockito.when(request.getResource()).thenReturn(resource);
    Mockito.when(resource.adaptTo(Node.class)).thenReturn(profileServiceImpleTest.getBaseNode());

    profileServlet.doGet(request, response);

    profileServiceImpleTest.checkResponse(w);
  }

}
