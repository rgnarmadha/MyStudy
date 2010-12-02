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
package org.sakaiproject.nakamura.activity.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.activity.ActivityConstants;
import org.sakaiproject.nakamura.api.activity.ActivityUtils;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Component(immediate = true, enabled = true, label = "ActivitySearchPropertyProvider")
@Properties(value = {
    @Property(name = "sakai.search.provider", value = "Activity"),
    @Property(name = "sakai.search.resourceType", value = "sakai/page"),
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Provides properties to the activity search templates.") })
@Service(value = SearchPropertyProvider.class)
public class ActivitySearchPropertyProvider implements SearchPropertyProvider {

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {

    // The current user his feed.
    String user = request.getRemoteUser();
    if (user.equals(UserConstants.ANON_USERID)) {
      throw new IllegalStateException("Anonymous users can't see the feed.");
    }
    Authorizable au;
    try {
      Session session = request.getResourceResolver().adaptTo(Session.class);
      UserManager um = AccessControlUtil.getUserManager(session);
      au = um.getAuthorizable(user);
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
    String path = ActivityUtils.getUserFeed(au);
    // Encode the path
    path = ISO9075.encodePath(path);
    propertiesMap.put("_myFeed", path);

    // Encode the site path.
    RequestParameter siteParam = request.getRequestParameter("site");
    if (siteParam != null && !siteParam.getString().equals("")) {
      String sitePath = siteParam.getString();
      sitePath += "/" + ActivityConstants.ACTIVITY_FEED_NAME;
      sitePath = ISO9075.encodePath(sitePath);
      propertiesMap.put("_siteFeed", sitePath);
    }

  }

}
