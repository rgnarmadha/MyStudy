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

import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

/**
 *
 */
public class SiteAuthz {
  private static final Logger LOGGER = LoggerFactory.getLogger(SiteAuthz.class);

  /**
   * Contains the path of the associated site. Write-only at present, but could be used to
   * guard against deleting non-site-specific groups in site deletion.
   */
  public static final String GROUP_SITE_PROPERTY = "sakai:site";

  /**
   * Contains the associated role. Write-only at present.
   */
  public static final String GROUP_ROLE_PROPERTY = "name";
  public static final String GROUP_JOINABLE_PROPERTY = "sakai:joinable";
  public static final String JOINABLE_WITH_APPROVAL = "withauth";
  public static final String JOINABLE = "yes";

  public static final String SITE_ROLES_PROPERTY = "sakai:roles";
  public static final String SITE_ROLE_MEMBERSHIPS_PROPERTY = "sakai:rolemembers";
  public static final String SITE_STATUS_PROPERTY = "status";
  public static final String SITE_ACCESS_TYPE_PROPERTY = "access";
  public static final Collection<String> MONITORED_SITE_PROPERTIES = Arrays
      .asList(new String[] { SITE_STATUS_PROPERTY, SITE_ACCESS_TYPE_PROPERTY });
  public static final String SITE_IS_USER_MAINTAINER_PROPERTY = ":isMaintainer";
  public static final String SITE_IS_USER_PENDING_APPROVAL = ":isPendingApproval";

  private static final String AUTHZ_CONFIG_PATH = "_authz.txt/jcr:content";
  private static final String[] SITE_MAINTENANCE_PRIVILEGES = new String[] {
    Privilege.JCR_MODIFY_PROPERTIES, Privilege.JCR_MODIFY_ACCESS_CONTROL, Privilege.JCR_REMOVE_NODE
  };
  private Node site;
  private String siteRef; // Saved in case of site deletion
  private JSONObject authzConfig;
  private Map<String, String> roleToGroupMap;

  private AuthorizablePostProcessService postProcessService;

  /**
   * @param site
   * @throws RepositoryException
   */
  public SiteAuthz(Node site, AuthorizablePostProcessService postProcesService) throws RepositoryException {
    this.postProcessService = postProcesService;
    this.site = site;
    this.siteRef = site.getIdentifier();
    this.roleToGroupMap = new HashMap<String, String>();
    if (site.hasProperty(SITE_ROLES_PROPERTY)
        && site.hasProperty(SITE_ROLE_MEMBERSHIPS_PROPERTY)) {
      Value[] roleValues = site.getProperty(SITE_ROLES_PROPERTY).getValues();
      Value[] groupValues = site.getProperty(SITE_ROLE_MEMBERSHIPS_PROPERTY).getValues();
      if (roleValues.length == groupValues.length) {
        for (int i = 0; i < roleValues.length; i++) {
          this.roleToGroupMap.put(roleValues[i].getString(), groupValues[i].getString());
        }
      } else {
        LOGGER.warn("Site " + site.getPath()
            + " has mismatched roles and groups properties");
      }
    }
  }

  /**
   * @param site
   * @return a JSON object describing the current configuration of site roles and access
   * schemes, or an empty object if no configuration exists
   * @throws RepositoryException
   */
  private static JSONObject findSiteAuthzConfig(Node site) throws RepositoryException {
    JSONObject authzConfig = null;
    if (site.hasNode(AUTHZ_CONFIG_PATH)) {
      Node node = site.getNode(AUTHZ_CONFIG_PATH);
      Property data = node.getProperty("jcr:data");
      String configString = data.getString();
      try {
        authzConfig = new JSONObject(configString);
      } catch (JSONException e) {
        LOGGER.warn("Could not parse " + configString, e);
      }
    }
    if (authzConfig == null) {
      authzConfig = new JSONObject();
    }
    return authzConfig;
  }

  /**
   * Lazy-load the configuration.
   */
  private JSONObject getAuthzConfig() throws RepositoryException {
    if (authzConfig == null) {
      authzConfig = findSiteAuthzConfig(site);
    }
    return authzConfig;
  }

