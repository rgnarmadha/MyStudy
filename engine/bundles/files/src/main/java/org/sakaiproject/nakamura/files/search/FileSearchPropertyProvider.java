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
package org.sakaiproject.nakamura.files.search;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;
import org.sakaiproject.nakamura.api.site.SiteException;
import org.sakaiproject.nakamura.api.site.SiteService;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

/**
 * Provides properties to process the search
 * 
 */
@Component(immediate = true, label = "FileSearchPropertyProvider", description = "Property provider for file searches")
@Service(value = SearchPropertyProvider.class)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.provider", value = "Files") })
public class FileSearchPropertyProvider implements SearchPropertyProvider {

  @Reference
  protected SiteService siteService;

  @Reference
  protected ConnectionManager connectionManager;

  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {

    Session session = request.getResourceResolver().adaptTo(Session.class);
    String user = request.getRemoteUser();
    Authorizable auUser;
    try {
      auUser = PersonalUtils.getAuthorizable(session, user);
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }

    // Set the userid.
    propertiesMap.put("_me", user);

    // Set the public space.
    propertiesMap
        .put("_mySpace", ISO9075.encodePath(PersonalUtils.getPublicPath(auUser)));

    // Set the contacts.
    propertiesMap.put("_mycontacts", getMyContacts(user));

    // Set all mysites.
    propertiesMap.put("_mysites", getMySites(session, user));

    // request specific.
    // Sorting order
    propertiesMap.put("_order", doSortOrder(request));

    // Filter by site
    String usedinClause = doUsedIn(request);
    String tags = doUsedIn(request);
    String tagsAndUsedIn = "";
    if (tags.length() > 0) {
      propertiesMap.put("_usedin", " and (" + tags + ")");
      tagsAndUsedIn = tags;
    }
    
    if (usedinClause.length() > 0) {
      propertiesMap.put("_usedin", " and (" + usedinClause + ")");
      tagsAndUsedIn = "(" + tagsAndUsedIn + ") and (" + usedinClause + ")";
      
    }
    propertiesMap.put("_tags_and_usedin", tagsAndUsedIn);

    
  }

  /**
   * Filter files by looking up where they are used.
   * 
   * @param request
   * @return
   */
  protected String doUsedIn(SlingHttpServletRequest request) {
    String usedin[] = request.getParameterValues("usedin");
    if (usedin != null && usedin.length > 0) {
      StringBuilder sb = new StringBuilder();

      for (String u : usedin) {
        sb.append("jcr:contains(@sakai:linkpaths,\"").append(u).append("\") or ");
      }

      String usedinClause = sb.toString();
      int i = usedinClause.lastIndexOf(" or ");
      if (i > -1) {
        usedinClause = usedinClause.substring(0, i);
      }
      if (usedinClause.length() > 0) {
        return usedinClause;
      }
    }
    return "";
  }

  protected String getSearchValue(SlingHttpServletRequest request) {
    RequestParameter searchParam = request.getRequestParameter("q");
    String search = "*";
    if (searchParam != null) {
      search = escapeString(searchParam.getString(), Query.XPATH);
      if (search.equals(""))
        search = "*";
    }
    return search;
  }

  /**
   * Returns default sort order.
   * 
   * @param request
   * @return
   */
  protected String doSortOrder(SlingHttpServletRequest request) {
    RequestParameter sortOnParam = request.getRequestParameter("sortOn");
    RequestParameter sortOrderParam = request.getRequestParameter("sortOrder");
    String sortOn = "sakai:filename";
    String sortOrder = "ascending";
    if (sortOrderParam != null
        && (sortOrderParam.getString().equals("ascending") || sortOrderParam.getString()
            .equals("descending"))) {
      sortOrder = sortOrderParam.getString();
    }
    if (sortOnParam != null) {
      sortOn = sortOnParam.getString();
    }
    return " order by @" + sortOn + " " + sortOrder;
  }

  /**
   * Gets a clause for a query by looking at the sakai:tags request parameter.
   * 
   * @param request
   * @return
   */
  protected String doTags(SlingHttpServletRequest request) {
    String[] tags = request.getParameterValues("sakai:tags");
    if (tags != null) {
      StringBuilder sb = new StringBuilder();

      for (String t : tags) {
        sb.append("@sakai:tags=\"");
        sb.append(t);
        sb.append("\" and ");
      }
      String tagClause = sb.toString();
      int i = tagClause.lastIndexOf(" and ");
      if (i > -1) {
        tagClause = tagClause.substring(0, i);
      }
      if (tagClause.length() > 0) {
        tagClause = " and (" + tagClause + ")";
        return tagClause;
      }
    }
    return "";
  }

  /**
   * Get a user his sites.
   * 
   * @param session
   * @param user
   * @return
   */
  @SuppressWarnings(justification = "siteService is OSGi managed", value = {
      "NP_UNWRITTEN_FIELD", "UWF_UNWRITTEN_FIELD" })
  protected String getMySites(Session session, String user) {
    try {
      StringBuilder sb = new StringBuilder();
      Map<String, List<Group>> membership = siteService.getMembership(session, user);
      for (Entry<String, List<Group>> site : membership.entrySet()) {
        sb.append("@sakai:sites=\"").append(site.getKey()).append("\" or ");
      }
      String sites = sb.toString();
      int i = sites.lastIndexOf(" or ");
      if (i > -1) {
        sites = sites.substring(0, i);
      }
      if (sites.length() > 0) {
        sites = " and (" + sites + ")";
      }
      return sites;

    } catch (SiteException e1) {
      return "";
    }
  }

  /**
   * Escape a parameter string so it doesn't contain anything that might break the query.
   * 
   * @param value
   * @param queryLanguage
   * @return
   */
  protected String escapeString(String value, String queryLanguage) {
    String escaped = null;
    if (value != null) {
      if (queryLanguage.equals(Query.XPATH) || queryLanguage.equals(Query.SQL)) {
        // See JSR-170 spec v1.0, Sec. 6.6.4.9 and 6.6.5.2
        escaped = value.replaceAll("\\\\(?![-\"])", "\\\\\\\\").replaceAll("'", "\\\\'")
            .replaceAll("'", "''");
      }
    }
    return escaped;
  }

  /**
   * Get a string of all the connected users.
   * 
   * @param user
   *          The user to get the contacts for.
   * @return and (@sakai:user=\"simon\" or @sakai:user=\"ieb\")
   */
  @SuppressWarnings(justification = "connectionManager is OSGi managed", value = {
      "NP_UNWRITTEN_FIELD", "UWF_UNWRITTEN_FIELD" })
  protected String getMyContacts(String user) {
    List<String> connectedUsers = connectionManager.getConnectedUsers(user,
        ConnectionState.ACCEPTED);
    StringBuilder sb = new StringBuilder();
    for (String u : connectedUsers) {
      sb.append("@jcr:createdBy=\"").append(u).append("\" or ");
    }
    String usersClause = sb.toString();
    int i = usersClause.lastIndexOf(" or ");
    if (i > -1) {
      usersClause = usersClause.substring(0, i);
    }
    if (usersClause.length() > 0) {
      usersClause = " and (" + usersClause + ")";
      return usersClause;
    }

    return "";
  }

}
