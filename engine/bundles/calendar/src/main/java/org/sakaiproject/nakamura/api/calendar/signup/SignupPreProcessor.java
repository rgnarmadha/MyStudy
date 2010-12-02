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
package org.sakaiproject.nakamura.api.calendar.signup;

import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.nakamura.api.calendar.CalendarException;

import javax.jcr.Node;

/**
 *
 */
public interface SignupPreProcessor {

  /**
   * Preprocesses a request that is trying to signup for an event.
   * 
   * @param request
   *          The {@link SlingHttpServletRequest request} that is executed when trying to
   *          signup for this event.
   * @param signupNode
   *          The signup node (sling:resourceType = sakai/event-signup) that is located
   *          under event (sling:resourceType = sakai/calendar-vevent)
   * @throws CalendarException
   *           If this request is for some reason invalid, a {@link CalendarException
   *           calendar exception} will be thrown containing the correct HTTP status code
   *           and message that should be returned.
   */
  void checkSignup(SlingHttpServletRequest request, Node signupNode)
      throws CalendarException;

}