  /**
   * Get the default role for "maintainers" of the site, notably including
   * the site creator.
   * @return null if no maintainer role defined
   * @throws RepositoryException
   */
  public String getMaintenanceRole() throws RepositoryException {
    try {
      return getAuthzConfig().getString("maintenanceRole");
    } catch (JSONException e) {
      return null;
    }
  }

  /**
   * Apply the current roles and access schemes configuration to a new site.
   *
   * @param creatorId the user creating the site (needed since node creation
   * usually occurs in an administrative session)
   * @throws RepositoryException
   */
  public void initAccess(String creatorId) throws RepositoryException {
    // Generate JCR groups to hold the site role membership lists.
    roleToGroupMap = initRoleMembershipGroups(creatorId);

    // Add the role and group properties to the site node.
    List<String> roles = new ArrayList<String>(roleToGroupMap.keySet());
    List<String> membershipGroups = new ArrayList<String>(roles.size());
    for (String role : roles) {
      membershipGroups.add(roleToGroupMap.get(role));
    }
    JcrResourceUtil.setProperty(site, SITE_ROLES_PROPERTY, roles.toArray(new String[roles
        .size()]));
    JcrResourceUtil.setProperty(site, SITE_ROLE_MEMBERSHIPS_PROPERTY, membershipGroups
        .toArray(new String[membershipGroups.size()]));

    // Also add the membership groups to the "sakai:authorizables" property.
    // TODO Is this actually needed?
    JcrResourceUtil.setProperty(site, SiteService.AUTHORIZABLE, membershipGroups
        .toArray(new String[membershipGroups.size()]));

    // Apply default access scheme to site node.
    JSONObject defaultProperties = getAuthzConfig().optJSONObject("defaultProperties");
    if (defaultProperties != null) {
      try {
        String statusType = defaultProperties.getString("status");
        String accessType = defaultProperties.getString("access");
        JcrResourceUtil.setProperty(site, SITE_STATUS_PROPERTY, statusType);
        JcrResourceUtil.setProperty(site, SITE_ACCESS_TYPE_PROPERTY, accessType);
      } catch (JSONException e) {
        LOGGER.error("Bad site authz config for site " + site.getPath(), e);
      }
      applyAuthzChanges();
    } else {
      applyStandardAccessRules();
    }
  }

  /**
   * Use the site authorization configuration to put the site's currently selected access
   * scheme (e.g., "Offline" or "Members Only") into effect.
   * @throws RepositoryException
   */
  public void applyAuthzChanges() throws RepositoryException {
    // Get the current settings.
    String statusType = null;
    String accessType = null;
    if (site.hasProperty(SITE_STATUS_PROPERTY)) {
      statusType = site.getProperty(SITE_STATUS_PROPERTY).getString();
    } else {
      LOGGER.debug("Site node missing status property; skipping authz settings");
      return;
    }
    if (site.hasProperty(SITE_ACCESS_TYPE_PROPERTY)) {
      accessType = site.getProperty(SITE_ACCESS_TYPE_PROPERTY).getString();
    }

    // Now try to apply them.
    JSONObject accessSchemes = getAuthzConfig().optJSONObject("accessSchemes");
    if (accessSchemes != null) {
      // A status type will either override the access type or defer to the access type.
      try {
        if (!applyAccessScheme(statusType, accessSchemes)) {
          if (accessType != null) {
            applyAccessScheme(accessType, accessSchemes);
          } else {
            LOGGER.warn("Site node missing access property; skipping authz settings");
            return;
          }
        }
      } catch (JSONException e) {
        LOGGER.error("Bad site authz config for site " + site.getPath(), e);
      }
    }

    // Re-apply standard ACLs to site node to position them as most recent.
    // Otherwise they might be functionally overwritten due to ordered
    // ACL provision.
    applyStandardAccessRules();
  }

