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
package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.RuleACLModifier;

import javax.jcr.AccessDeniedException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 *
 */
public class RuleProtectedACLModifier {

  protected RuleProtectedACLModifier() {
    Class<?> cl = getClass();
    if (!(RuleACLModifier.class.isAssignableFrom(cl))) {
      throw new IllegalArgumentException(
          "Only RuleACLModifier may extend from the RuleProtectedACLModifier");
    }
  }

  protected Property setProperty(NodeImpl parentImpl, Name name, Value value)
      throws RepositoryException {
    checkPermission(parentImpl, name, getPermission(false, false));
    // validation: make sure Node is not locked or checked-in.
    parentImpl.checkSetProperty();
    InternalValue intVs = InternalValue.create(value, parentImpl.session);
    return parentImpl.internalSetProperty(name, intVs);
  }

  /**
   * Set a multiple property as a same name property array (ie append the index)
   * We do this becuase the schema for the acl does not allow us to do array properties.
   * @param parentImpl
   * @param name
   * @param values
   * @return
   * @throws RepositoryException
   */
  protected Property[] setProperty(NodeImpl parentImpl, Name name, Value[] values)
      throws RepositoryException {
    String nameBase = name.getLocalName();
    String prefix = parentImpl.session.getWorkspace().getNamespaceRegistry().getPrefix(name.getNamespaceURI());
    Property[] p = new Property[values.length];
    for (int i = 0; i < values.length; i++) {
      Name propertyName = parentImpl.session.getQName(prefix+":"+nameBase+i);
      p[i] = setProperty(parentImpl, propertyName, values[i]);
    }
    return p;
  }


  private int getPermission(boolean isNode, boolean isRemove) {
    if (isNode) {
      return (isRemove) ? Permission.REMOVE_NODE : Permission.ADD_NODE;
    } else {
      return (isRemove) ? Permission.REMOVE_PROPERTY : Permission.SET_PROPERTY;
    }
  }

  private void checkPermission(NodeImpl node, Name childName, int perm)
      throws RepositoryException {
    if (perm > Permission.NONE) {
      SessionImpl sImpl = (SessionImpl) node.getSession();
      AccessManager acMgr = sImpl.getAccessManager();

      boolean isGranted = acMgr.isGranted(node.getPrimaryPath(), childName, perm);
      if (!isGranted) {
        throw new AccessDeniedException("Permission denied.");
      }
    }
  }

}
