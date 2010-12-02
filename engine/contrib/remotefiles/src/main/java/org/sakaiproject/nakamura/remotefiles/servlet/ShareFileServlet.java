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
package org.sakaiproject.nakamura.remotefiles.servlet;


import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.nakamura.remotefiles.RemoteFilesRepository;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

@SlingServlet(resourceTypes = { "sakai/remotefiles-share" }, methods = { "POST" }, generateComponent = true, generateService = true)
public class ShareFileServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 3794459685138851653L;

  @SuppressWarnings(value = "NP_UNWRITTEN_FIELD", justification = "Injected by OSGi")
  @Reference
  private transient RemoteFilesRepository remoteFilesRepository;

  @Override
  protected void doPost(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException,
      IOException {
    try {
      String currentUserId = request.getResource().adaptTo(Node.class).getSession().getUserID();
      String resource = request.getParameter("resource");
      String groupId = request.getParameter("groupid");

      boolean success = remoteFilesRepository.shareFileWithGroup(groupId, resource, currentUserId);
      String status = success ? "success" : "failed";
      response.setContentType("application/json");
      PrintWriter out = response.getWriter();
      out.println("{\"status\":\""+status+"\"}");
      out.flush();
    } catch (RepositoryException e) {
      response.sendError(500, e.getMessage());
    }
  }

}
