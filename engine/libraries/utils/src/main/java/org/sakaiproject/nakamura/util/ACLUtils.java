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
package org.sakaiproject.nakamura.util;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlList;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.AccessControlPolicyIterator;
import javax.jcr.security.Privilege;

/**
 * @deprecated The functionality and constants in this class is deprecated. All this
 *             functionality can be found in {@link AccessControlUtil} and
 *             {@link Privilege}.
 */
public class ACLUtils {

  /**
   *
   */
  public static final String DENIED = "d:";
  /**
   *
   */
  public static final String GRANTED = "g:";

  
  public static final String READ_GRANTED = GRANTED + "jcr:read";
  
  public static final String MODIFY_PROPERTIES_GRANTED = GRANTED +"jcr:modifyProperties";
  
  public static final String ADD_CHILD_NODES_GRANTED = GRANTED +"jcr:addChildNodes";
  
  public static final String REMOVE_NODE_GRANTED = GRANTED +"jcr:removeNode";
  
  public static final String REMOVE_CHILD_NODES_GRANTED = GRANTED +"jcr:removeChildNodes";
  
  public static final String WRITE_GRANTED = GRANTED +"jcr:write";
  
  public static final String READ_ACL_GRANTED = GRANTED +"jcr:readAccessControl";
  
  public static final String MODIFY_ACL_GRANTED = GRANTED +"jcr:modifyAccessControl";
  
  public static final String NODE_TYPE_MANAGEMENT_GRANTED = GRANTED +"jcr:nodeTypeManagement";
  
  public static final String VERSION_MANAGEMENT_GRANTED = GRANTED + "jcr:versionManagement";
  
  public static final String ALL_GRANTED = GRANTED +"jcr:all";

  public static final String READ_DENIED = DENIED + "jcr:read";
  
  public static final String MODIFY_PROPERTIES_DENIED = DENIED +"jcr:modifyProperties";
  
  public static final String ADD_CHILD_NODES_DENIED = DENIED +"jcr:addChildNodes";
  
  public static final String REMOVE_NODE_DENIED = DENIED +"jcr:removeNode";
  
  public static final String REMOVE_CHILD_NODES_DENIED = DENIED +"jcr:removeChildNodes";
  
  public static final String WRITE_DENIED = DENIED +"jcr:write";
  
  public static final String READ_ACL_DENIED = DENIED +"jcr:readAccessControl";
  
  public static final String MODIFY_ACL_DENIED = DENIED +"jcr:modifyAccessControl";
  
  public static final String NODE_TYPE_MANAGEMENT_DENIED = DENIED +"jcr:nodeTypeManagement";
  
  public static final String VERSION_MANAGEMENT_DENIED = DENIED + "jcr:versionManagement";
  
  public static final String ALL_DENIED = DENIED +"jcr:all";
  
  
  private static final Logger LOGGER = LoggerFactory.getLogger(ACLUtils.class);

  /**
   * Add an ACL entry at a path for the authorizable.
   * 
   * @param path
   * @param authorizable
   * @param session
   * @param writePrivilageGranted
   * @throws RepositoryException
   * @deprecated use {@link AccessControlUtil.replaceAccessControlEntry(Session, String,
   *             Principal, String[], String[], String[], String)} instead.
   */
  @Deprecated
  public static void addEntry(String path, Authorizable authorizable, Session session,
      String... privilegeSpec) throws RepositoryException {
    addEntry(path, authorizable.getPrincipal(), session, privilegeSpec);
  }

