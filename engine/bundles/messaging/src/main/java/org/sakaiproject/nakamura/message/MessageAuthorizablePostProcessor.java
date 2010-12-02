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
package org.sakaiproject.nakamura.message;

import static javax.jcr.security.Privilege.JCR_ALL;
import static javax.jcr.security.Privilege.JCR_READ;
import static javax.jcr.security.Privilege.JCR_WRITE;
import static org.apache.sling.jcr.base.util.AccessControlUtil.replaceAccessControlEntry;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * This PostProcessor listens to post operations on User objects and creates a message
 * store.
 */
@Component(immediate = true, label = "MessageAuthorizablePostProcessor", description = "Creates the message stores for users and groups.", metatype = false)
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Creates the message stores for users and groups."),
    @Property(name = "service.ranking", intValue=10)})
public class MessageAuthorizablePostProcessor implements AuthorizablePostProcessor {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(MessageAuthorizablePostProcessor.class);


  public void process(Authorizable authorizable, Session session, Modification change,
      Map<String, Object[]> parameters) throws Exception {
    LOGGER.debug("Starting MessageAuthorizablePostProcessor process");
    if (ModificationType.CREATE.equals(change.getType())) {
      if (authorizable != null && authorizable.getID() != null) {
        PrincipalManager principalManager = AccessControlUtil.getPrincipalManager(session);
        String path = PersonalUtils.getHomeFolder(authorizable) + "/"
            + MessageConstants.FOLDER_MESSAGES;
        LOGGER
            .debug("Creating message store node: {}", path);

        Node messageStore = JcrUtils.deepGetOrCreateNode(session, path);
        messageStore.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
            MessageConstants.SAKAI_MESSAGESTORE_RT);
        // ACL's are managed by the Personal User Post processor.
        Principal anon = new Principal() {

          public String getName() {
            return UserConstants.ANON_USERID;
          }

        };
        Principal everyone = principalManager.getEveryone();


        if ( !UserConstants.ANON_USERID.equals(authorizable.getID()) ) {
          // The user can do everything on this node.
          replaceAccessControlEntry(session, path, authorizable.getPrincipal(),
              new String[] { JCR_ALL }, null, null, null);
        }
        // explicitly deny anon and everyone, this is private space.
        String[] deniedPrivs = new String[] { JCR_READ, JCR_WRITE };
        replaceAccessControlEntry(session, path, anon, null, deniedPrivs, null, null);
        replaceAccessControlEntry(session, path, everyone, null, deniedPrivs, null, null);
      }
    }
  }

}
