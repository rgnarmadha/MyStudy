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
package org.sakaiproject.nakamura.site;

import static org.sakaiproject.nakamura.api.site.SiteConstants.AUTHORIZABLES_SITE_IS_MAINTAINER;
import static org.sakaiproject.nakamura.api.site.SiteConstants.AUTHORIZABLES_SITE_NODENAME;
import static org.sakaiproject.nakamura.api.site.SiteConstants.AUTHORIZABLES_SITE_NODENAME_SINGLE;
import static org.sakaiproject.nakamura.api.site.SiteConstants.AUTHORIZABLES_SITE_PRINCIPAL_NAME;
import static org.sakaiproject.nakamura.api.site.SiteConstants.JOINABLE;
import static org.sakaiproject.nakamura.api.site.SiteConstants.SAKAI_IS_SITE_TEMPLATE;
import static org.sakaiproject.nakamura.api.site.SiteConstants.SAKAI_SITE_TEMPLATE;
import static org.sakaiproject.nakamura.api.site.SiteConstants.SAKAI_SKIN;
import static org.sakaiproject.nakamura.api.site.SiteConstants.SITES;
import static org.sakaiproject.nakamura.api.site.SiteConstants.SITE_RESOURCE_TYPE;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.nakamura.api.site.AuthorizableKey;
import org.sakaiproject.nakamura.api.site.GroupKey;
import org.sakaiproject.nakamura.api.site.SiteException;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.site.Sort;
import org.sakaiproject.nakamura.api.site.UserKey;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletResponse;

