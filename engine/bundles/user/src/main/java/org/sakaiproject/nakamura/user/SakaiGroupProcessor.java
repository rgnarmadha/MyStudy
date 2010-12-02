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
package org.sakaiproject.nakamura.user;

import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_GROUP_MANAGERS;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_MANAGED_GROUP;
import static org.sakaiproject.nakamura.api.user.UserConstants.PROP_MANAGERS_GROUP;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

/**
 * This class handles whatever processing is needed before the Jackrabbit Group modification
 * can be sent to other AuthorizablePostProcessor services.
 */
public class SakaiGroupProcessor extends AbstractAuthorizableProcessor implements
    AuthorizablePostProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(SakaiGroupProcessor.class);
  public static final String PARAM_ADD_TO_MANAGERS_GROUP = SlingPostConstants.RP_PREFIX + "sakai:manager";
  public static final String PARAM_REMOVE_FROM_MANAGERS_GROUP = PARAM_ADD_TO_MANAGERS_GROUP + SlingPostConstants.SUFFIX_DELETE;

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor#process(org.apache.jackrabbit.api.security.user.Authorizable, javax.jcr.Session, org.apache.sling.servlets.post.Modification, java.util.Map)
   */
  public void process(Authorizable authorizable, Session session, Modification change,
      Map<String, Object[]> parameters) throws Exception {
    if (authorizable.isGroup()) {
      Group group = (Group) authorizable;
      if (change.getType() == ModificationType.DELETE) {
        deleteManagersGroup(group, session);
      } else {
        ensurePath(authorizable, session, UserConstants.GROUP_REPO_LOCATION);
        if (change.getType() == ModificationType.CREATE) {
          createManagersGroup(group, session);
        }
        updateManagersGroupMembership(group, session, parameters);
      }
    }
  }

  /**
   * Generate a private self-managed Jackrabbit Group to hold Sakai group
   * members with the Manager role. Such members have all access
   * rights over the Sakai group itself and may be given special access
   * rights to content.
   */
  private void createManagersGroup(Group group, Session session) throws RepositoryException {
    UserManager userManager = AccessControlUtil.getUserManager(session);
    String managersGroupId = makeUniqueAuthorizableId(group.getID() + "-managers", userManager);

    // Create the public self-managed managers group.
    Group managersGroup = userManager.createGroup(makePrincipal(managersGroupId));
    ValueFactory valueFactory = session.getValueFactory();
    Value managersGroupValue = valueFactory.createValue(managersGroupId);
    managersGroup.setProperty(PROP_GROUP_MANAGERS, new Value[] {managersGroupValue});

    // Add the managers group to its Sakai group.
    group.addMember(managersGroup);

    // Have the managers group manage the Sakai group.
    addStringToValues(group, PROP_GROUP_MANAGERS, managersGroupId, valueFactory);

    // Set the association between the two groups.
    group.setProperty(PROP_MANAGERS_GROUP, managersGroupValue);
    managersGroup.setProperty(PROP_MANAGED_GROUP, valueFactory.createValue(group.getID()));
  }

  private void addStringToValues(Authorizable authorizable, String propertyName, String newString, ValueFactory valueFactory) throws RepositoryException {
    List<Value> newValues = new ArrayList<Value>();
    if (authorizable.hasProperty(propertyName)) {
      Value[] oldValues = authorizable.getProperty(propertyName);
      for (Value oldValue : oldValues) {
        if (newString.equals(oldValue.getString())) {
          return;
        } else {
          newValues.add(oldValue);
        }
      }
    }
    Value newValue = valueFactory.createValue(newString);
    newValues.add(newValue);
    authorizable.setProperty(propertyName, newValues.toArray(new Value[newValues.size()]));
  }

  private Principal makePrincipal(final String principalId) {
    return new Principal() {
      public String getName() {
        return principalId;
      }
    };
  }

  /**
   * @param group
   * @param session
   * @return the group that holds the group's Manager members, or null if there is no
   *         managers group or it is inaccessible
   * @throws RepositoryException
   */
  private Group getManagersGroup(Group group, UserManager userManager) throws RepositoryException {
    Group managersGroup = null;
    if (group.hasProperty(UserConstants.PROP_MANAGERS_GROUP)) {
      Value values[] = group.getProperty(UserConstants.PROP_MANAGERS_GROUP);
      String managersGroupId = values[0].getString();
      managersGroup = (Group) userManager.getAuthorizable(managersGroupId);
    }
    return managersGroup;
  }

  /**
   * Inspired by Sling's AbstractCreateOperation ensureUniquePath.
   * @param startId
   * @param userManager
   * @return
   * @throws RepositoryException
   */
  private String makeUniqueAuthorizableId(String startId, UserManager userManager) throws RepositoryException {
    String newAuthorizableId = startId;
    int idx = 0;
    while (userManager.getAuthorizable(newAuthorizableId) != null) {
      if (idx > 100) {
        throw new RepositoryException("Too much contention on authorizable ID " + startId);
      } else {
        newAuthorizableId = startId + "_" + idx++;
      }
    }
    return newAuthorizableId;
  }

  private void updateManagersGroupMembership(Group group, Session session, Map<String, Object[]> parameters) throws RepositoryException {
    UserManager userManager = AccessControlUtil.getUserManager(session);
    Group managersGroup = getManagersGroup(group, userManager);
    if (managersGroup != null) {
      Object[] addValues = parameters.get(PARAM_ADD_TO_MANAGERS_GROUP);
      if ((addValues != null) && (addValues instanceof String[])) {
        for (String memberId : (String [])addValues) {
          Authorizable authorizable = userManager.getAuthorizable(memberId);
          if (authorizable != null) {
            managersGroup.addMember(authorizable);
          } else {
            LOGGER.warn("Could not add {} to managers group {}", memberId, managersGroup.getID());
          }
        }
      }
      Object[] removeValues = parameters.get(PARAM_REMOVE_FROM_MANAGERS_GROUP);
      if ((removeValues != null) && (removeValues instanceof String[])) {
        for (String memberId : (String [])removeValues) {
          Authorizable authorizable = userManager.getAuthorizable(memberId);
          if (authorizable != null) {
            managersGroup.removeMember(authorizable);
          } else {
            LOGGER.warn("Could not remove {} from managers group {}", memberId, managersGroup.getID());
          }
        }
      }
    }
  }

  private void deleteManagersGroup(Group group, Session session) throws RepositoryException {
    UserManager userManager = AccessControlUtil.getUserManager(session);
    Group managersGroup = getManagersGroup(group, userManager);
    if (managersGroup != null) {
      LOGGER.debug("Deleting managers group {} as part of deleting {}", managersGroup.getID(), group.getID());
      managersGroup.remove();
    }
  }
}
