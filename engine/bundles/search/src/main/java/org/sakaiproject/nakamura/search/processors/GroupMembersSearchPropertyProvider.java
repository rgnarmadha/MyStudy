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
package org.sakaiproject.nakamura.search.processors;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 *
 */
@Component
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SearchConstants.REG_PROVIDER_NAMES, value = "GroupMembers")
})
public class GroupMembersSearchPropertyProvider implements SearchPropertyProvider {
  private static final Logger logger = LoggerFactory
      .getLogger(GroupMembersSearchPropertyProvider.class);

  @Reference
  protected SlingRepository slingRepository;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    try {
      Session session = request.getResourceResolver().adaptTo(Session.class);
      UserManager um = AccessControlUtil.getUserManager(session);

      if (request.getParameter("q") == null) {
        throw new IllegalArgumentException(
            "Must provide 'q' parameter to use for search.");
      }

      // get the request group name
      String groupName = request.getParameter("group");
      if (groupName == null) {
        throw new IllegalArgumentException("Must provide group to search within.");
      }

      // get the authorizable associated to the requested group name
      Group group = (Group) um.getAuthorizable(groupName);
      if (group == null) {
        throw new IllegalArgumentException("Unable to find group [" + groupName + "]");
      }

      LinkedHashSet<String> memberIds = new LinkedHashSet<String>();

      // collect the declared members of the requested group
      addDeclaredMembers(memberIds, group);

      // get the managers group for the requested group and collect its members
      addDeclaredManagerMembers(memberIds, group);

      boolean includeSelf = Boolean.parseBoolean(request.getParameter("includeSelf"));
      String currentUser = request.getRemoteUser();
      if (!includeSelf) {
        memberIds.remove(currentUser);
      }

      // 900 is the number raydavis said we should split on. This can be tuned as needed.
      if (memberIds.size() > 900) {
        // more than the threshold; pass along for post processing
        request.setAttribute("memberIds", memberIds);
      } else {
        // update the query to filter before writing nodes
        String users = StringUtils.join(memberIds, "' or rep:userId='");
        propertiesMap.put("_groupQuery", "and (rep:userId='" + users + "')");
      }
    } catch (RepositoryException e) {
      logger.error(e.getMessage(), e);
    }
  }

  /**
   * Add any declared members of {@link group} to {@link memberIds}.
   *
   * @param memberIds List to collect member IDs.
   * @param group Group to plunder through for member IDs.
   * @throws RepositoryException
   */
  private void addDeclaredMembers(Collection<String> memberIds, Group group)
      throws RepositoryException {
    Iterator<Authorizable> members = group.getDeclaredMembers();
    while (members.hasNext()) {
      String memberId = members.next().getID();
      if (!memberIds.contains(memberId)) {
        memberIds.add(memberId);
      }
    }
  }

  /**
   * Add any declared manager members of {@link group} to {@link memberIds}.
   *
   * @param memberIds List to collect member IDs.
   * @param group Group to plunder through for manager member IDs.
   * @param um UserManager for digging up the manager group.
   * @throws RepositoryException
   */
  private void addDeclaredManagerMembers(Collection<String> memberIds, Group group)
      throws RepositoryException {

    Session adminSession = null;

    try {
      adminSession = slingRepository.loginAdministrative(null);
      UserManager um = AccessControlUtil.getUserManager(adminSession);

      if (group.hasProperty(UserConstants.PROP_MANAGERS_GROUP)) {
        Value[] values = group.getProperty(UserConstants.PROP_MANAGERS_GROUP);
        if (values != null && values.length == 1) {
          String managerGroupId = values[0].getString();
          Group managerGroup = (Group) um.getAuthorizable(managerGroupId);
          if (managerGroup != null) {
            addDeclaredMembers(memberIds, managerGroup);
          } else {
            logger.warn("Unable to find manager's group [" + managerGroupId + "]");
          }
        }
      }
    } finally {
      if (adminSession != null) {
        adminSession.logout();
      }
    }
  }
}
