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
package org.sakaiproject.nakamura.files.pool;

import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_MEMBERS_NODE;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_MANAGER;
import static org.sakaiproject.nakamura.api.files.FilesConstants.POOLED_CONTENT_USER_VIEWER;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Service
@Component(immediate = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Provides some extra properties for the PooledContent searches."),
    @Property(name = "sakai.search.provider", value = "PooledContent") })
public class ContentPoolSearchPropertyProvider implements SearchPropertyProvider {

  public static final Logger LOGGER = LoggerFactory
      .getLogger(ContentPoolSearchPropertyProvider.class);

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    String userID = request.getRemoteUser();
    Session session = request.getResourceResolver().adaptTo(Session.class);

    if (!UserConstants.ANON_USERID.equals(userID)) {
      addMyGroups(session, propertiesMap);
    }

  }

  /**
   * Gets all the declared groups for a user and adds an xpath constraint for both
   * managers and viewers to the map.
   *
   * @param session
   * @param propertiesMap
   */
  protected void addMyGroups(Session session, Map<String, String> propertiesMap) {
    String userID = session.getUserID();
    try {
      Authorizable au = PersonalUtils.getAuthorizable(session, userID);
      String path = PersonalUtils.getUserHashedPath(au).substring(1);
      String membersRelativePath = POOLED_CONTENT_MEMBERS_NODE.substring(1);
      String safePath = membersRelativePath + "/" + ISO9075.encodePath(path);

      // Get all the groups I'm a member of and add the property in the map.
      Iterator<Group> groups = au.memberOf();
      StringBuilder sbManagingGroups = new StringBuilder("(");
      sbManagingGroups.append(safePath).append("/@");
      sbManagingGroups.append(POOLED_CONTENT_USER_MANAGER);
      sbManagingGroups.append("='").append(au.getID()).append("'");

      StringBuilder sbViewingGroups = new StringBuilder("(");
      sbViewingGroups.append(safePath).append("/@");
      sbViewingGroups.append(POOLED_CONTENT_USER_VIEWER);
      sbViewingGroups.append("='").append(au.getID()).append("'");

      while (groups.hasNext()) {
        Group g = groups.next();
        path = PersonalUtils.getUserHashedPath(g).substring(1);
        safePath = membersRelativePath + "/" + ISO9075.encodePath(path);

        // Add the group to the managers contraint
        sbManagingGroups.append(" or ").append(safePath).append("/@").append(
            POOLED_CONTENT_USER_MANAGER);
        sbManagingGroups.append("='").append(g.getID()).append("'");

        // Add the group to the viewers contraint
        sbViewingGroups.append(" or ").append(safePath).append("/@").append(
            POOLED_CONTENT_USER_VIEWER);
        sbViewingGroups.append("='").append(g.getID()).append("'");

      }

      // Close contraint.
      sbManagingGroups.append(") ");
      sbViewingGroups.append(") ");


      // Add the 2 properties to the map.
      propertiesMap.put("_meManagerGroupsNoAnd", sbManagingGroups.toString());
      propertiesMap.put("_meViewerGroupsNoAnd", sbViewingGroups.toString());
      sbManagingGroups.insert(0, " and ");
      sbViewingGroups.insert(0, " and ");
      propertiesMap.put("_meManagerGroups", sbManagingGroups.toString());
      propertiesMap.put("_meViewerGroups", sbViewingGroups.toString());
    } catch (RepositoryException e) {
      LOGGER.error("Could not get the groups for user [{}].",userID , e);
    }
  }
}