  private boolean applyAccessScheme(String accessSchemeName, JSONObject accessSchemes)
      throws RepositoryException, JSONException {
    boolean isAuthzChanged = false;
    JSONObject accessScheme = accessSchemes.optJSONObject(accessSchemeName);
    if (accessScheme == null) {
      LOGGER.warn("Site authz configuration missing specified access scheme "
          + accessSchemeName);
      return false;
    }
    JSONArray aceModifications = accessScheme.optJSONArray("aceModifications");
    if (aceModifications != null) {
      for (int i = 0; i < aceModifications.length(); i++) {
        applyAceModification(aceModifications.getJSONObject(i));
      }
      isAuthzChanged = true;
    }
    return isAuthzChanged;
  }


  private boolean applyAceModification(JSONObject aceModification)
      throws RepositoryException {
    boolean isAuthzChanged = false;
    String principalId = aceModification.optString("principal");
    if (principalId.length() == 0) {
      String role = aceModification.optString("role");
      if (role.length() != 0) {
        principalId = roleToGroupMap.get(role);
      }
    }
    if (principalId.length() != 0) {
      Session session = site.getSession();
      PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
      Principal principal = principalManager.getPrincipal(principalId);
      List<String> grants = new ArrayList<String>();
      List<String> denies = new ArrayList<String>();
      JSONObject aces = aceModification.optJSONObject("aces");
      if (aces != null) {
        Iterator<String> privileges = aces.keys();
        while (privileges.hasNext()) {
          String privilege = privileges.next();
          String grantedOrDenied = aces.optString(privilege);
          if ("granted".equals(grantedOrDenied)) {
            grants.add(privilege);
          } else if ("denied".equals(grantedOrDenied)) {
            denies.add(privilege);
          }
        }
      }
      // Start with a clean slate for this principal.
      String[] removes = {"jcr:all"};
      AccessControlUtil.replaceAccessControlEntry(site.getSession(), site.getPath(), principal,
          grants.toArray(new String[grants.size()]), denies.toArray(new String[denies.size()]), removes, null);
      isAuthzChanged = true;
    }
    return isAuthzChanged;
  }

  private void applyStandardAccessRules() throws RepositoryException {
    try {
      JSONArray standardAces = getAuthzConfig().optJSONArray("standardAces");
      if (standardAces != null) {
        for (int i = 0; i < standardAces.length(); i++) {
          applyAceModification(standardAces.getJSONObject(i));
        }
      }
    } catch (JSONException e) {
      LOGGER.error("Bad site authz config for site " + site.getPath(), e);
    }
  }

  /**
   * TODO This is copied from org.sakaiproject.nakamura.user servlet code which should
   * instead be refactored into a shared service. See KERN-580.
   */
  private Group createGroup(Session session, String groupId, String creatorId)
      throws RepositoryException {
    UserManager userManager = AccessControlUtil.getUserManager(session);
    Authorizable authorizable = userManager.getAuthorizable(groupId);
    if (authorizable != null) {
      // TODO Work around any duplicates, since sites code doesn't monitor
      // group creation in general.
      throw new RepositoryException(
          "A principal already exists with the requested name: " + groupId);
    }
    final String principalName = groupId;
    Group group = userManager.createGroup(new Principal() {
      public String getName() {
        return principalName;
      }
    });

    try {
      postProcessService.process(group, session, ModificationType.CREATE);
    } catch (Exception e) {
      throw new RepositoryException(e.getMessage(),e);
    }

    return group;
  }

