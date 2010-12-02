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
package org.sakaiproject.nakamura.connections;

import static javax.jcr.security.Privilege.JCR_ALL;
import static org.sakaiproject.nakamura.api.connections.ConnectionOperation.accept;
import static org.sakaiproject.nakamura.api.connections.ConnectionOperation.block;
import static org.sakaiproject.nakamura.api.connections.ConnectionOperation.cancel;
import static org.sakaiproject.nakamura.api.connections.ConnectionOperation.ignore;
import static org.sakaiproject.nakamura.api.connections.ConnectionOperation.invite;
import static org.sakaiproject.nakamura.api.connections.ConnectionOperation.reject;
import static org.sakaiproject.nakamura.api.connections.ConnectionOperation.remove;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.ACCEPTED;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.BLOCKED;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.IGNORED;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.INVITED;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.NONE;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.PENDING;
import static org.sakaiproject.nakamura.api.connections.ConnectionState.REJECTED;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.nakamura.api.connections.ConnectionConstants;
import org.sakaiproject.nakamura.api.connections.ConnectionException;
import org.sakaiproject.nakamura.api.connections.ConnectionManager;
import org.sakaiproject.nakamura.api.connections.ConnectionOperation;
import org.sakaiproject.nakamura.api.connections.ConnectionState;
import org.sakaiproject.nakamura.api.locking.LockManager;
import org.sakaiproject.nakamura.api.locking.LockTimeoutException;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 * Service for doing operations with connections.
 */
