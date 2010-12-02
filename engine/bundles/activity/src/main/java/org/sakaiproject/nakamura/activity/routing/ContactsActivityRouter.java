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
package org.sakaiproject.nakamura.activity.routing;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.activity.AbstractActivityRoute;
import org.sakaiproject.nakamura.api.activity.ActivityConstants;
import org.sakaiproject.nakamura.api.activity.ActivityRoute;
import org.sakaiproject.nakamura.api.activity.ActivityRouter;
import org.sakaiproject.nakamura.api.activity.ActivityUtils;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

/**
 * This router will deliver an activity to the feed of all the contacts of the actor. It
 * will only deliver if the contact has READ access on the node where the activity was
 * performed on.
 */
@Component(immediate = true, enabled = true)
@Service(value = ActivityRouter.class)
public class ContactsActivityRouter implements ActivityRouter {

  @Reference
  protected ConnectionManager connectionManager;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ContactsActivityRouter.class);

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.activity.ActivityRouter#getPriority()
   */
  public int getPriority() {
    // The priority for this router isn't really important..
    return 0;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.activity.ActivityRouter#route(javax.jcr.Node,
   *      java.util.List)
   */
  public void route(Node activity, List<ActivityRoute> routes) {
    try {
      Session adminSession = activity.getSession();
      String activityFeedPath = null;
      String actor = activity.getProperty(ActivityConstants.PARAM_ACTOR_ID)
          .getString();
      List<String> connections = connectionManager.getConnectedUsers(actor,
          ConnectionState.ACCEPTED);
      if (connections != null && connections.size() > 0) {

        String activityPath = activity.getPath();
        AccessControlManager adminACM = AccessControlUtil
            .getAccessControlManager(adminSession);
        UserManager um = AccessControlUtil.getUserManager(adminSession);
        Privilege readPriv = adminACM.privilegeFromName("jcr:read");
        Privilege[] privs = new Privilege[] { readPriv };
        for (String connection : connections) {
          // Check if this connection has READ access on the path.
          boolean allowCopy = true;
          Session userSession = null;
          try {
            final SimpleCredentials credentials = new SimpleCredentials(
                connection, "foo".toCharArray());
            userSession = adminSession.impersonate(credentials);
            AccessControlManager userACM = AccessControlUtil
                .getAccessControlManager(userSession);
            allowCopy = userACM.hasPrivileges(activityPath, privs);
          } finally {
            // We no longer need this session anymore, release it.
            userSession.logout();
          }

          if (allowCopy) {
            // Get the activity feed for this contact and deliver it.
            Authorizable au = um.getAuthorizable(connection);
            activityFeedPath = ActivityUtils.getUserFeed(au);
            ActivityRoute route = new AbstractActivityRoute(activityFeedPath) {
            };
            routes.add(route);
          }
        }
      }
    } catch (RepositoryException e) {
      LOGGER.error(
          "Exception when trying to deliver an activity to contacts feed.", e);
    }
  }

}