@Component(immediate = true, enabled = true)
@Service()
public class SiteServiceImpl implements SiteService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SiteServiceImpl.class);

  @org.apache.felix.scr.annotations.Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @org.apache.felix.scr.annotations.Property(value = "Provides a site service to manage sites.")
  static final String SERVICE_DESCRIPTION = "service.description";

  /**
   * The default site template, used when none has been defined.
   */
  public static final String DEFAULT_SITE = "/sites/default.html";

  /**
   * The maximum size of any list before we truncate. The user is warned.
   */
  public static final int MAXLISTSIZE = 10000;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.site.SiteService#isSite(javax.jcr.Item)
   */
  public boolean isSite(Item site) {
    try {
      if (site instanceof Node) {
        Node n = (Node) site;
        if (n.hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY)
            && SITE_RESOURCE_TYPE.equals(n.getProperty(
                JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY).getString())) {
          return true;
        }
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.site.SiteService#isSiteTemplate(javax.jcr.Item)
   */
  public boolean isSiteTemplate(Item site) {
    try {
      if (site instanceof Node) {
        Node n = (Node) site;
        if (n.hasProperty(SAKAI_IS_SITE_TEMPLATE)) {
          return n.getProperty(SAKAI_IS_SITE_TEMPLATE).getBoolean();
        }
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
      return false;
    }
    return false;
  }

  /**
   * @param site
   * @return true if the site is joinable
   * @throws RepositoryException
   * @throws PathNotFoundException
   * @throws ValueFormatException
   */
  public Joinable getJoinable(Node site) {
    try {
      if (site.hasProperty(JOINABLE)) {
        try {
          return Joinable.valueOf(site.getProperty(JOINABLE).getString());
        } catch (IllegalArgumentException e) {
        }
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return Joinable.no;
  }

  /**
   * @param site
   * @return true if the authz group is joinable
   * @throws RepositoryException
   * @throws PathNotFoundException
   * @throws ValueFormatException
   */
  public Joinable getJoinable(Authorizable authorizable) {
    try {
      if (authorizable instanceof Group && authorizable.hasProperty(JOINABLE)) {
        try {
          Value[] joinable = authorizable.getProperty(JOINABLE);
          if (joinable != null && joinable.length > 0)
            return Joinable.valueOf(joinable[0].getString());
        } catch (IllegalArgumentException e) {
        }
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return Joinable.no;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.site.SiteService#getSiteTemplate(javax.jcr.Node)
   */
  public String getSiteTemplate(Node site) {
    try {
      if (site.hasProperty(SAKAI_SITE_TEMPLATE)) {
        return site.getProperty(SAKAI_SITE_TEMPLATE).getString();
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return DEFAULT_SITE;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.site.SiteService#getSiteSkin(javax.jcr.Node)
   */
  public String getSiteSkin(Node site) throws SiteException {
    try {
      if (site.hasProperty(SAKAI_SKIN)) {
        return site.getProperty(SAKAI_SKIN).getString();
      }
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return DEFAULT_SITE;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.site.SiteService#getDefaultSiteTemplate(javax.jcr.Node)
   */
  public String getDefaultSiteTemplate(Node site) {
    // we should probably test that this node exists, but since this is a hard config,
    // then its probably not worth doing it.
    return DEFAULT_SITE;
  }

  /**
   * Unwraps an Iterator of GroupKeys to an Iterator of Group authorizables
   * 
   * @param underlying
   * @return
   */
  public Iterator<Group> unwrapGroups(final Iterator<GroupKey> underlying) {
    return new Iterator<Group>() {

      public boolean hasNext() {
        return underlying.hasNext();
      }

      public Group next() {
        return underlying.next().getGroup();
      }

      public void remove() {
        underlying.remove();
      }
    };
  }

  /**
   * Sets a list at the correct starting point, limits the result and does sorting (if
   * required).
   * 
   * @param memberList
   *          The list that needs to be manipulated.
   * @param start
   *          The starting point
   * @param nitems
   *          The number of items that should be in the collection.
   * @param sort
   *          The requested sorting, can be null.
   * @return
   */
  protected AbstractCollection<AuthorizableKey> returnCollection(
      final List<AuthorizableKey> memberList, final int start, final int nitems,
      Sort[] sort) {
    Iterator<AuthorizableKey> listIterator = null;
    // Check if we need to sort the list.
    if (sort != null && sort.length > 0) {
      Comparator<AuthorizableKey> comparitor = buildCompoundComparitor(sort);
      Collections.sort(memberList, comparitor);
      listIterator = memberList.listIterator(start);
    }

    // Start at the requested point in the list.
    listIterator = memberList.listIterator(start);

    // -1 means that we should fetch them all.
    if (nitems > 0) {
      listIterator = Iterators.limit(listIterator, nitems);
    }

    final Iterator<AuthorizableKey> iterator = listIterator;
    return new AbstractCollection<AuthorizableKey>() {

      @Override
      public Iterator<AuthorizableKey> iterator() {
        return iterator;
      }

      @Override
      public int size() {
        return memberList.size();
      }
    };
  }

  /**
   * Build a compound set of comparators for performing sorts.
   * 
   * @param sort
   *          the sort array to base the compound set
   * @return the first comparator in the set.
   */
  @SuppressWarnings("unchecked")
  protected <T extends AuthorizableKey> Comparator<T> buildCompoundComparitor(Sort[] sort) {
    if (sort.length == 0) {
      return null;
    }
    final Comparator<T>[] comparables = new Comparator[sort.length];
    int i = 0;
    for (final Sort s : sort) {

      final int next = i + 1;
      comparables[i++] = new Comparator<T>() {

        /**
         * Compare the objects
         */
        public int compare(T o1, T o2) {
          try {
            String c1 = o1.getAuthorizable().getID();
            String c2 = o2.getAuthorizable().getID();
            switch (s.getField()) {
            case firstName:
              c1 = o1.getFirstName();
              c2 = o2.getFirstName();
              break;
            case id:
              c1 = o1.getAuthorizable().getID();
              c2 = o2.getAuthorizable().getID();
              break;
            case lastName:
              c1 = o1.getLastName();
              c2 = o2.getLastName();
              break;
            }
            switch (s.getOrder()) {
            case asc:
              int i = c1.compareTo(c2);
              if (i == 0) {
                i = compareNext(o1, o2);
              }
              return i;
            case desc:
              i = c2.compareTo(c1);
              if (i == 0) {
                i = compareNext(o1, o2);
              }
              return i;
            }
          } catch (RepositoryException e) {
          }
          return 0;
        }

        /**
         * Chain to the next comparator in the ordering list.
         * 
         * @param o1
         *          the first object to compare.
         * @param o2
         *          the second object to compare.
         * @return the result of the next comparator in the chain or 0 if this is the last
         *         one.
         */
        private int compareNext(T o1, T o2) {
          if (next < comparables.length) {
            return comparables[next].compare(o1, o2);
          }
          return 0;
        }
      };
    }
    return comparables[0];
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.site.SiteService#getMembership(javax.jcr.Session,
   *      java.lang.String)
   */
  public Map<String, List<Group>> getMembership(Session session, String user)
      throws SiteException {
    try {
      Map<String, List<Group>> sites = Maps.newHashMap();
      UserManager userManager = AccessControlUtil.getUserManager(session);
      Authorizable a = userManager.getAuthorizable(user);
      if (a instanceof User) {
        User u = (User) a;
        for (Iterator<Group> igroup = u.memberOf(); igroup.hasNext();) {
          Group group = igroup.next();
          if (group.hasProperty(SITES)) {
            Value[] siteReferences = group.getProperty(SITES);
            for (Value v : siteReferences) {
              List<Group> g = sites.get(v.getString());
              if (g == null) {
                g = Lists.newArrayList();
                sites.put(v.getString(), g);
              }
              g.add(group);
            }
          }
        }
      }
      return sites;
    } catch (RepositoryException e) {
      throw new SiteException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e
          .getMessage());
    }
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.site.SiteService#findSiteByName(javax.jcr.Session,
   *      java.lang.String)
   */
  public Node findSiteByName(Session session, String siteName) throws SiteException {
    try {
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      String queryString = "//*[@sling:resourceType=\"" + SITE_RESOURCE_TYPE
          + "\" and jcr:contains(.,\"" + siteName + "\")]";
      Query query = queryManager.createQuery(queryString, Query.XPATH);
      QueryResult result = query.execute();

      NodeIterator nodeIterator = result.getNodes();
      if (nodeIterator.getSize() == 0) {
        return null;
      }

      while (nodeIterator.hasNext()) {
        Node siteNode = nodeIterator.nextNode();
        if (isSite(siteNode)) {
          return siteNode;
        }
      }

    } catch (RepositoryException e) {
      LOGGER.warn("Unable to retrieve site: {}", e.getMessage());
    }

    LOGGER.info("No site found for {}", siteName);
    return null;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.site.SiteService#getMemberCount(javax.jcr.Node)
   */
  public int getMemberCount(Node site) throws SiteException {
    return getMembers(site, 0, -1, null, -1).size();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.site.SiteService#isMember(javax.jcr.Node,
   *      org.apache.jackrabbit.api.security.user.Authorizable)
   */
  public boolean isMember(Node site, Authorizable target) {
    try {
      List<Authorizable> authorizables = getSiteAuthorizables(site, false);
      return isMember(target, authorizables);
    } catch (Exception e) {
      LOGGER.warn("Could not check if the authorizable {} is a member of this site.",
          target);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.site.SiteService#isUserSiteMaintainer(javax.jcr.Node)
   */
  public boolean isUserSiteMaintainer(Node site) throws RepositoryException {
    // TODO Maybe just check JCR_ALL or JCR_WRITE permission on the site node?
    Session session = site.getSession();
    UserManager um = AccessControlUtil.getUserManager(session);
    Authorizable target = um.getAuthorizable(session.getUserID());
    List<Authorizable> authorizables = getSiteAuthorizables(site, true);
    return isMember(target, authorizables);
  }

  /**
   * Retrieves a list of {@link Authorizable authorizables} for a certain site.
   * 
   * @param site
   *          The node that represents the site (top level node)
   * @param justMaintainers
   *          true only retrieves those authorizables that are marked as site maintainers.
   * @return
   * @throws RepositoryException
   */
  protected List<Authorizable> getSiteAuthorizables(Node site, boolean justMaintainers)
      throws RepositoryException {
    Session session = site.getSession();
    UserManager um = AccessControlUtil.getUserManager(session);
    Node authorizableNodes = site.getNode(AUTHORIZABLES_SITE_NODENAME);
    NodeIterator authorizableIt = authorizableNodes
        .getNodes(AUTHORIZABLES_SITE_NODENAME_SINGLE + "*");
    List<Authorizable> authorizables = new ArrayList<Authorizable>();
    while (authorizableIt.hasNext()) {
      // Each one of these nodes will either represent a Group or a User
      Node auNode = authorizableIt.nextNode();
      String auName = auNode.getProperty(AUTHORIZABLES_SITE_PRINCIPAL_NAME).getString();

      // If we only need the maintainers we look at the property on the node to see if
      // this authorizable represents a maintainer.
      if (justMaintainers) {
        if (!auNode.hasProperty(AUTHORIZABLES_SITE_IS_MAINTAINER)) {
          continue;
        }
        if (!auNode.getProperty(AUTHORIZABLES_SITE_IS_MAINTAINER).getBoolean()) {
          continue;
        }
      }

      // Get the authorizable
      Authorizable a = um.getAuthorizable(auName);

      authorizables.add(a);
    }

    return authorizables;
  }

  /**
   * Checks if the target authorizable is in the list of authorizables (or member of one
   * of the Group authorizables).
   * 
   * @param target
   *          The authorizable to test.
   * @param authorizables
   *          The List of authorizables to test the target on.
   * @return Will return true if the authorizable is in or member of the list, false
   *         otherwise.
   * @throws RepositoryException
   */
  private boolean isMember(Authorizable target, List<Authorizable> authorizables)
      throws RepositoryException {
    // We loop over the list of authorizables twice.
    // Once to check if the target is in the list.
    // If it hasn't been found by then, we loop over it again and check if the target is
    // a member of one the group authorizables.
    for (Authorizable a : authorizables) {
      // Authorizables implement the necessary equals method to check for equality.
      if (a.equals(target)) {
        return true;
      }
    }

    // The authorizable hasn't been found yet, we check each group authorizable for
    // membership of the target.
    // We don't have to worry about subgroups as these will be checked for us.
    // This check is obviously more expensive.
    for (Authorizable a : authorizables) {
      if (a instanceof Group) {
        if (((Group) a).isMember(target)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.site.SiteService#getGroups(javax.jcr.Node, int, int,
   *      org.sakaiproject.nakamura.api.site.Sort[])
   */
  public AbstractCollection<AuthorizableKey> getGroups(Node site, int start, int nitems,
      Sort[] sort) throws SiteException {
    return getGroups(site, start, nitems, sort, -1);
  }

  /**
   * @param site
   * @param start
   * @param nitems
   * @param sort
   * @param i
   * @return
   */
  public AbstractCollection<AuthorizableKey> getGroups(Node site, int start, int nitems,
      Sort[] sort, int maxLevels) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.site.SiteService#getMembers(javax.jcr.Node, int,
   *      int, org.sakaiproject.nakamura.api.site.Sort[])
   */
  public AbstractCollection<AuthorizableKey> getMembers(Node site, int start, int nitems,
      Sort[] sort) throws SiteException {
    return getMembers(site, start, nitems, sort, -1);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.site.SiteService#getMembers(javax.jcr.Node, int,
   *      int, org.sakaiproject.nakamura.api.site.Sort[], int)
   */
  public AbstractCollection<AuthorizableKey> getMembers(Node site, int start, int nitems,
      Sort[] sort, int maxLevels) throws SiteException {
    try {
      List<AuthorizableKey> memberList = new ArrayList<AuthorizableKey>();

      // Get the main user/groups that are attached to this site.
      List<Authorizable> siteAuthorizables = getSiteAuthorizables(site, false);

      // Add them (and their children if required) to the list.
      processAuthorizables(siteAuthorizables.iterator(), memberList, true, 0, maxLevels,
          null);

      return returnCollection(memberList, start, nitems, sort);
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new SiteException(500, "Could not retrieve the members for this site.", e);
    }
  }

  /**
   * Returns a tree of members for a site. These can be both groups and users.
   * 
   * 
   * @param site
   *          The node that represents the site.
   * @param start
   * @param nitems
   * @param sort
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
      int nitems, Sort[] sort, int maxLevels) throws SiteException {
    try {
      List<AuthorizableKey> memberList = new ArrayList<AuthorizableKey>();

      // Get the main user/groups that are attached to this site.
      List<Authorizable> siteAuthorizables = getSiteAuthorizables(site, false);

      // Add them (and their children if required) to the list.
      processAuthorizables(siteAuthorizables.iterator(), memberList, false, 0, maxLevels,
          null);

      return returnCollection(memberList, start, nitems, sort);
    } catch (RepositoryException e) {
      LOGGER.warn(e.getMessage(), e);
      throw new SiteException(500, "Could not retrieve the site members in tree format.",
          e);
    }
  }

  /**
   * @param iterator
   * @param memberList
   * @param b
   * @param i
   * @param maxLevels
   * @throws RepositoryException
   */
  protected void processAuthorizables(Iterator<Authorizable> iterator,
      List<AuthorizableKey> memberList, boolean isFlat, int currentLevel, int maxLevels,
      GroupKey groupKey) throws RepositoryException {
    while (iterator.hasNext()) {
      Authorizable au = iterator.next();
      AuthorizableKey key = null;
      // Handle groups
      if (au instanceof Group) {
        Group g = (Group) au;
        key = new GroupKey(g);
        populateMembers((GroupKey) key, g, memberList, isFlat, 0, maxLevels);
      }

      // Add the user.
      else if (au instanceof User) {
        key = new UserKey((User) au);
      }

      if (isFlat) {
        if (key instanceof UserKey && !memberList.contains(key)) {
          // We requested a flat list of members
          // Since it is flat, we only add users.
          memberList.add(key);
        }
      } else {
        if (groupKey != null) {
          groupKey.getChildren().add(key);
        }
        if (currentLevel == 0 && !memberList.contains(key)) {
          memberList.add(key);
        }
      }
    }
  }

  /**
   * Populates the groupKey with the members of a group. All it really does, is check if
   * we can proceed a level deeper and does a new recursive call.
   * 
   * @param groupKey
   *          The groupkey for the provided group
   * @param group
   *          The group that should be "resolved".
   * @param memberList
   *          The list of members. If isFlat is set to true, this will contain ALL the
   *          members, regardless of the current level.
   * @param isFlat
   * @param currentLevel
   *          The current level of hierarchy
   * @param maxLevels
   *          The maximum levels of groups that should be resolved.
   * @return
   * @throws RepositoryException
   */
  protected void populateMembers(GroupKey groupKey, Group group,
      List<AuthorizableKey> memberList, boolean isFlat, int currentLevel, int maxLevels)
      throws RepositoryException {
    currentLevel++;
    if (checkLevel(maxLevels, currentLevel)) {
      // We can resolve this group's members.
      Iterator<Authorizable> members = group.getDeclaredMembers();
      processAuthorizables(members, memberList, isFlat, currentLevel, maxLevels, groupKey);
    }
  }

  /**
   * @param maxLevels
   *          The levels that should be resolved.
   * @param currentLevel
   *          The level that the implementation is on right now.
   * @return true if the implementation should proceed with resolving group members, false
   *         otherwise.
   */
  protected boolean checkLevel(int maxLevels, int currentLevel) {
    return (maxLevels == -1 || currentLevel <= maxLevels);
  }

}
