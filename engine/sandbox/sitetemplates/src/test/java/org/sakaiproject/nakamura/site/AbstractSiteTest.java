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

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.site.SiteConstants.AUTHORIZABLES_SITE_IS_MAINTAINER;
import static org.sakaiproject.nakamura.api.site.SiteConstants.AUTHORIZABLES_SITE_NODENAME;
import static org.sakaiproject.nakamura.api.site.SiteConstants.AUTHORIZABLES_SITE_NODENAME_SINGLE;
import static org.sakaiproject.nakamura.api.site.SiteConstants.AUTHORIZABLES_SITE_PRINCIPAL_NAME;
import static org.sakaiproject.nakamura.api.site.SiteConstants.RT_SITE_AUTHORIZABLE;
import static org.sakaiproject.nakamura.api.site.SiteConstants.SITE_RESOURCE_TYPE;

import junit.framework.TestCase;

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic.RepositoryBase;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;
import org.sakaiproject.nakamura.api.profile.ProfileConstants;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PathUtils;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 * Sets up a repository and creates some nodes.
 */
public class AbstractSiteTest extends TestCase {

  private static final String DEFAULT_PASSWORD = "test";

  private static BundleContext bundleContext;
  private static RepositoryBase repositoryBase;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    // When we start up a class, we create a repository.
    if (repositoryBase == null) {
      bundleContext = Mockito.mock(BundleContext.class);
      repositoryBase = new RepositoryBase(bundleContext);
      repositoryBase.start();
      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

        public void run() {
          if (repositoryBase != null) {
            repositoryBase.stop();
            repositoryBase = null;
          }
        }
      }));
    }
  }

  public void testFoo() {

  }

  public static Repository getRepository() throws IOException, RepositoryException {
    return repositoryBase.getRepository();
  }

  /**
   * @return Returns a new admin session.
   * @throws IOException
   * @throws RepositoryException
   * @throws LoginException
   */
  public static Session loginAdministrative() throws LoginException, RepositoryException,
      IOException {
    return getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
  }

  /**
   * Login as a user
   * 
   * @param user
   * @return
   * @throws LoginException
   * @throws RepositoryException
   * @throws IOException
   */
  public Session login(User user) throws LoginException, RepositoryException, IOException {
    return getRepository().login(
        new SimpleCredentials(user.getID(), DEFAULT_PASSWORD.toCharArray()));
  }

  /**
   * Login as a user
   * 
   * @param user
   * @param password
   * @return
   * @throws LoginException
   * @throws RepositoryException
   * @throws IOException
   */
  public Session login(User user, String password) throws LoginException,
      RepositoryException, IOException {
    return getRepository().login(
        new SimpleCredentials(user.getID(), password.toCharArray()));
  }

  /**
   * Create a user.
   * 
   * @param session
   *          A session to create a user with.
   * @param userID
   *          The name of the user
   * @return The user object that represents the new user.
   * @throws RepositoryException
   * @throws IOException
   */
  public static User createUser(Session session, String userID)
      throws RepositoryException, IOException {
    return createUser(session, userID, DEFAULT_PASSWORD);
  }

  /**
   * Create a user.
   * 
   * @param session
   *          A session to create a user with.
   * @param userID
   *          The name of the user
   * @param password
   *          The password of the user
   * @return The user object that represents the new user.
   * @throws RepositoryException
   * @throws IOException
   */
  public static User createUser(Session session, String userID, String password)
      throws RepositoryException, IOException {
    UserManager userManager = AccessControlUtil.getUserManager(session);
    User user = userManager.createUser(userID, password);
    ItemBasedPrincipal p = (ItemBasedPrincipal) user.getPrincipal();
    ValueFactory vf = session.getValueFactory();
    user.setProperty("path", vf.createValue(p.getPath().substring(
        UserConstants.USER_REPO_LOCATION.length())));
    // Create profile
    createProfile(session, user, userID);
    return user;
  }

  /**
   * 
   * @param session
   * @param groupName
   * @return
   * @throws RepositoryException
   */
  public static Group createGroup(Session session, final String groupName)
      throws RepositoryException {
    UserManager userManager = AccessControlUtil.getUserManager(session);
    Principal principal = new Principal() {

      public String getName() {
        return groupName;
      }
    };
    Group group = userManager.createGroup(principal);
    ItemBasedPrincipal p = (ItemBasedPrincipal) group.getPrincipal();
    ValueFactory vf = session.getValueFactory();
    group.setProperty("path", vf.createValue(p.getPath().substring(
        UserConstants.GROUP_REPO_LOCATION.length())));

    // Create profile
    createProfile(session, group, groupName);

    return group;
  }

  private static void createProfile(Session session, Authorizable au, String name)
      throws RepositoryException {
    ValueFactory vf = session.getValueFactory();
    Value firstName = vf.createValue(name);
    Value lastName = vf.createValue(name);
    String path = PathUtils.normalizePath(ProfileConstants.USER_JCR_PATH_PREFIX+PathUtils.getSubPath(au)+"/public/authprofile");
    Node profile = JcrUtils.deepGetOrCreateNode(session, path);
    profile.setProperty("name", name);
    profile.setProperty("firstName", firstName);
    profile.setProperty("lastName", lastName);
    au.setProperty("firstName", firstName);
    au.setProperty("lastName", lastName);
  }

  /**
   * 
   * @param session
   * @param groupName
   * @param members
   * @return
   * @throws RepositoryException
   */
  public static Group createGroup(Session session, final String groupName,
      List<Authorizable> members) throws RepositoryException {
    Group g = createGroup(session, groupName);
    for (Authorizable a : members) {
      g.addMember(a);
    }
    return g;
  }

  /**
   * Creates a site node.
   * 
   * @param string
   * @return
   * @throws RepositoryException
   */
  public Node createSite(Session session, String path) throws RepositoryException {
    Node siteNode = JcrUtils.deepGetOrCreateNode(session, path);
    siteNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, SITE_RESOURCE_TYPE);
    return siteNode;
  }

  /**
   * Sets a couple of managers on a site.
   * 
   * @param siteNode
   * @param managers
   * @throws RepositoryException
   */
  public void setManagers(Node siteNode, List<Authorizable> managers)
      throws RepositoryException {
    for (Authorizable manager : managers) {
      addAuthorizable(siteNode, manager, true);
    }
  }

  public void addAuthorizable(Node siteNode, Authorizable authorizable,
      boolean isMaintainer) throws RepositoryException {
    Node groupNodes = org.apache.jackrabbit.commons.JcrUtils.getOrAddNode(siteNode,
        AUTHORIZABLES_SITE_NODENAME);
    // Not the most performant mechanism..
    long size = groupNodes.getNodes().getSize();
    long i = size + 1;
    Node groupNode = org.apache.jackrabbit.commons.JcrUtils.getOrAddNode(groupNodes,
        AUTHORIZABLES_SITE_NODENAME_SINGLE + i);

    groupNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, RT_SITE_AUTHORIZABLE);
    groupNode.setProperty(AUTHORIZABLES_SITE_PRINCIPAL_NAME, authorizable.getPrincipal()
        .getName());
    groupNode.setProperty(AUTHORIZABLES_SITE_IS_MAINTAINER, isMaintainer);

    Session session = siteNode.getSession();
    if (session.hasPendingChanges()) {
      session.save();
    }
  }

  /**
   * @throws IOException
   * @throws RepositoryException
   * @throws LoginException
   * 
   */
  public static ACMEGroupStructure createAcmeStructure(String uniqueIdentifier)
      throws LoginException, RepositoryException, IOException {
    Session adminSession = loginAdministrative();
    ACMEGroupStructure acme = new ACMEGroupStructure();
    acme.acmeLabs = createGroup(adminSession, "acme-labs-" + uniqueIdentifier);
    acme.acmeManagers = createGroup(adminSession, "acme-labs-managers-"
        + uniqueIdentifier);
    acme.acmeDevelopers = createGroup(adminSession, "acme-labs-developers-"
        + uniqueIdentifier);
    acme.acmeQA = createGroup(adminSession, "acme-labs-qa-" + uniqueIdentifier);
    acme.acmeResearchers = createGroup(adminSession, "acme-labs-researchers-"
        + uniqueIdentifier);

    acme.userTopManager = createUser(adminSession, "acme-top-manager-" + uniqueIdentifier);
    acme.userDevelopersManager = createUser(adminSession, "acme-developers-manager-"
        + uniqueIdentifier);
    acme.userQAManager = createUser(adminSession, "acme-qa-manager-" + uniqueIdentifier);
    acme.userResearchManager = createUser(adminSession, "acme-research-manager-"
        + uniqueIdentifier);

    acme.userDeveloper = createUser(adminSession, "acme-developer-" + uniqueIdentifier);
    acme.userQA = createUser(adminSession, "acme-qa-" + uniqueIdentifier);
    acme.userResearch = createUser(adminSession, "acme-research-" + uniqueIdentifier);

    // Add the users to the groups
    acme.acmeManagers.addMember(acme.userTopManager);
    acme.acmeManagers.addMember(acme.userDevelopersManager);
    acme.acmeManagers.addMember(acme.userQAManager);
    acme.acmeManagers.addMember(acme.userResearchManager);

    acme.acmeDevelopers.addMember(acme.userDeveloper);
    acme.acmeResearchers.addMember(acme.userResearch);
    acme.acmeQA.addMember(acme.userQA);

    // Add them all the the main group.
    acme.acmeLabs.addMember(acme.acmeManagers);
    acme.acmeLabs.addMember(acme.userDeveloper);
    acme.acmeLabs.addMember(acme.userQA);
    acme.acmeLabs.addMember(acme.userResearch);

    if (adminSession.hasPendingChanges()) {
      adminSession.save();
    }

    return acme;
  }
}
