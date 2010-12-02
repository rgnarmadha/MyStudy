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
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.commons.collections.iterators.IteratorChain;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.DynamicSecurityManager;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.SessionImpl;
import org.apache.jackrabbit.core.id.NodeId;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.CompiledPermissions;
import org.apache.jackrabbit.core.security.principal.EveryonePrincipal;
import org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic.RuleProcessorManager;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.DynamicPrincipalManager;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.ISO8601Date;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.RuleProcessor;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.RulesBasedAce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.security.AccessControlEntry;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.Privilege;

/**
 * Extension of the standard ACLProvider to use a dynamic entry collector.
 */
public class DynamicACLProvider extends ACLProvider {

  private static final Logger LOG = LoggerFactory.getLogger(DynamicACLProvider.class);
  private String userId;
  private DynamicPrincipalManager dynamicPrincipalManager;
  private LRUMap staticPrincipals = new LRUMap(1000);
  private NodeId rootNodeId;
  private RuleProcessorManager ruleProccesorManager;


  // This creates a second systemEditor that we can see, hopefully it wont cause problems having 2 of these.
  private ACLEditor systemEditor;



  /**
   * @param dynamicPrincipalManager2
   */
  public DynamicACLProvider(DynamicPrincipalManager dynamicPrincipalManager, RuleProcessorManager ruleProcessorManager) {
    this.dynamicPrincipalManager = dynamicPrincipalManager;
    this.ruleProccesorManager = ruleProcessorManager;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.impl.security.standard.ACLProvider#init(javax.jcr.Session,
   *      java.util.Map)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public void init(Session systemSession, Map configuration) throws RepositoryException {
    super.init(systemSession, configuration);
    NodeImpl node = (NodeImpl) systemSession.getRootNode();
    rootNodeId = node.getNodeId();
    systemEditor = new ACLEditor(systemSession, this);

  }

