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
package org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic;

import org.apache.sling.jcr.jackrabbit.server.security.dynamic.DynamicPrincipalManager;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

/**
 * A Singleton implementation of the DynamicPrincipalManagerFactory.
 */
public class DynamicPrincipalManagerFactoryImpl extends ServiceTracker implements
    DynamicPrincipalManagerFactory {

  private DynamicPrincipalManager dynamicPrincipalManager;

  /**
   * Construct the Factory.
   * 
   * @param bundleContext
   *          the current bundle context.
   * 
   */
  public DynamicPrincipalManagerFactoryImpl(BundleContext bundleContext) {
    super(bundleContext, DynamicPrincipalManager.class.getName(), null);
    dynamicPrincipalManager = new DynamicPrincipalManager() {

      public boolean hasPrincipalInContext(String principalName, Node aclNode, Node contextNode,
          String userId) {
        Object[] services = getServices();
        if (services == null || services.length == 0) {
          // no managers configured, pass through, the user does not have the principal.
          return false;
        }
        for (Object serviceObject : services) {
          DynamicPrincipalManager principalManager = (DynamicPrincipalManager) serviceObject;
          if (principalManager.hasPrincipalInContext(principalName, aclNode, contextNode, userId)) {
            return true;
          }
        }
        return false;
      }

      public List<String> getMembersOf(String principalName) {
        Object[] services = getServices();
        if (services == null || services.length == 0) {
          // no managers configured, pass through, the user does not have the principal.
          return null;
        }
        boolean added = false;
        List<String> list = new ArrayList<String>();
        for (Object serviceObject : services) {
          DynamicPrincipalManager principalManager = (DynamicPrincipalManager) serviceObject;
          List<String> members = principalManager.getMembersOf(principalName);
          if (members != null) {
            list.addAll(members);
            added = true;
          }
        }
        if (!added) {
          return null;
        } 
        return list;
      }

      public List<String> getMembershipFor(String principalName) {
        Object[] services = getServices();
        if (services == null || services.length == 0) {
          // no managers configured, pass through, the user does not have the principal.
          return null;
        }
        boolean added = false;
        List<String> list = new ArrayList<String>();
        for (Object serviceObject : services) {
          DynamicPrincipalManager principalManager = (DynamicPrincipalManager) serviceObject;
          List<String> groups = principalManager.getMembershipFor(principalName);
          if (groups != null) {
            list.addAll(groups);
            added = true;
          }
        }
        if (!added) {
          return null;
        } 
        return list;
      }

    };
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic.DynamicPrincipalManagerFactory#getDynamicPrincipalManager()
   */
  public DynamicPrincipalManager getDynamicPrincipalManager() {
    return dynamicPrincipalManager;
  }

}
