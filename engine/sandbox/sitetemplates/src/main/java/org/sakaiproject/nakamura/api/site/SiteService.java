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
package org.sakaiproject.nakamura.api.site;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;

import java.util.AbstractCollection;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Site service wraps site management in Sakai Nakamura.
 */
public interface SiteService {

  /**
   * The joinable property
   */
  public enum Joinable {
    /**
     * The site is joinable.
     */
    yes(),
    /**
     * The site is not joinable.
     */
    no(),
    /**
     * Is joinable.
     */
    withauth();
  }

  /**
   * An Event Enumeration for all the events that the Site Service might emit.
   */
  public enum SiteEvent {
    /**
     * This event is posted to indicate the at join workflow should be started for the
     * user.
     */
    startJoinWorkflow(),
    /**
     * Indicates that a user just joined the site.
     */
    joinedSite(),
    /**
     * Indicates a user just left the site.
     */
    unjoinedSite();
    /**
     * The topic that the event is sent as.
     */
    public static final String TOPIC = "org/sakaiproject/nakamura/api/site/event/";
    /**
     * The event property name use to store the JCR path to the site.
     */
    public static final String SITE = "site";
    /**
     * The use that is the subject of the event.
     */
    public static final String USER = "user";
    /**
     * The target group of the request.
     */
    public static final String GROUP = "group";

    /**
     * @return a topic ID for sites, bound to the operation being performed.
     */
    public String getTopic() {
      return TOPIC + toString();
    }

  }

  /**
   * @param site
   *          the site to test.
   * @return true if item represents a site.
   */
  boolean isSite(Item site);

  /**
   * @param site
   *          the site to test.
   * @return true if item represents a site template.
   */
  boolean isSiteTemplate(Item site);

  /**
   * @param site
   *          the site in question.
   * @return the joinable status of the site.
   */
  Joinable getJoinable(Node site);

  /**
   * @param authorizable
   *          the authorizable in question.
   * @return the joinable status of the authorizable
   */
  Joinable getJoinable(Authorizable authorizable);

  /**
   * Is the group a member of the site, either directly or implied.
   * 
   * @param site
   *          the site in question.
   * @param group
   *          the group under test.
   * @return true if the group is a member of the site.
   */
  boolean isMember(Node site, Authorizable group);

  /**
   * Is the user who got the site node a maintainer of the site.
   * 
   * @param site
   *          The site in question
   * @return true is the session who is bound to the sitenode has maintainer access on it.
   */
  boolean isUserSiteMaintainer(Node site) throws RepositoryException;

  /**
   * @param site
   *          the site in question.
   * @return the path to the template-site where the given site is based on.
   */
  String getSiteTemplate(Node site) throws SiteException;

  /**
   * 
   * @param site
   *          the site in question
   * @return the path to the template associated with the site, may be the default site
   *         template if none is specified.
   * @throws SiteException
   */
  String getSiteSkin(Node site) throws SiteException;

  /**
   * Retrieves a flat list of the members. This will only contain {@link UserKey UserKey}
   * objects. If a group object is encountered, the declared members will be retrieved.
   * (if requested)
   * 
   * @param site
   *          The node that represents the site.
   * @param start
   *          How many items should be skipped.
   * @param nitems
   *          How many items should be retrieved. -1 will retrieve everything.
   * @param sort
   *          What the Users should be sorted on. Can be null.
   * @return
   * @throws RepositoryException
   */
  AbstractCollection<AuthorizableKey> getMembers(Node site, int start, int nitems,
      Sort[] sort) throws SiteException;

  /**
   * Retrieves a flat list of the members. This will only contain {@link UserKey UserKey}
   * objects. If a group object is encountered, the declared members will be retrieved.
   * (if requested)
   * 
   * @param site
   *          The node that represents the site.
   * @param start
   *          How many items should be skipped.
   * @param nitems
   *          How many items should be retrieved. -1 will retrieve everything.
   * @param sort
   *          What the Users should be sorted on. Can be null.
   * @param maxLevels
   *          The amount of group nesting that should be resolved.
   * @return
   * @throws RepositoryException
   */
  AbstractCollection<AuthorizableKey> getMembers(Node site, int start, int nitems,
      Sort[] sort, int maxLevels) throws SiteException;

  /**
   * Returns a tree of members for a site. These can be both groups and users.
   * 
   * 
   * @param site
   *          The node that represents the site.
   * @param start
   *          The starting position. This will only work for the first level.
   * @param nitems
   *          The amount of items that should be retrieved for the first level.
   * @param sort
   *          What the first level authorizables should be sorted on.
   * @param maxLevels
   *          The amount of sub groups that need to be resolved. Assume the following
   *          group structure exists in JCR. (+ is a group, - is a user).
   * 
   *          <pre>
   *          + Apple
   *             + Apple Managers
   *                - Steve Jobs
   *                - Tim Cook
   *                + Apple HR managers
   *                   - ...
   *                + Apple Sales managers
   *                   - ...
   *             + Apple Developers
   *                + OS X Developers
   *                   - ...
   *                + iPhone Developers
   *                   - ...
   *                - Developer XYZ
   * </pre>
   * 
   *          Assume a site where the entire Apple group is added on.
   *          <ul>
   *          <li>-1 : All<br />
   *          Everything is expanded.</li>
   *          <li>0 : None<br />
   *          "Apple" is returned, none of it's members or subgroups will be shown.</li>
   *          <li>1 : The first groups will be resolved and it's Users will be added. The
   *          subgroups will be added, but their members won't.<br />
   *          "Apple" is returned. It will include the "Apple Managers" and
   *          "Apple Developers" as well. None of the subgroup/members are added though.</li>
   *          <li>2 : The first groups will be resolved and it's Users will be added. The
   *          subgroups will be added including their members.</li>
   *          <li>...</li>
   *          </ul>
   * @return
   * @throws RepositoryException
   */
  public AbstractCollection<AuthorizableKey> getTreeMembers(Node site, int start,
      int nitems, Sort[] sort, int maxLevels) throws SiteException;

  /**
   * Returns the number of declared members of a site
   * 
   * @param site
   *          the Site node
   * @return The number of members
   */
  int getMemberCount(Node site) throws SiteException;

  /**
   * Get and Iterator of Groups for the site.
   * 
   * @param site
   *          the site node.
   * @param start
   *          the first group.
   * @param nitems
   *          the number of groups.
   * @param sort
   *          a specification for sorting.
   * @return an Iterator of groups for the list requested.
   * @throws SiteException
   *           when there is an internal problem with getting the groups.
   */
  AbstractCollection<AuthorizableKey> getGroups(Node site, int start, int nitems,
      Sort[] sort) throws SiteException;

  /**
   * 
   * @param site
   * @param start
   * @param nitems
   * @param sort
   * @param maxLevels
   * @return
   * @throws SiteException
   */
  AbstractCollection<AuthorizableKey> getGroups(Node site, int start, int nitems,
      Sort[] sort, int maxLevels) throws SiteException;

  /**
   * Looks up the 
   * @param session
   * @param user
   * @throws SiteException
   */
  Map<String, List<Group>> getMembership(Session session, String user)
      throws SiteException;

  /**
   * Gets the default site template for the node, this must be a node that exist and is
   * readable by the current session.
   * 
   * @param site
   * @return the location of the default site template.
   */
  String getDefaultSiteTemplate(Node site);

  /**
   * Finds the site by doing a query for the sitename. Returns null if nothing is found.
   * 
   * @param siteName
   * @return The Node that resembles the site or null if nothing is found.
   */
  public Node findSiteByName(Session session, String siteName) throws SiteException;

}
