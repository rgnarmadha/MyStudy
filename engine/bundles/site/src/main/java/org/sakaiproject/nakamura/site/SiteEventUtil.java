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
package org.sakaiproject.nakamura.site;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.osgi.service.event.Event;
import org.sakaiproject.nakamura.api.site.SiteService.SiteEvent;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Utility class to create Site Events.
 */
public class SiteEventUtil {

  /**
   * Create a new event suitable for async propagation
   * 
   * @param event
   *          the event being emitted.
   * @param site
   *          The site
   * @param targetGroup
   *          the group.
   * @return A new event ready for posting.
   * @throws RepositoryException
   */
  public static Event newSiteEvent(SiteEvent event, Node site, Group targetGroup, Authorizable owner)
      throws RepositoryException {
    Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
    dictionary.put(SiteEvent.SITE, site.getPath());
    dictionary.put(SiteEvent.USER, site.getSession().getUserID());
    dictionary.put(SiteEvent.GROUP, targetGroup.getID());
    if (owner != null) {
      dictionary.put(SiteEvent.OWNER, owner.getID());
    }
    return new Event(event.getTopic(), dictionary);
  }

}
