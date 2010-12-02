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
import org.apache.jackrabbit.api.security.user.User;

import java.util.AbstractCollection;
import java.util.Iterator;
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
   * The property name of the site template.
   */
  public static final String SAKAI_SITE_TEMPLATE = "sakai:site-template";
  /**
   * The property name for the skin of a website.
   */
  public static final String SAKAI_SKIN = "sakai:skin";
  /**
   * The sling resource type for a site.
   */
  public static final String SITE_RESOURCE_TYPE = "sakai/site";
  /**
   * The property name that defines if this is a site template or not.
   */
  public static final String SAKAI_IS_SITE_TEMPLATE = "sakai:is-site-template";
  /**
   * The property name for the name of a site.
   */
  public static final String SAKAI_SITE_NAME = "sakai:site-name";
  /**
   * The property used to store the joinable status of the site.
   */
  public static final String JOINABLE = "sakai:joinable";

   /**
   * The property used to store the type of site.(e.g. portfolio, course, other)
   */
  public static final String SAKAI_SITE_TYPE = "sakai:site-type";

  /**
   * The property used to store the sites a group is associated with.
   */
  public static final String SITES = "sakai:site";
  /**
   * The property returned with the groups info in the members listing
   * (this is not and should not actually be stored on the node,
   * it is generated on demand)
   */
  public static final String MEMBER_GROUPS = "member:groups";

  /**
   * The multivalued property to store the list of associated authorizables.
   */
  public static final String AUTHORIZABLE = "sakai:authorizables";
  /**
   * The request parameter indicating the target group for join and unjoin operations.
   */
  public static final String PARAM_GROUP = "targetGroup";
  public static final String PARAM_START = "start";
  public static final String PARAM_ITEMS = "items";
  public static final String PARAM_SORT = "sort";
  public static final String PARAM_ADD_GROUP = "addauth";
  public static final String PARAM_REMOVE_GROUP = "removeauth";

  /**
   * Request parameters used when creating a site.
   */
  public static final String PARAM_SITE_PATH = ":sitepath";
  public static final String PARAM_MOVE_FROM = ":moveFrom";
  public static final String PARAM_COPY_FROM = ":copyFrom";
  public static final String SITES_CONTAINER_RESOURCE_TYPE = "sakai/sites";

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
     * The site is joinable with approval.
     */
    withauth();
  }

  /**
   * An Event Enumeration for all the events that the Site Service might emit.
   */
  public enum SiteEvent {
    /**
     * Event posted to indicate a site has been created
     */
    created(),
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
     * One of the maintainers of the request group
     */
    public static final String OWNER = "owner";

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
   * Join a site in a target Group. The target Group must already be associated with the
   * site and the Site and the group must both be joinable. THe target group does not need
   * to be a directly associated with the site.
   * 
   * @param site
   *          the site to join
   * @param targetGroup
   *          the target Group to join
   * @throws SiteException
   *           if its not possible to fulfill the request.
   */
  void joinSite(Node site, String targetGroup) throws SiteException;

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
   * Initiate a join workflow for the site and the user. The user is taken from the
   * current request.
   * 
   * @param site
   *          the site in question
   * @param group
   *          the group being joined.
   * @throws SiteException
   *           thrown if there was a problem initiating the join workflow.
   */
  void startJoinWorkflow(Node site, Group group, Authorizable owner) throws SiteException;

  /**
   * @param site
   *          the site in question.
   * @return the path to the template-site where the given site is based on.
   */
  String getSiteTemplate(Node site) throws SiteException;

  /**
   *
   * @param site the site in question
   * @return the path to the template associated with the site, may be the default site
   *         template if none is specified.
   * @throws SiteException
   */
  String getSiteSkin(Node site) throws SiteException;

  /**
   * Unjoin a site, only if the user is a member of the group and the group is associated
   * with the site. The user must also be a member of the group.
   * 
   * @param site
   *          the site containing the group.
   * @param group
   *          the group to unjoin.
   */
  void unjoinSite(Node site, String string) throws SiteException;

  /**
   * Lists declared members of the site with a sort order and paging.
   *
   * @param site
   *          the Site node
   * @param start
   *          the first item of the iterator
   * @param nitems
   *          the number of items
   * @param sort
   *          an array of sort specifications, in order of preference.
   * @param sortOrder
   *          the sort order of each field.
   * @return An iterator of User elements representing the users of the the site, this
   *         does not include users that have membership of dynamic groups, or groups
   */
  AbstractCollection<User> getMembers(Node site, int start, int nitems, Sort[] sort);

  /**
   * Returns the number of declared members of a site
   *
   * @param site
   *          the Site node
   * @return The number of members
   */
  int getMemberCount(Node site);

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
  Iterator<Group> getGroups(Node site, int start, int nitems, Sort[] sort)
      throws SiteException;

  /**
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
   * @param siteName
   * @return The Node that resembles the site or null if nothing is found.
   */
  public Node findSiteByName(Session session, String siteName) throws SiteException;

  /**
   * Finds a site by giving it a path. This will travel upwards along the path and if
   * it finds a sakai/sites store will try to expand the path with the siteName.
   * the sitename is the part after the last slash (/)
   * @param session
   * @param uriPath
   * @return The Node that resembles the site or null if nothing is found.
   * @throws SiteException
   */
  public Node findSiteByURI(Session session, String uriPath) throws SiteException;

  /**
   * Create a site at a given path. This site can be based on a template (it doesn't have
   * to be.) You will have to call session.save() explicitly.
   * 
   * @param session
   *          A session that can create nodes at the specified sitePath.
   * @param creator
   *          The authorizable that requested this site.
   * @param sitePath
   *          The path where the site should be created.
   * @param templatePath
   *          [OPTIONAL] The path of the template that this site should be based on.
   * @param siteType
   *          The type of this site.
   * @return The top node of the new site.
   * @throws SiteException
   */
  public Node createSite(Session session, Authorizable creator, String sitePath,
      String templatePath, String sakaiSiteType) throws SiteException;

  /**
   * Copy a site from one location to another.
   * 
   * @param session
   *          The session that has write access on the destination.
   * @param creator
   *          The authorizable that represents the creator.
   * @param sitePath
   *          The destination where the site should be created.
   * @param fromPath
   *          The original site that should be copied.
   * @return The new node for the copied site.
   * @throws SiteException
   */
  public Node copySite(Session session, Authorizable creator, String sitePath,
      String fromPath) throws SiteException;

  /**
   * Move a site from one location to another
   * 
   * @param session
   *          The site that has write access on BOTH locations.
   * @param creator
   *          The authorizable that represents the creator.
   * @param sitePath
   *          The path where the site should be moved too.
   * @param fromPath
   *          The path where the original site is located.
   * @return The node that represents the new moved site.
   * @throws SiteException
   */
  public Node moveSite(Session session, Authorizable creator, String sitePath,
      String fromPath) throws SiteException;

}
