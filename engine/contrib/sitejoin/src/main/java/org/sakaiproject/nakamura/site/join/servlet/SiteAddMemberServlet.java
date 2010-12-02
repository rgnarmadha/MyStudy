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
import org.sakaiproject.nakamura.api.auth.trusted.RequestTrustValidator;
import org.sakaiproject.nakamura.api.auth.trusted.RequestTrustValidatorService;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.site.join.JoinRequestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 *
 */
@SlingServlet(resourceTypes = "sakai/site", methods = "POST", selectors = "addmember")
public class SiteAddMemberServlet extends SlingAllMethodsServlet {
	private static final long serialVersionUID = -522151562754810962L;
	private static final Logger logger = LoggerFactory
			.getLogger(SiteJoinApproveServlet.class);

	@Reference
	private SlingRepository slingRepository;

	@Reference
	protected RequestTrustValidatorService requestTrustValidatorService;

	@Override
	protected void doPost(SlingHttpServletRequest request,
			SlingHttpServletResponse response) throws ServletException,
			IOException {
		String paramUser = request.getParameter(SiteService.SiteEvent.USER);
		logger.info("Request to add user " + paramUser);
		String paramGroup = "";
		try {
			Node requestedNode = request.getResource().adaptTo(Node.class);
			Value[] authorizables = requestedNode.getProperty("sakai:authorizables").getValues();
			paramGroup = authorizables[1].getString();
			request.setAttribute(JoinRequestConstants.PARAM_SITENODE, requestedNode);
			Session session = slingRepository.loginAdministrative(null);
			UserManager userManager = AccessControlUtil.getUserManager(session);
			Authorizable userAuth = userManager.getAuthorizable(paramUser);
			Group groupAuth = (Group) userManager.getAuthorizable(paramGroup);
			if (siteJoinIsAuthorized(request)) {
				groupAuth.addMember(userAuth);
				logger.info(paramUser + " added as member of group " + paramGroup);
			} else {
				response.sendError(403, "Not authorized to add member to site.");
			}
			if (session.hasPendingChanges()) {
				session.save();
			}
		} catch (Exception e) {
			response.sendError(500, e.getMessage());
		}

	}

	private boolean siteJoinIsAuthorized(HttpServletRequest request) {
		boolean trustedRequest = false;
		String trustMechanism = request.getParameter(":join-auth");
		if (trustMechanism != null) {
			RequestTrustValidator validator = requestTrustValidatorService
					.getValidator(trustMechanism);
			if (validator != null
					&& validator.getLevel() >= RequestTrustValidator.CREATE_USER
					&& validator.isTrusted(request)) {
				trustedRequest = true;
			}
		}
		return trustedRequest;
	}
}
