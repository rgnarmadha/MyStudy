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
package org.sakaiproject.nakamura.user.servlet;

import static org.sakaiproject.nakamura.api.user.UserConstants.ANON_USERID;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.profile.ProfileService;

import java.io.IOException;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@Component(immediate = true, label = "%group.joinServlet.label", description = "%group.joinServlet.desc")
@SlingServlet(resourceTypes = "sakai/joinrequests", methods = "POST", selectors = "create", generateComponent = false)
public class GroupJoinRequestServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = -6508149691456203381L;

  private static final String PARAM_USERID = "userid";

  @SuppressWarnings(value = "NP_UNWRITTEN_FIELD", justification = "Injected by OSGi")
  @Reference
  protected transient SlingRepository slingRepository;

  /**
   * The OSGi Event Admin Service.
   */
  @SuppressWarnings(value = "NP_UNWRITTEN_FIELD", justification = "Injected by OSGi")
  @Reference
  private transient EventAdmin eventAdmin;

  @SuppressWarnings(value = "NP_UNWRITTEN_FIELD", justification = "Injected by OSGi")
  @Reference
  private transient ProfileService profileService;

  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Node joinrequests = request.getResource().adaptTo(Node.class);
    if (joinrequests == null) {
      response.sendError(HttpServletResponse.SC_NO_CONTENT,
          "Couldn't find joinrequests node");
      return;
    }
    try {
      String requestedBy = joinrequests.getSession().getUserID();
      if (ANON_USERID.equals(requestedBy)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "anonymous user may not request to join a group");
        return;
      }
      Node group = joinrequests.getParent();
      RequestParameter userToJoin = request.getRequestParameter(PARAM_USERID);
      if (userToJoin == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "requesting user must be specified in the request parameter " + PARAM_USERID);
        return;
      }
      if (!userToJoin.getString().equalsIgnoreCase(requestedBy)) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "You may not request someone other than yourself join a group");
        return;
      }
      joinGroup(group, userToJoin.getString());
    } catch (Exception e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
    }
  }

  private void joinGroup(Node group, String userId) throws AccessDeniedException,
      UnsupportedRepositoryOperationException, RepositoryException {
    Session session = null;

    Property groupId = group.getProperty("public/authprofile/sakai:group-id");
    session = slingRepository.loginAdministrative(null);
    UserManager userManager = AccessControlUtil.getUserManager(session);
    Authorizable user = userManager.getAuthorizable(userId);
    Authorizable groupAuthorizable = userManager.getAuthorizable(groupId.getString());
    Group targetGroup = (Group) groupAuthorizable;
    Joinable joinable = Joinable.no;
    Property joinability = group.getProperty("public/authprofile/sakai:group-joinable");
    if (joinability != null) {
      joinable = Joinable.valueOf(joinability.getString());
    }

    switch (joinable) {
    case no:
      break;
    case yes:
      // membership is automatically granted
      targetGroup.addMember(user);
      Dictionary<String, Object> eventProps = new Hashtable<String, Object>();
      eventAdmin.postEvent(new Event(GroupEvent.joinedSite.getTopic(), eventProps));
    case withauth:
      // a node is added to represent this user's request to join
      Node joinrequests = session.getNode(group.getPath() + "/joinrequests");
      // check to see if this user is already there
      if (joinrequests.hasNode(userId)) {
        // just update the date
        Node joinRequestUpdate = joinrequests.getNode(userId);
        joinRequestUpdate.setProperty("jcr:created", Calendar.getInstance());
      } else {
        Node joinrequest = joinrequests.addNode(userId);
        Node profileNode = session.getNode(profileService.getProfilePath(user));
        String profileId = profileNode.getProperty("jcr:uuid").getString();
        joinrequest.setProperty("jcr:created", Calendar.getInstance());
        joinrequest.setProperty("profile", profileId, PropertyType.REFERENCE);
        joinrequest.setProperty("sling:resourceType", "sakai/joinrequest");
      }
      session.save();
      break;
    default:
      break;
    }
  }

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
  public enum GroupEvent {
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
    public static final String TOPIC = "org/sakaiproject/nakamura/api/group/event/";
    /**
     * The user that is the subject of the event.
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

}
