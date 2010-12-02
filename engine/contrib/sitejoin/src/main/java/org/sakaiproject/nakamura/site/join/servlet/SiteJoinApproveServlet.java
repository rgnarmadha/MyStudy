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
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.site.SiteException;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.site.SiteService.SiteEvent;
import org.sakaiproject.nakamura.api.site.join.JoinRequestConstants;
import org.sakaiproject.nakamura.api.site.join.JoinRequestUtil;
import org.sakaiproject.nakamura.site.SiteEventUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@SlingServlet(resourceTypes = "sakai/site", methods = "POST", selectors = "approve")
public class SiteJoinApproveServlet extends SlingAllMethodsServlet {
  private static final long serialVersionUID = -522151562754810962L;
  private static final Logger logger = LoggerFactory
      .getLogger(SiteJoinApproveServlet.class);

//  @Reference
//  private EventAdmin eventAdmin;

  @Reference
  private SiteService siteService;

  @Reference
  private SlingRepository slingRepository;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    String paramUser = request.getParameter(SiteService.SiteEvent.USER);
    String paramGroup = "";
    try {
      Node site = request.getResource().adaptTo(Node.class);
      Session session = slingRepository.loginAdministrative(null);
      UserManager userManager = AccessControlUtil.getUserManager(session);

      
      Authorizable userAuth = userManager.getAuthorizable(paramUser);

      // get the join request
      Node joinRequest = JoinRequestUtil.getRequest(site.getPath(), paramUser, session);
      
      if (joinRequest.hasProperty(JoinRequestConstants.PROP_TARGET_GROUP)) {
        paramGroup = joinRequest.getProperty(JoinRequestConstants.PROP_TARGET_GROUP).getString();
      }
      Group groupAuth = (Group) userManager.getAuthorizable(paramGroup);
      String requestState = null;
      if (joinRequest.hasProperty(JoinRequestConstants.PROP_REQUEST_STATE)) {
        requestState = joinRequest.getProperty(JoinRequestConstants.PROP_REQUEST_STATE)
            .getString();
      }

      // verify join request is 'pending'
      // verify the user isn't already part of the site
      // verify user making request is a maintainer of the site
      if ("pending".equals(requestState) && !siteService.isMember(site, userAuth)
          && siteService.isUserSiteMaintainer(site)) {

        // add user to the site's group
        groupAuth.addMember(userAuth);
//        postEvent(SiteEvent.joinedSite, site, groupAuth);

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

//  private void postEvent(SiteEvent event, Node site, Group targetGroup)
//      throws SiteException {
//    try {
//      eventAdmin.postEvent(SiteEventUtil.newSiteEvent(event, site, targetGroup, null));
//    } catch (RepositoryException ex) {
//      logger.warn(ex.getMessage(), ex);
//      throw new SiteException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
//          ex.getMessage());
//    }
//  }
}