  /**
   * Add an ACL entry at a path for the authorizable.
   *
   * @param path
   * @param principal
   * @param session
   * @param writePrivilageGranted
   * @throws RepositoryException
   * @deprecated use {@link AccessControlUtil.replaceAccessControlEntry(Session, String,
   *             Principal, String[], String[], String[], String)} instead.
   */
  @Deprecated
  public static void addEntry(String path, Principal principal, Session session,
        String... privilegeSpec) throws RepositoryException {

    String principalName = principal.getName();

    List<String> grantedPrivilegeNames = new ArrayList<String>();
    List<String> deniedPrivilegeNames = new ArrayList<String>();
    for (String spec : privilegeSpec) {
      if (spec.startsWith(GRANTED)) {
        grantedPrivilegeNames.add(spec.substring(GRANTED.length()));
      } else if (spec.startsWith(DENIED)) {
        deniedPrivilegeNames.add(spec.substring(DENIED.length()));
      }
    }

    AccessControlManager accessControlManager = AccessControlUtil
        .getAccessControlManager(session);
    AccessControlList updatedAcl = null;
    
    
    AccessControlPolicyIterator applicablePolicies = accessControlManager
        .getApplicablePolicies(path);
    while (applicablePolicies.hasNext()) {
      AccessControlPolicy policy = applicablePolicies.nextAccessControlPolicy();
      if (policy instanceof AccessControlList) {
        updatedAcl = (AccessControlList) policy;
        break;
      }
    }
    
    if (updatedAcl == null) {
      AccessControlPolicy[] policies = accessControlManager.getPolicies(path);
      for ( AccessControlPolicy policy : policies ) {
        if (policy instanceof AccessControlList) {
          updatedAcl = (AccessControlList) policy;
          break;
        }      
      }
    }

    if (updatedAcl == null) {
      throw new RepositoryException("Unable to find an access conrol policy to update.");
    }

    StringBuilder oldPrivileges = null;
    StringBuilder newPrivileges = null;
    if (LOGGER.isDebugEnabled()) {
      oldPrivileges = new StringBuilder();
      newPrivileges = new StringBuilder();
    }

    // keep track of the existing Aces for the target principal
    AccessControlEntry[] accessControlEntries = updatedAcl.getAccessControlEntries();
    List<AccessControlEntry> oldAces = new ArrayList<AccessControlEntry>();
    for (AccessControlEntry ace : accessControlEntries) {
      if (principalName.equals(ace.getPrincipal().getName())) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Found Existing ACE for principal {} on resource: ", new Object[] {
              principalName, path});
        }
        oldAces.add(ace);

        if (LOGGER.isDebugEnabled()) {
          // collect the information for debug logging
          boolean isAllow = AccessControlUtil.isAllow(ace);
          Privilege[] privileges = ace.getPrivileges();
          for (Privilege privilege : privileges) {
            if (oldPrivileges.length() > 0) {
              oldPrivileges.append(", "); // separate entries by commas
            }
            if (isAllow) {
              oldPrivileges.append("granted=");
            } else {
              oldPrivileges.append("denied=");
            }
            oldPrivileges.append(privilege.getName());
          }
        }
      }
    }

    // remove the old aces
    if (!oldAces.isEmpty()) {
      for (AccessControlEntry ace : oldAces) {
        updatedAcl.removeAccessControlEntry(ace);
      }
    }

    // add a fresh ACE with the granted privileges
    List<Privilege> grantedPrivilegeList = new ArrayList<Privilege>();
    for (String name : grantedPrivilegeNames) {
      if (name.length() == 0) {
        continue; // empty, skip it.
      }
      Privilege privilege = accessControlManager.privilegeFromName(name);
      grantedPrivilegeList.add(privilege);

      if (LOGGER.isDebugEnabled()) {
        if (newPrivileges.length() > 0) {
          newPrivileges.append(", "); // separate entries by commas
        }
        newPrivileges.append("granted=");
        newPrivileges.append(privilege.getName());
      }
    }
    if (grantedPrivilegeList.size() > 0) {
      updatedAcl.addAccessControlEntry(principal, grantedPrivilegeList
          .toArray(new Privilege[grantedPrivilegeList.size()]));
    }

    // add a fresh ACE with the denied privileges
    List<Privilege> deniedPrivilegeList = new ArrayList<Privilege>();
    for (String name : deniedPrivilegeNames) {
      if (name.length() == 0) {
        continue; // empty, skip it.
      }
      Privilege privilege = accessControlManager.privilegeFromName(name);
      deniedPrivilegeList.add(privilege);

      if (LOGGER.isDebugEnabled()) {
        if (newPrivileges.length() > 0) {
          newPrivileges.append(", "); // separate entries by commas
        }
        newPrivileges.append("denied=");
        newPrivileges.append(privilege.getName());
      }
    }
    if (deniedPrivilegeList.size() > 0) {
      AccessControlUtil.addEntry(updatedAcl, principal, deniedPrivilegeList
          .toArray(new Privilege[deniedPrivilegeList.size()]), false);
    }

    accessControlManager.setPolicy(path, updatedAcl);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Updated ACE for principalId {} for resource {} from {} to {}",
          new Object[] {principal.getName(), path, oldPrivileges.toString(),
              newPrivileges.toString()});
    }

  }

}
