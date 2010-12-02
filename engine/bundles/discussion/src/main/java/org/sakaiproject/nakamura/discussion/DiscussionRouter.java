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

package org.sakaiproject.nakamura.discussion;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.discussion.DiscussionConstants;
import org.sakaiproject.nakamura.api.discussion.DiscussionManager;
import org.sakaiproject.nakamura.api.message.AbstractMessageRoute;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRouter;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * This router will check for messages who have a transport of discussion or comment, then
 * checks the settings for this discussion. If there is a property that states all
 * discussion messages should be re-routed to an email address, this router will take care
 * of it.
 */
@Component(immediate = true, inherit = true, label = "%discussion.router.label", description = "%discussion.router.desc")
@Service
public class DiscussionRouter implements MessageRouter {

  @Reference
  private DiscussionManager discussionManager;

  private static final Logger logger = LoggerFactory.getLogger(DiscussionRouter.class);

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  protected void bindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = discussionManager;
  }

  protected void unbindDiscussionManager(DiscussionManager discussionManager) {
    this.discussionManager = null;
  }

  public int getPriority() {
    return 0;
  }

  public void route(Node n, MessageRoutes routing) {
    // Check if this message is a discussion/comment transport.
    try {
      if (n.hasProperty(MessageConstants.PROP_SAKAI_TO)
          && n.hasProperty(DiscussionConstants.PROP_MARKER)) {

        String to = n.getProperty(MessageConstants.PROP_SAKAI_TO).getString();
        String type = StringUtils.split(to, ':')[0];

        if ("comment".equals(type) || "discussion".equals(type)) {

          // TODO: I have a feeling that this is really part of something more generic
          // and not specific to discussion. If we make it specific to discussion we
          // will lose unified messaging and control of that messaging.

          // This is a discussion message, find the settings file for it.

          String marker = n.getProperty(DiscussionConstants.PROP_MARKER).getString();
          Node settings = discussionManager.findSettings(marker, n.getSession(), type);
          if (settings != null
              && settings.hasProperty(DiscussionConstants.PROP_NOTIFICATION)) {
            boolean sendMail = settings
                .getProperty(DiscussionConstants.PROP_NOTIFICATION).getBoolean();
            if (sendMail && settings.hasProperty(DiscussionConstants.PROP_NOTIFY_ADDRESS)) {
              String address = settings.getProperty(
                  DiscussionConstants.PROP_NOTIFY_ADDRESS).getString();
              // TODO: make this smtp.
              routing.add(new AbstractMessageRoute("internal:" + address) {
              });

            }
          }
        }

      }
    } catch (RepositoryException e) {
      logger.warn("Catched an exception when trying to re-route discussion messages: {}",
          e.getMessage());
    }
  }

}