  private Map<String, String> initRoleMembershipGroups(String creatorId)
      throws RepositoryException {
    Map<String, String> roleToGroupMap = new HashMap<String, String>();
    JSONArray roles = getAuthzConfig().optJSONArray("roles");
    if (roles != null) {
      // Site names exist in a hierarchical namespace but group names exist
      // in a flat namespace. To lessen the chance of conflicts, use the
      // node UUID to generate site-dependent group names.
      String siteId = site.getIdentifier();
      Session session = site.getSession();
      ValueFactory valueFactory = session.getValueFactory();
      String maintenanceRole = getMaintenanceRole();
      Collection<Value> groupAdministrators = new ArrayList<Value>();
      groupAdministrators.add(valueFactory.createValue(creatorId));
      Collection<Group> membershipGroups = new ArrayList<Group>();

      // Create groups to hold site role memberships.
      try {
        JSONObject roleToGroupPattern = getAuthzConfig()
            .getJSONObject("roleToGroupPattern");
        for (int i = 0; i < roles.length(); i++) {
          String role = roles.getString(i);

          // Create JCR group to hold role memberships.
          String groupId = roleToGroupPattern.getString(role);
          groupId = groupId.replace("${siteId}", siteId);
          Group group = createGroup(session, groupId, creatorId);

          // Add site-related properties to the group.
          group.setProperty(GROUP_SITE_PROPERTY, valueFactory.createValue(siteRef));
          group.setProperty(GROUP_ROLE_PROPERTY, valueFactory.createValue(role));

          // Make groups joinable
          group.setProperty(GROUP_JOINABLE_PROPERTY, valueFactory.createValue(JOINABLE));

          // Remember the mapping.
          roleToGroupMap.put(role, groupId);
          membershipGroups.add(group);

          if (role.equals(maintenanceRole)) {
            // Add the creator as a high-access site member.
            UserManager userManager = AccessControlUtil.getUserManager(session);
            group.addMember(userManager.getAuthorizable(creatorId));
            LOGGER.warn("Added " + creatorId + " to group " + groupId);

            // Let members in the maintenance role administer memberships.
            groupAdministrators.add(valueFactory.createValue(groupId));
          }
        }
      } catch (JSONException e) {
        LOGGER.error("Bad site authz config for site " + site.getPath(), e);
      }
      // Set group management rights.
      Value[] adminPrincipals = groupAdministrators.toArray(new Value[groupAdministrators
          .size()]);
      for (Group membershipGroup : membershipGroups) {
        membershipGroup.setProperty(UserConstants.PROP_GROUP_MANAGERS,
            adminPrincipals);
        try {
          postProcessService.process(membershipGroup,session, ModificationType.CREATE);
        } catch (Exception e) {
          throw new RepositoryException(e.getMessage(),e);
        }
      }

    }
    return roleToGroupMap;
  }

  /**
   * @return true if the current user has sufficient rights to perform maintenance functions
   *         in the site, either directly or by membership in a maintainers role
   * @throws RepositoryException
   */
  public boolean isUserSiteMaintainer() {
    boolean isMaintainer = false;
    Session session;
    try {
      session = site.getSession();
      AccessControlManager accessControlManager = AccessControlUtil.getAccessControlManager(session);
      Privilege[] privileges = new Privilege[SITE_MAINTENANCE_PRIVILEGES.length];
      for (int i = 0; i < SITE_MAINTENANCE_PRIVILEGES.length; i++) {
        privileges[i] = accessControlManager.privilegeFromName(SITE_MAINTENANCE_PRIVILEGES[i]);
      }
      isMaintainer = accessControlManager.hasPrivileges(site.getPath(), privileges);
    } catch (RepositoryException e) {
      LOGGER.warn("Problem checking site authz", e);
    }
    return isMaintainer;
  }

  public boolean isUserPendingApproval() {
    boolean isPending = false;
    Session session;
    try {
      session = site.getSession();
      String sitePath = PathUtils.normalizePath(site.getPath());
      UserManager userManager = AccessControlUtil.getUserManager(session);
      String userPath = PathUtils.normalizePath(PathUtils.getSubPath(userManager.getAuthorizable(session.getUserID())));
      String requestPath = sitePath + "/joinrequests" + userPath;
      if (session.nodeExists(requestPath)) {
        Node joinRequestNode = session.getNode(requestPath);
        if (joinRequestNode.hasProperty("sakai:requestState") && "pending".equals(joinRequestNode.getProperty("sakai:requestState").getString())) {
          isPending = true;
        }
      }
    } catch (RepositoryException e) {
      LOGGER.warn("Problem checking for pending site-join approval", e);
    }
    return isPending;
  }

  /**
   * Handle authorization-related clean-up after site deletion. Currently, this means deleting
   * site-dependent groups.
   * @param session
   * @param slingRepository
   * @throws RepositoryException
   */
  public void deletionPostProcess(Session session, SlingRepository slingRepository) throws RepositoryException {
    if (!roleToGroupMap.isEmpty()) {
      UserManager userManager = AccessControlUtil.getUserManager(session);
      Collection<String> roleGroups = roleToGroupMap.values();
      for (String groupId : roleGroups) {
        Group group = (Group) userManager.getAuthorizable(groupId);
        deleteGroup(session, slingRepository, group);
      }
    }
  }