  /**
   * {@inheritDoc}
   * @see org.apache.jackrabbit.core.security.authorization.acl.ACLProvider#compilePermissions(java.util.Set)
   */
  @Override
  public CompiledPermissions compilePermissions(Set<Principal> principals)
      throws RepositoryException {
    userId = DynamicSecurityManager.getThreadBoundAMContext().getSession().getUserID();
    return super.compilePermissions(principals);
  }
  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.jcr.jackrabbit.server.impl.security.standard.ACLProvider#retrieveResultEntries(org.apache.jackrabbit.core.NodeImpl,
   *      java.util.List)
   */
  @Override
  protected Iterator<AccessControlEntry> retrieveResultEntries(NodeImpl node,
      List<String> principalNames) throws RepositoryException {
    return new Entries(node, principalNames).iterator();
  }

  /**
   * Inner class used to collect ACEs for a given set of principals throughout the node
   * hierarchy.
   */
  private class Entries {

    private final Collection<String> principalNames;
    private final List<AccessControlEntry> userAces = new ArrayList<AccessControlEntry>();
    private final List<AccessControlEntry> groupAces = new ArrayList<AccessControlEntry>();
    private StringBuilder construct;
    private boolean forceDebug = true;

    private Entries(NodeImpl node, Collection<String> principalNames)
        throws RepositoryException {
      this.principalNames = principalNames;
      if ( LOG.isDebugEnabled() || forceDebug ) {
        construct = new StringBuilder();
        construct.append("\nPath:").append(node.getPath());
      }
      collectEntries(node, node);
    }

    private void collectEntries(NodeImpl node, NodeImpl contextNode) throws RepositoryException {
      // if the given node is access-controlled, construct a new ACL and add
      // it to the list
      if (isAccessControlled(node)) {
        // build acl for the access controlled node
        NodeImpl aclNode = node.getNode(N_POLICY);
        // collectEntries(aclNode, principalNamesToEntries);
        collectEntriesFromAcl(aclNode, contextNode);
      }
      // recursively look for access controlled parents up the hierarchy.
      if (!rootNodeId.equals(node.getId())) {
        NodeImpl parentNode = (NodeImpl) node.getParent();
        collectEntries(parentNode, contextNode);
      }
    }

    /**
     * Separately collect the entries defined for the user and group principals.
     *
     * @param aclNode
     *          acl node
     * @throws RepositoryException
     *           if an error occurs
     */
    private void collectEntriesFromAcl(NodeImpl aclNode, NodeImpl contextNode) throws RepositoryException {
      if ( LOG.isDebugEnabled() || forceDebug ) {
        construct.append(":ACLNode:").append(aclNode.getPath());
        construct.append("\n");
      }
      SessionImpl sImpl = (SessionImpl) aclNode.getSession();
      PrincipalManager principalMgr = sImpl.getPrincipalManager();
      AccessControlManager acMgr = sImpl.getAccessControlManager();

      // first collect aces present on the given aclNode.
      List<AccessControlEntry> gaces = new ArrayList<AccessControlEntry>();
      List<AccessControlEntry> uaces = new ArrayList<AccessControlEntry>();

      ACLTemplate template = (ACLTemplate) systemEditor.getACL(aclNode);
      NodeIterator itr = aclNode.getNodes();
      while (itr.hasNext()) {
        NodeImpl aceNode = (NodeImpl) itr.nextNode();
        String principalName = aceNode.getProperty(
            AccessControlConstants.P_PRINCIPAL_NAME).getString();
        RulesPrincipal rp = null;
        try {
          rp = new RulesPrincipal(principalName);
          principalName = rp.getPrincipalName();
        } catch ( IllegalArgumentException e ) {
          LOG.debug("Principal {} is not a rules principal ",principalName, e);
        }
        if ( rp == null || isAceActiveCheap(aceNode) ) {
          // only process aceNode if 'principalName' is contained in the given set
          // or the dynamicPrincialManager says the user has the principal.

          if (principalNames.contains(principalName)
              || hasPrincipal(principalName, aclNode, contextNode,
                  userId)) {
            if ( rp == null || isAceActiveExpensive(aceNode,contextNode,userId) ) {
              Principal princ = principalMgr.getPrincipal(principalName);

              Value[] privValues = getValues(aceNode.getProperty(AccessControlConstants.P_PRIVILEGES));
              Privilege[] privs = new Privilege[privValues.length];
              if ( LOG.isDebugEnabled() || forceDebug ) {
                construct.append("[Matched,");
                construct.append((princ instanceof Group)?"group,":"user,");
                construct.append(aceNode
                    .isNodeType(AccessControlConstants.NT_REP_GRANT_ACE)?"grant,":"deny,").append(principalName);
                for (int i = 0; i < privValues.length; i++) {
                  construct.append(",").append(privValues[i].getString());
                }
                construct.append("]\n");
              }
              for (int i = 0; i < privValues.length; i++) {
                privs[i] = acMgr.privilegeFromName(privValues[i].getString());
              }
              // create a new ACEImpl (omitting validation check)
              AccessControlEntry ace = template.createEntry(princ, privs, aceNode
                  .isNodeType(AccessControlConstants.NT_REP_GRANT_ACE));
              // add it to the proper list (e.g. separated by principals)
              /**
               * NOTE: access control entries must be collected in reverse order in order to
               * assert proper evaluation.
               */
              if (EveryonePrincipal.getInstance().getName().equals(princ.getName()) ) {
                gaces.add(ace);
              } else if (princ instanceof Group) {
                gaces.add(0, ace);
              } else {
                uaces.add(0, ace);
              }
            } else if ( LOG.isDebugEnabled()  || forceDebug) {

              construct.append("[Not Active,").append(principalName).append("]\n");
            }
          } else if ( LOG.isDebugEnabled()  || forceDebug){
            construct.append("[Ignored,").append(principalName).append("]\n");
          }
        }
      }

      // add the lists of aces to the overall lists that contain the entries
      // throughout the hierarchy.
      if (!gaces.isEmpty()) {
        groupAces.addAll(gaces);
      }
      if (!uaces.isEmpty()) {
        userAces.addAll(uaces);
      }
    }


    @SuppressWarnings("unchecked")
    private Iterator<AccessControlEntry> iterator() {
      if ( forceDebug) {
        LOG.debug("User {} ACE {} ",userId,construct);
      }
      return new IteratorChain(userAces.iterator(), groupAces.iterator());
    }
  }

  /**
   * A more expensive check on the Ace to see if its active. The user will already have this principal so this can look wider than just the node.
   * @param aceNode
   * @param contextNode
   * @param userId
   * @return
   */
  protected boolean isAceActiveExpensive(NodeImpl aceNode, NodeImpl contextNode,
      String userId) {
    try {
      // check
      RulesPrincipal.checkValid(aceNode.getProperty(AccessControlConstants.P_PRINCIPAL_NAME).getString());
      if ( aceNode.hasProperty(RulesBasedAce.P_RULEPROCESSOR)) {
        String name = aceNode.getProperty(RulesBasedAce.P_RULEPROCESSOR).getString();
        RuleProcessor ruleProcessor = ruleProccesorManager.getRuleProcessor(name);
        if ( ruleProcessor != null ) {
          return ruleProcessor.isAceActive(aceNode,contextNode,userId);
        }
        return false; // no rule processor found so cant be active
      }
      return true; // it was active cheap, this MUST have been called, we could add a 2nd check here but that would be a waste.
    } catch ( IllegalArgumentException e ) {
      return true; // its not a rules based ACL so it must be active.
    } catch ( Exception e ) {
      return false; // an error in processing has to default to inactive
    }
  }

  /**
   * A cheap check on the node to see it it should be ignored. This should only consider properties on the node.
   * @param aceNode
   * @return
   * @throws RepositoryException
   * @throws ItemNotFoundException
   */
  protected boolean isAceActiveCheap(NodeImpl aceNode) throws ItemNotFoundException, RepositoryException {
    // should only be here if the principal is a RulesPrincipal
    try {
      RulesPrincipal.checkValid(aceNode.getProperty(AccessControlConstants.P_PRINCIPAL_NAME).getString());
      long now = System.currentTimeMillis();

      Value[] activeRanges = getValues(RulesBasedAce.P_ACTIVE_RANGE, aceNode);
      if ( activeRanges.length != 0 ) {
        for ( Value r : activeRanges) {
          String[] range = StringUtils.split(r.getString(),'/');
          ISO8601Date from = new ISO8601Date(range[0]);
          ISO8601Date to = new ISO8601Date(range[1]);
          if ( from.before(now) && to.after(now) ) {
            return true;

          }
        }
        return false; // it had active times but none matched
      }

      Value[] inactiveRanges = getValues(RulesBasedAce.P_INACTIVE_RANGE,aceNode);
      if ( inactiveRanges.length != 0 ) {
        for ( Value r : inactiveRanges) {
          String[] range = StringUtils.split(r.getString(),'/');
          ISO8601Date from = new ISO8601Date(range[0]);
          ISO8601Date to = new ISO8601Date(range[1]);
          if ( from.before(now) && to.after(now) ) {
            return false;
          }
        }
      }
      return true;
    } catch ( IllegalArgumentException e ) {
      LOG.debug("Was not a rules based acl {} ", e.getMessage());
      return true; // its not a rules based ACL so it must be active.
    } catch ( Exception e ) {
      LOG.debug("Was not a rules based acl {} ", e.getMessage());
      return true; // an error in processing has to default to active
    }
  }

  /**
   * Get the range of ACLs taking into account multiple property nameing, required by the ACL node schema. (it cant hold array properties)
   * @param property
   * @return
   * @throws RepositoryException
   */
  private Value[] getValues(String propertyName, NodeImpl node) throws RepositoryException {
    List<Value> values = new ArrayList<Value>();
    if ( node.hasProperty(propertyName)) {
      values.add(node.getProperty(propertyName).getValue());
    }
    for ( int i = 0; i < 100; i++ ) {
      if ( node.hasProperty(propertyName+i)) {
        values.add(node.getProperty(propertyName+i).getValue());
      } else {
        break;
      }
    }
    return values.toArray(new Value[values.size()]);
  }

  private Value[] getValues(Property property) throws RepositoryException {
    if ( property.isMultiple()) {
      return property.getValues();
    } else {
      return new Value[] {property.getValue()};
    }
  }

  protected boolean hasPrincipal(String principalName, NodeImpl aclNode, NodeImpl contextNode,
       String userId) {
    /*
     * Principals that don't have a 'dynamic=true' property will not be resolved
     * dynamically. We cache principals that are found not to be dynamic. The
     * cache is never invalidated because it is assumed that principals will not
     * be included in ACLs until their dynamic/static status has been set, and
     * that setting will not be modified subsequently.
     */
    if ( LOG.isDebugEnabled()) {
      try {
        LOG.debug("Dynamic Principal Resolution for Principal {} on {} context {} for {} ", new Object[] {principalName, aclNode.getPath(), contextNode.getPath(), userId});
      } catch (RepositoryException e1) {
        LOG.warn(e1.getMessage(),e1);
      }
    }
    synchronized (staticPrincipals) {
      if (staticPrincipals.containsKey(principalName)) {
        LOG.debug("Principal {} is cached static - not resolving dynamically",principalName );
        return false;
      }      
    }
    Session session = aclNode.getSession();
    if (session instanceof JackrabbitSession) {
      JackrabbitSession jcrSession = (JackrabbitSession) session;
      try {
        boolean dynamic = false;
        UserManager manager = jcrSession.getUserManager();
        Authorizable principal = manager.getAuthorizable(principalName);
        if ( principal == null ) {
          return false;
        } else if (principal.hasProperty(PrincipalProperties.DYNAMIC)) {
          Value[] dyn = principal.getProperty(PrincipalProperties.DYNAMIC);
          if (dyn != null && dyn.length > 0 && ("true".equals(dyn[0].getString()))) {
            LOG.debug("Found dynamic principal {} ",principalName);
            dynamic = true;
          }
        }
        if (!dynamic) {
          LOG.debug("Found static principal {}. Caching ",principalName);
          synchronized (staticPrincipals) {
            staticPrincipals.put(principalName, true);            
          }
          return false;
        }
      } catch (AccessDeniedException e) {
        LOG.error("Unable to determine group status", e);
      } catch (UnsupportedRepositoryOperationException e) {
        LOG.error("Unable to access user manager", e);
      } catch (RepositoryException e) {
        LOG.error("Unable to access user manager", e);
      }
    }
    LOG.debug("Resolving dynamic principal {} ",principalName);
    boolean has = dynamicPrincipalManager.hasPrincipalInContext(principalName, aclNode, contextNode, userId);
    if ( LOG.isDebugEnabled() ) {
      try {
        LOG.debug("This user {} has principal {}  at {} : {} ", new Object[] {userId, principalName, contextNode.getPath(), has});
      } catch (RepositoryException e) {
        LOG.warn(e.getMessage(),e);
      }
    }
    return has;
  }

}
