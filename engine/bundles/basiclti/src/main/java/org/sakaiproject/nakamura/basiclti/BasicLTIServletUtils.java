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
package org.sakaiproject.nakamura.basiclti;

import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_KEY;
import static org.sakaiproject.nakamura.api.basiclti.BasicLTIAppConstants.LTI_SECRET;

import org.apache.sling.jcr.base.util.AccessControlUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.security.AccessControlException;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;
import javax.jcr.version.VersionException;

public class BasicLTIServletUtils {
  /**
   * The keys that must be specially secured from normal Sling operation.
   */
  protected static Set<String> sensitiveKeys;
  /**
   * The keys which we cannot set on a Node due to JCR semantics.
   */
  protected static Set<String> unsupportedKeys;

  static {
    sensitiveKeys = new HashSet<String>(2);
    sensitiveKeys.add(LTI_KEY);
    sensitiveKeys.add(LTI_SECRET);
    sensitiveKeys = Collections.unmodifiableSet(sensitiveKeys);

    unsupportedKeys = new HashSet<String>(5);
    unsupportedKeys.add("jcr:primaryType");
    unsupportedKeys.add("jcr:created");
    unsupportedKeys.add("jcr:createdBy");
    unsupportedKeys.add(":operation");
    unsupportedKeys.add("_MODIFIERS"); // TrimPath stuff
    unsupportedKeys = Collections.unmodifiableSet(unsupportedKeys);
  }

  /**
   * Helper method to return a Set of Privileges that a normal user <i>should not</i> have
   * on a sensitive Node.
   * 
   * @param acm
   * @return
   * @throws AccessControlException
   * @throws RepositoryException
   */
  protected static Set<Privilege> getInvalidUserPrivileges(final AccessControlManager acm)
      throws AccessControlException, RepositoryException {
    Set<Privilege> invalidUserPrivileges = new HashSet<Privilege>(9);
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_ALL));
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_READ));
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_WRITE));
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_MODIFY_PROPERTIES));
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_READ_ACCESS_CONTROL));
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_MODIFY_ACCESS_CONTROL));
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_ADD_CHILD_NODES));
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_REMOVE_CHILD_NODES));
    invalidUserPrivileges.add(acm.privilegeFromName(Privilege.JCR_REMOVE_NODE));
    return invalidUserPrivileges;
  }

  /**
   * Checks to see if the current user is a member of the administrators group.
   * 
   * @param session
   * @return
   * @throws UnsupportedRepositoryOperationException
   * @throws RepositoryException
   */
  protected static boolean isAdminUser(final Session session)
      throws UnsupportedRepositoryOperationException, RepositoryException {
    final AccessControlManager acm = AccessControlUtil.getAccessControlManager(session);
    final Privilege[] privJcrAll = { acm.privilegeFromName(Privilege.JCR_ALL) };
    return acm.hasPrivileges("/", privJcrAll);
  }

  /**
   * Quietly removes a Property on a Node if it exists.
   * 
   * @param node
   * @param property
   * @throws VersionException
   * @throws LockException
   * @throws ConstraintViolationException
   * @throws PathNotFoundException
   * @throws RepositoryException
   */
  protected static void removeProperty(final Node node, final String property)
      throws VersionException, LockException, ConstraintViolationException,
      PathNotFoundException, RepositoryException {
    if (node.hasProperty(property)) {
      node.getProperty(property).remove();
    }
  }

}