  /**
   * TODO Mostly copied from UpdateSakaiGroupPostServlet, and should be moved to a
   * shared service or utility function. See KERN-580.
   */
  private static boolean isUserGroupMaintainer(Session session, Group group) throws RepositoryException {
    boolean isMaintainer = false;
    UserManager userManager = AccessControlUtil.getUserManager(session);
    User currentUser = (User) userManager.getAuthorizable(session.getUserID());
    if (currentUser == null) {
      throw new RepositoryException("Can't locate the current user");
    }
    if (currentUser.isAdmin()) {
      isMaintainer = true;
    } else {
      if (group.hasProperty(UserConstants.PROP_GROUP_MANAGERS)) {
        Set<String> userPrincipals = new HashSet<String>();
        /* The following does not exist in JR2
        for (PrincipalIterator iter = currentUser.getPrincipals(); iter.hasNext();) {
          String pname = iter.nextPrincipal().getName();
          userPrincipals.add(pname);
        }
        */
        for (Iterator<Group> iter = currentUser.declaredMemberOf(); iter.hasNext();) {
          Group userGroup = iter.next();
          userPrincipals.add(userGroup.getID());
        }
        Value[] adminPrincipalValues = group.getProperty(UserConstants.PROP_GROUP_MANAGERS);
        for (Value adminPrincipalValue : adminPrincipalValues) {
          String adminPrincipal = adminPrincipalValue.getString();
          if (userPrincipals.contains(adminPrincipal)) {
            isMaintainer = true;
            break;
          }
        }
      }
    }
    return isMaintainer;
  }

  /**
   * Currently, our JCR group support has a Catch-22: A user who is a member of a site
   * administrator's group can delete the group, but a group can't be deleted until
   * it's emptied of all members including the user. This means a two-step process:
   * 1. Check that the current user has the right to administer the group.
   * 2. Switch to an administrative session to do the actual deletion.
   *
   * TODO Mostly copied from UpdateSakaiGroupPostServlet, and should be moved to a
   * shared service or utility function. See KERN-580.
   *
   * @param group
   * @throws RepositoryException
   */
  private void deleteGroup(Session session, SlingRepository slingRepository, Group group)
      throws RepositoryException {
    // FIXME: this is managed by the Access Control manager and should not really be here.
    // For the moment, it has been adjusted to use the correct properties.
    if (!isUserGroupMaintainer(session, group)) {
      LOGGER.warn("User is not allowed to modify group");
      throw new RepositoryException("Not allowed to modify the group ");
    }
    // Switch to an administrative session.
    String groupId = group.getID();
    Session adminSession = null;
    try {
      adminSession = slingRepository.loginAdministrative(null);
      UserManager userManager = AccessControlUtil.getUserManager(adminSession);
      Authorizable authorizable = userManager.getAuthorizable(groupId);
      if (authorizable.isGroup()) {
        Group workingGroup = (Group)authorizable;
        // Is this group actually dependent on the site?
        if (group.hasProperty(GROUP_SITE_PROPERTY)) {
          Value[] groupSiteValues = group.getProperty(GROUP_SITE_PROPERTY);
          if (groupSiteValues.length == 1) {
            String groupSiteRef = groupSiteValues[0].getString();
            if (groupSiteRef.equals(siteRef)) {
              LOGGER.info("Delete site membership group {}", workingGroup.getID());
              Iterator<Authorizable> members = workingGroup.getDeclaredMembers();
              while (members.hasNext()) {
                Authorizable member = members.next();
                workingGroup.removeMember(member);
              }
              workingGroup.remove();
            } else {
              LOGGER.info("Membership group {} not linked to site {}",
                  new Object[] {workingGroup.getID(), siteRef});
            }
          }
        }
      }
      if ( adminSession.hasPendingChanges() ) {
        adminSession.save();
      }
    } finally {
      adminSession.logout();
    }
  }
}