@Component(immediate = true, description = "Service for doing operations with connections.", label = "ConnectionSearchResultProcessor")
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation") })
@Service(value = ConnectionManager.class)
public class ConnectionManagerImpl implements ConnectionManager {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ConnectionManagerImpl.class);

  @Reference
  protected transient LockManager lockManager;

  @Reference
  protected transient SlingRepository slingRepository;

  @Reference
  protected ProfileService profileService;

  private static Map<TransitionKey, StatePair> stateMap = new HashMap<TransitionKey, StatePair>();

  static {
    stateMap.put(tk(NONE, NONE, invite), sp(PENDING, INVITED)); // t1
    stateMap.put(tk(REJECTED, REJECTED, invite), sp(PENDING, INVITED)); // t2
    stateMap.put(tk(PENDING, IGNORED, invite), sp(PENDING, INVITED)); // t3
    stateMap.put(tk(PENDING, INVITED, cancel), sp(NONE, NONE)); // t4
    stateMap.put(tk(PENDING, IGNORED, cancel), sp(NONE, NONE)); // t5
    stateMap.put(tk(PENDING, BLOCKED, cancel), sp(NONE, BLOCKED)); // t6
    stateMap.put(tk(INVITED, PENDING, accept), sp(ACCEPTED, ACCEPTED)); // t7
    stateMap.put(tk(INVITED, PENDING, reject), sp(REJECTED, REJECTED)); // t8
    stateMap.put(tk(INVITED, PENDING, ignore), sp(IGNORED, PENDING)); // t9
    stateMap.put(tk(INVITED, PENDING, block), sp(BLOCKED, PENDING)); // t10
    stateMap.put(tk(ACCEPTED, ACCEPTED, remove), sp(NONE, NONE)); // t11
    stateMap.put(tk(REJECTED, REJECTED, remove), sp(NONE, NONE)); // t12
    stateMap.put(tk(IGNORED, PENDING, remove), sp(NONE, NONE)); // t13
    stateMap.put(tk(PENDING, IGNORED, remove), sp(NONE, NONE)); // t14
    stateMap.put(tk(BLOCKED, PENDING, remove), sp(NONE, NONE)); // t15
    stateMap.put(tk(PENDING, BLOCKED, remove), sp(NONE, BLOCKED)); // t16
    stateMap.put(tk(NONE, BLOCKED, invite), sp(PENDING, BLOCKED)); // t17
    stateMap.put(tk(IGNORED, PENDING, invite), sp(PENDING, INVITED)); // t19
    stateMap.put(tk(INVITED, PENDING, invite), sp(ACCEPTED, ACCEPTED)); // t20
    stateMap.put(tk(NONE, NONE, remove), sp(NONE, NONE)); // t21
    stateMap.put(tk(NONE, BLOCKED, remove), sp(NONE, BLOCKED)); // t22
    stateMap.put(tk(BLOCKED, NONE, remove), sp(NONE, NONE)); // t23
  }

  /**
   * @param pending
   * @param invited
   * @return
   */
  private static StatePair sp(ConnectionState thisState, ConnectionState otherState) {
    return new StatePairFinal(thisState, otherState);
  }

  /**
   * @return
   */
  private static TransitionKey tk(ConnectionState thisState, ConnectionState otherState,
      ConnectionOperation operation) {
    return new TransitionKey(sp(thisState, otherState), operation);
  }

  /**
   * Check to see if a userId is actually a valid one
   *
   * @param session
   *          the JCR session
   * @param userId
   *          the userId to check
   * @return
   */
  protected Authorizable checkValidUserId(Session session, String userId)
      throws ConnectionException {
    Authorizable authorizable;
    if ("anonymous".equals(session.getUserID()) || "anonymous".equals(userId)) {
      throw new ConnectionException(403, "Cant make a connection with anonymous.");
    }
    try {
      UserManager userManager = AccessControlUtil.getUserManager(session);
      authorizable = userManager.getAuthorizable(userId);
      if (authorizable != null && authorizable.getID().equals(userId)) {
        return authorizable;
      }
    } catch (RepositoryException e) {
      // general repo failure
      throw new ConnectionException(500, e.getMessage(), e);
    } catch (Exception e) {
      // other failures return false
      LOGGER.info("Failure checking for valid user (" + userId + "): " + e);
      throw new ConnectionException(404, "User " + userId + " does not exist.");
    }
    throw new ConnectionException(404, "User " + userId + " does not exist.");
  }

  /**
   * Get the connection state from a node
   *
   * @param userContactNode
   *          the node to check (should be a user contact node)
   * @return the connection state (may be NONE)
   * @throws ConnectionException
   * @throws RepositoryException
   */
  protected ConnectionState getConnectionState(Node userContactNode)
      throws ConnectionException, RepositoryException {
    if (userContactNode == null) {
      throw new IllegalArgumentException(
          "Node cannot be null to check for connection state");
    }
    try {
      if (userContactNode.hasProperty(ConnectionConstants.SAKAI_CONNECTION_STATE)) {

        return ConnectionState.valueOf(userContactNode.getProperty(
            ConnectionConstants.SAKAI_CONNECTION_STATE).getString());
      }
    } catch (Exception e) {
    }
    return ConnectionState.NONE;
  }

  // SERVICE INTERFACE METHODS

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.connections.ConnectionManager#connect(org.apache.sling.api.resource.Resource,
   *      java.lang.String,
   *      org.sakaiproject.nakamura.api.connections.ConnectionConstants.ConnectionOperation,
   *      java.lang.String)
   */
  public boolean connect(Map<String, String[]> requestParameters, Resource resource,
      String thisUserId, String otherUserId, ConnectionOperation operation)
      throws ConnectionException {

    Session session = resource.getResourceResolver().adaptTo(Session.class);

    if (thisUserId.equals(otherUserId)) {
      throw new ConnectionException(
          400,
          "A user cannot operate on their own connection, this user and the other user are the same");
    }

    // fail if the supplied users are invalid
    Authorizable thisAu = checkValidUserId(session, thisUserId);
    Authorizable otherAu = checkValidUserId(session, otherUserId);

    Session adminSession = null;
    try {
      adminSession = slingRepository.loginAdministrative(null);

      // get the contact userstore nodes
      Node thisNode = getOrCreateConnectionNode(adminSession, thisAu, otherAu);
      Node otherNode = getOrCreateConnectionNode(adminSession, otherAu, thisAu);

      // check the current states
      ConnectionState thisState = getConnectionState(thisNode);
      ConnectionState otherState = getConnectionState(otherNode);
      StatePair sp = stateMap.get(tk(thisState, otherState, operation));
      if (sp == null) {
        throw new ConnectionException(400, "Cannot perform operation "
            + operation.toString() + " on " + thisState.toString() + ":"
            + otherState.toString());
      }

      // A legitimate invitation can set properties on the invited
      // user's view of the connection, including relationship types
      // that differ from those viewed by the inviting user.
      if (operation == ConnectionOperation.invite) {
        handleInvitation(requestParameters, adminSession, thisNode, otherNode);
      }

      // KERN-763 : Connections need to be "stored" in groups.
      StatePairFinal spAccepted = new StatePairFinal(ACCEPTED, ACCEPTED);
      if (sp.equals(spAccepted)) {
        addUserToGroup(thisAu, otherAu, adminSession);
        addUserToGroup(otherAu, thisAu, adminSession);
      } else {
        // This might be an existing connection that needs to be removed
        removeUserFromGroup(thisAu, otherAu, adminSession);
        removeUserFromGroup(otherAu, thisAu, adminSession);
      }

      sp.transition(thisNode, otherNode);

      // save changes if any were actually made
      if (adminSession.hasPendingChanges()) {
        adminSession.save();
      }

      if (operation == ConnectionOperation.invite) {
        throw new ConnectionException(200, "Invitation made between "
            + thisNode.getPath() + " and " + otherNode.getPath());
      }

    } catch (InvalidItemStateException e) {
      throw new ConnectionException(
          409,
          "There was a data conflict that cannot be resolved without user input (Simultaneaus requests.)");
    } catch (RepositoryException e) {
      throw new ConnectionException(500, e.getMessage(), e);
    } finally {
      if (adminSession != null) {
        // destroy the admin session
        adminSession.logout();
      }
    }
    return true;
  }

  /**
   * Removes a member from a group
   *
   * @param thisAu
   *          The {@link Authorizable authorizable} who owns the group.
   * @param otherAu
   *          The {@link Authorizable authorizable} who needs to be removed from the
   *          contact group.
   * @param adminSession
   *          A session that can be used to modify a group.
   * @throws RepositoryException
   */
  protected void removeUserFromGroup(Authorizable thisAu, Authorizable otherAu,
      Session session) throws RepositoryException {
    UserManager um = AccessControlUtil.getUserManager(session);
    Group g = (Group) um.getAuthorizable("g-contacts-" + thisAu.getID());
    if (g != null && g.isMember(otherAu)) {
      g.removeMember(otherAu);
    }
  }

  /**
   * Adds one user to another user his connection group.
   *
   * @param thisAu
   *          The base user who is adding a contact.
   * @param otherAu
   *          The user that needs to be added to the group.
   * @param session
   *          The session that can be used to locate and manipulate the group
   * @throws RepositoryException
   */
  protected void addUserToGroup(Authorizable thisAu, Authorizable otherAu, Session session)
      throws RepositoryException {
    UserManager um = AccessControlUtil.getUserManager(session);
    Group g = (Group) um.getAuthorizable("g-contacts-" + thisAu.getID());
    g.addMember(otherAu);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.connections.ConnectionManager#getConnectedUsers(java.lang.String,
   *      org.sakaiproject.nakamura.api.connections.ConnectionState)
   */
  public List<String> getConnectedUsers(String user, ConnectionState state) {
    ArrayList<String> l = new ArrayList<String>();
    // search string should look something like this
    // "//_user/contacts/a0/b0/c0/d0/aaron/*[@sling:resourceType=\"sakai/contact\" and @sakai:state=\"ACCEPTED\"]"
    try {
      Session adminSession = slingRepository.loginAdministrative(null);
      try {
        UserManager um = AccessControlUtil.getUserManager(adminSession);
        Authorizable au = um.getAuthorizable(user);
        // this will generate the bigstore path
        String connectionPath = ConnectionUtils.getConnectionPathBase(au);
        // create the search query string
        String search = "/jcr:root" + ISO9075.encodePath(connectionPath)
            + "//element(*)[@" + JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY
            + "=\"" + ConnectionConstants.SAKAI_CONTACT_RT + "\"";
        if (state != null) {
          search += " and @" + ConnectionConstants.SAKAI_CONNECTION_STATE + "=\""
              + state.name() + "\"]";
        } else {
          search += "]";
        }
        QueryManager qm = adminSession.getWorkspace().getQueryManager();
        Query query = qm.createQuery(search, Query.XPATH);
        QueryResult result = query.execute();
        NodeIterator nodeIterator = result.getNodes();
        while (nodeIterator.hasNext()) {
          Node node = nodeIterator.nextNode();
          l.add(node.getName());
        }
      } finally {
        adminSession.logout();
      }
    } catch (RepositoryException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }
    return l;
  }

  protected Node getOrCreateConnectionNode(Session session, Authorizable fromUser,
      Authorizable toUser) throws RepositoryException {
    String nodePath = ConnectionUtils.getConnectionPath(fromUser, toUser);
    if (session.itemExists(nodePath)) {
      return (Node) session.getItem(nodePath);
    }
    String basePath = ConnectionUtils.getConnectionPathBase(fromUser);
    try {
      lockManager.waitForLock(basePath);
    } catch (LockTimeoutException e) {
      LOGGER.error("Unable to obtain lock on base node");
      throw new RepositoryException("Unable to get connection node - lock timed out");
    }
    try {
      if (!session.itemExists(basePath)) {
        JcrUtils.deepGetOrCreateNode(session, basePath);
        AccessControlUtil.replaceAccessControlEntry(session, basePath, fromUser
            .getPrincipal(), new String[] { JCR_ALL }, null, null, null);
        LOGGER.info("Added ACL to [{}]", basePath);
      }
      Node node = JcrUtils.deepGetOrCreateNode(session, nodePath);
      if (node.isNew()) {
        node.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            ConnectionConstants.SAKAI_CONTACT_RT);
        // Place a reference to the authprofile of the user.
        Node profileNode = (Node) session.getItem(profileService.getProfilePath(toUser));
        node.setProperty("jcr:reference", profileNode.getIdentifier(),
            PropertyType.REFERENCE);
      }
      return node;
    } finally {
      lockManager.clearLocks();
    }
  }

  protected void handleInvitation(Map<String, String[]> requestProperties,
      Session session, Node fromNode, Node toNode) throws RepositoryException {
    Set<String> toRelationships = new HashSet<String>();
    Set<String> fromRelationships = new HashSet<String>();
    Map<String, String[]> sharedProperties = new HashMap<String, String[]>();
    for (Entry<String, String[]> rp : requestProperties.entrySet()) {
      String key = rp.getKey();
      String[] values = rp.getValue();
      if (ConnectionConstants.PARAM_FROM_RELATIONSHIPS.equals(key)) {
        fromRelationships.addAll(Arrays.asList(values));
      } else if (ConnectionConstants.PARAM_TO_RELATIONSHIPS.equals(key)) {
        toRelationships.addAll(Arrays.asList(values));
      } else if (ConnectionConstants.SAKAI_CONNECTION_TYPES.equals(key)) {
        fromRelationships.addAll(Arrays.asList(values));
        toRelationships.addAll(Arrays.asList(values));
      } else {
        sharedProperties.put(key, values);
      }
    }
    addArbitraryProperties(fromNode, sharedProperties);
    fromNode.setProperty(ConnectionConstants.SAKAI_CONNECTION_TYPES, fromRelationships
        .toArray(new String[fromRelationships.size()]));
    addArbitraryProperties(toNode, sharedProperties);
    toNode.setProperty(ConnectionConstants.SAKAI_CONNECTION_TYPES, toRelationships
        .toArray(new String[toRelationships.size()]));
  }

  /**
   * Add property values as individual strings or as string arrays.
   *
   * @param node
   * @param properties
   */
  protected void addArbitraryProperties(Node node, Map<String, String[]> properties)
      throws RepositoryException {
    for (Entry<String, String[]> param : properties.entrySet()) {
      String[] values = param.getValue();
      if (values.length == 1) {
        node.setProperty(param.getKey(), values[0]);
      } else {
        node.setProperty(param.getKey(), values);
      }
    }
  }

}
