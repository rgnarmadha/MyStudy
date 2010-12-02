/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.email.outgoing;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.jcr.api.SlingRepository;
import org.sakaiproject.nakamura.api.message.AbstractMessageRoute;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRouter;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

@Service
@Component(inherit = true, immediate = true)
public class SmtpRouter implements MessageRouter {
  private static final Logger LOG = LoggerFactory.getLogger(SmtpRouter.class);

  /**
   * The JCR Repository we access.
   *
   */
  @Reference
  private SlingRepository slingRepository;

  /**
   * @param slingRepository
   *          the slingRepository to set
   */
  protected void bindSlingRepository(SlingRepository slingRepository) {
    this.slingRepository = slingRepository;
  }

  public int getPriority() {
    return 0;
  }

  public void route(Node n, MessageRoutes routing) {
    Collection<MessageRoute> rewrittenRoutes = new ArrayList<MessageRoute>();
    Iterator<MessageRoute> routeIterator = routing.iterator();
    while (routeIterator.hasNext()) {
      MessageRoute route = routeIterator.next();
      String rcpt = route.getRcpt();
      String transport = route.getTransport();

      LOG.debug("Checking Message Route {} ",route);
      boolean rcptNotNull = rcpt != null;
      boolean transportNullOrInternal = transport == null || "internal".equals(transport);

      if (rcptNotNull && transportNullOrInternal) {
        // check the user's profile for message delivery preference. if the
        // preference is set to smtp, change the transport to 'smtp'.
        try {
          Session session = slingRepository.loginAdministrative(null);
          Authorizable user = PersonalUtils.getAuthorizable(session, rcpt);
          if (user != null) {
            // We only check the profile is the recipient is a user.
            String profilePath = PersonalUtils.getProfilePath(user);
            Node profileNode = JcrUtils.deepGetOrCreateNode(session, profilePath);

            boolean smtpPreferred = isPreferredTransportSmtp(profileNode);
            boolean smtpMessage = isMessageTypeSmtp(n);
            if (smtpPreferred || smtpMessage) {
              LOG.debug("Message is an SMTP Message, getting email address for the user from  {} ", profileNode.getPath());
              String rcptEmailAddress = PersonalUtils.getPrimaryEmailAddress(profileNode);

              if (rcptEmailAddress == null || rcptEmailAddress.trim().length() == 0) {
                LOG.warn("Can't find a primary email address for [" + rcpt
                    + "]; smtp message will not be sent to user.");
              } else {
                AbstractMessageRoute smtpRoute = new AbstractMessageRoute(
                    MessageConstants.TYPE_SMTP + ":" + rcptEmailAddress) {
                };
                rewrittenRoutes.add(smtpRoute);
                routeIterator.remove();
              }
            }
          }
        } catch (RepositoryException e) {
          LOG.error(e.getMessage());
        }
      }
    }
    routing.addAll(rewrittenRoutes);
    
    LOG.debug("Final Routing is [{}]", Arrays.toString(routing.toArray(new MessageRoute[routing.size()])));
  }

  private boolean isMessageTypeSmtp(Node n) throws RepositoryException {
    boolean isSmtp = false;

    if (n != null && n.hasProperty(MessageConstants.PROP_SAKAI_TYPE)) {
      String prop = n.getProperty(MessageConstants.PROP_SAKAI_TYPE).getString();
      isSmtp = MessageConstants.TYPE_SMTP.equals(prop);
    }
   

    return isSmtp;
  }

  private boolean isPreferredTransportSmtp(Node profileNode) throws RepositoryException {
    boolean prefersSmtp = false;

    if (profileNode != null) {
      String transport = PersonalUtils.getPreferredMessageTransport(profileNode);
      prefersSmtp = MessageConstants.TYPE_SMTP.equals(transport);
    }

    return prefersSmtp;
  }
}
