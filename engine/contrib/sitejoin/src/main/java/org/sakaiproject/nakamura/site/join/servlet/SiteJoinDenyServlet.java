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
 * specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.site.join.servlet;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.site.join.JoinRequestConstants;
import org.sakaiproject.nakamura.api.site.join.JoinRequestUtil;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@SlingServlet(resourceTypes = "sakai/site", methods = "POST", selectors = "deny")
public class SiteJoinDenyServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = -7146621369733216817L;

  @Reference
  private SiteService siteService;

  @Reference
  private SlingRepository slingRepository;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    String paramUser = request.getParameter(SiteService.SiteEvent.USER);

    try {
      Node site = request.getResource().adaptTo(Node.class);
      Session session = slingRepository.loginAdministrative(null);

      // get the join request
      Node joinRequest = JoinRequestUtil.getRequest(site.getPath(), paramUser, session);
      String requestState = null;
      if (joinRequest.hasProperty(JoinRequestConstants.PROP_REQUEST_STATE)) {
        requestState = joinRequest.getProperty(JoinRequestConstants.PROP_REQUEST_STATE)
            .getString();
      }

      // verify join request is 'pending'
      // verify user making request is a maintainer of the site
      if ("pending".equals(requestState) && siteService.isUserSiteMaintainer(site)) {

        // remove the pending request
        joinRequest.remove();
      }

      if (session.hasPendingChanges()) {
        session.save();
      }
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

}
