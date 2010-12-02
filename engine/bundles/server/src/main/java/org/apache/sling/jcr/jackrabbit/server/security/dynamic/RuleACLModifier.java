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
package org.apache.sling.jcr.jackrabbit.server.security.dynamic;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.RuleProtectedACLModifier;
import org.apache.jackrabbit.core.security.authorization.AccessControlConstants;
import org.apache.jackrabbit.core.security.authorization.acl.RulesPrincipal;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * Sets RuleACL properties on the node for permitted users.
 */
public class RuleACLModifier extends RuleProtectedACLModifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(RuleACLModifier.class);

  public RuleACLModifier() {
  }

  public Map<String, Property> getProperties(String path, Session sessionIn,
      Principal principal) throws RepositoryException {
    if (!(sessionIn instanceof JackrabbitSession) || !(sessionIn instanceof NameResolver)) {
      throw new IllegalArgumentException("Session must be a instance of "
          + JackrabbitSession.class.getName() + " was " + sessionIn.getClass().getName());
    }
    JackrabbitSession session = (JackrabbitSession) sessionIn;
    // get the ACL, then find the ACE that matches the principal
    // set the properties.
    if (!(principal instanceof RulesPrincipal)) {
      throw new IllegalArgumentException("Principal must identify a rule based ACE, "
          + principal + " does not");
    }
    NodeImpl controlledNode = (NodeImpl) session.getItem(path);
    if (isAccessControlled(controlledNode)) {
      NodeImpl aclNode = controlledNode.getNode(AccessControlConstants.N_POLICY);

      for (NodeIterator ni = aclNode.getNodes(); ni.hasNext();) {
        NodeImpl aceNode = (NodeImpl) ni.nextNode();
        String principalName = aceNode.getProperty(
            AccessControlConstants.P_PRINCIPAL_NAME).getString();
        if (principalName.equals(principal.getName())) {
          Map<String, Property> plist = new HashMap<String, Property>();
          for (PropertyIterator pi = aceNode.getProperties(); pi.hasNext();) {
            Property p = pi.nextProperty();
            plist.put(p.getName(), p);
          }
          return plist;
        }
      }

    }
    return null;

  }

  public Map<String, Object> setProperties(String path, Session sessionIn,
      Principal principal, Map<String, Object> properties) throws RepositoryException {
    if (!(sessionIn instanceof JackrabbitSession) || !(sessionIn instanceof NameResolver)) {
      throw new IllegalArgumentException("Session must be a instance of "
          + JackrabbitSession.class.getName() + " was " + sessionIn.getClass().getName());
    }
    JackrabbitSession session = (JackrabbitSession) sessionIn;
    NameResolver nameResolver = (NameResolver) sessionIn;
    // get the ACL, then find the ACE that matches the principal
    // set the properties.
    if (!(principal instanceof RulesPrincipal)) {
      throw new IllegalArgumentException("Principal must identify a rule based ACE, "
          + principal + " does not");
    }
    Map<String, Object> propertyMap = new HashMap<String, Object>();
    NodeImpl controlledNode = (NodeImpl) session.getItem(path);
    if (isAccessControlled(controlledNode)) {
      NodeImpl aclNode = controlledNode.getNode(AccessControlConstants.N_POLICY);
      for (NodeIterator ni = aclNode.getNodes(); ni.hasNext();) {
        NodeImpl aceNode = (NodeImpl) ni.nextNode();
        String principalName = aceNode.getProperty(
            AccessControlConstants.P_PRINCIPAL_NAME).getString();
        if (principalName.equals(principal.getName())) {
          for (Entry<String, Object> e : properties.entrySet()) {
            String propName = e.getKey();
            if (!propName.startsWith("rep:")) {
              Name name = nameResolver.getQName(propName);
              Object o = e.getValue();
              if (o instanceof Value[]) {
                LOGGER.info("Setting Property " + name + " to Array "
                    + Arrays.toString((Value[]) o));
                propertyMap.put(propName, setProperty(aceNode, name, (Value[]) o));
              } else if (o instanceof Value) {
                LOGGER.info("Setting Property " + name + " to " + o);
                propertyMap.put(propName, setProperty(aceNode, name, (Value) o));
              } else {
                throw new RepositoryException("Cant set " + propName + " as to " + o
                    + ", must be a Value or Value[] ");
              }
            } else {
              throw new RepositoryException("Cant set " + propName
                  + ", all rep:* properties are protected ");
            }
          }
        }
      }

    }
    return propertyMap;
  }

  /**
   * @param controlledNode
   * @return
   * @throws RepositoryException
   */
  private boolean isAccessControlled(NodeImpl controlledNode) throws RepositoryException {
    return controlledNode.isNodeType(AccessControlConstants.NT_REP_ACCESS_CONTROLLABLE)
        && controlledNode.hasNode(AccessControlConstants.N_POLICY);
  }
}
