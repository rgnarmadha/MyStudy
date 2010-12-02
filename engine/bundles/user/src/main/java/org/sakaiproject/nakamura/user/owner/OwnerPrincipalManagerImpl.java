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
package org.sakaiproject.nakamura.user.owner;

import static org.sakaiproject.nakamura.api.user.UserConstants.JCR_CREATED_BY;

import org.apache.sling.jcr.jackrabbit.server.security.dynamic.DynamicPrincipalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

/**
 * The <code>OwnerPrincipalManager</code>
 * Note, nodes must have mix:created or be a hierachy node (nt:file, nt:folder) to get jcr:createdBy
 *
 * @scr.component immediate="true" metatype="false"
 * @scr.service interface=
 *              "org.apache.sling.jcr.jackrabbit.server.security.dynamic.DynamicPrincipalManager"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="service.description" value="Owner Principal Manager Implementation"
 */
public class OwnerPrincipalManagerImpl implements DynamicPrincipalManager {

  /**
   *
   */
  private static Logger LOG = LoggerFactory.getLogger(OwnerPrincipalManagerImpl.class);

  public boolean hasPrincipalInContext(String principalName, Node aclNode, Node contextNode, String userId) {
    try {
      if ( userId == null ) {
        return false;
      }
      if ("owner".equals(principalName)) {

        LOG.debug("Granting .owner privs to node owner {} ",contextNode.getPath());
        if (contextNode.hasProperty(JCR_CREATED_BY)) {
          Property owner = contextNode.getProperty(JCR_CREATED_BY);
          String ownerName = owner.getString();
          LOG.debug("Got node owner: {}, Current User {}", ownerName, userId);
          if (userId.equals(ownerName)) {
              LOG.debug(" Current user [{}] is the owner of {} ",new Object[] {ownerName, contextNode.getPath()});
            return true;
          }
          LOG.debug(" node owner [{}] didn't match current user [{}] at {} ",new Object[] {ownerName, userId, contextNode.getPath()});
        } else {
          LOG.debug("Node: {}  has no {} property", contextNode.getPath(), JCR_CREATED_BY);
        }
      }
    } catch (RepositoryException e) {
      LOG.error("Unable to determine node ownership", e);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.jcr.jackrabbit.server.security.dynamic.DynamicPrincipalManager#getMembersOf(java.lang.String)
   */
  public List<String> getMembersOf(String principalName) {
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.jcr.jackrabbit.server.security.dynamic.DynamicPrincipalManager#getMembershipFor(java.lang.String)
   */
  public List<String> getMembershipFor(String principalName) {
    return null;
  }

}
