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
package org.sakaiproject.nakamura.calendar.signup;

import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_EVENT_SIGNUP_PARTICIPANT_RT;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.nakamura.api.calendar.CalendarException;
import org.sakaiproject.nakamura.api.calendar.signup.SignupPreProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.http.HttpServletResponse;

/**
 * Checks if a signup event has a maximum number of participants.
 */
@Service
@Component(immediate = true)
public class MaxParticipantsSignupPreProcessors implements SignupPreProcessor {

  protected static final String SAKAI_EVENT_MAX_PARTICIPANTS = "sakai:event-max-participants";
  public static final Logger LOGGER = LoggerFactory
      .getLogger(MaxParticipantsSignupPreProcessors.class);

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.calendar.signup.SignupPreProcessor#checkSignup(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.Node)
   */
  public void checkSignup(SlingHttpServletRequest request, Node signupNode)
      throws CalendarException {

    try {
      // Get the number of maximum participants.
      if (signupNode.hasProperty(SAKAI_EVENT_MAX_PARTICIPANTS)) {
        long maxParticipants = signupNode.getProperty(SAKAI_EVENT_MAX_PARTICIPANTS)
            .getLong();

        // If a valid number is set, we check it.
        // -1 or smaller means we don't.
        if (maxParticipants > 0) {
          checkParticipants(signupNode, maxParticipants);
        }
      }
    } catch (RepositoryException e) {
      LOGGER
          .error(
              "Caugth a repository exception when checking for the maximum participants for an event signup.",
              e);
      throw new CalendarException(500, e.getMessage());
    }

  }

  /**
   * @param signupNode
   * @param maxParticipants
   * @throws CalendarException
   */
  protected void checkParticipants(Node signupNode, long maxParticipants)
      throws CalendarException {

    // Check how many participants there are in this event.
    // We do this by doing a query for the participants.
    try {
      Session session = signupNode.getSession();
      QueryManager qm = session.getWorkspace().getQueryManager();
      String query = "/jcr:root" + signupNode.getPath() + "//*[@sling:resourceType='"
          + SAKAI_EVENT_SIGNUP_PARTICIPANT_RT + "']";
      Query q = qm.createQuery(query, Query.XPATH);
      QueryResult result = q.execute();
      NodeIterator iterator = result.getNodes();

      // If there is a massive number of participants, this will be rather slow.
      long count = 0;
      while (iterator.hasNext()) {
        iterator.nextNode();
        count++;
      }

      // If we have filled the available slots, we throw an exception that bubbles up to
      // the signup servlet.
      if (count > maxParticipants) {
        throw new CalendarException(HttpServletResponse.SC_BAD_REQUEST,
            "This event has reached the maximum number of participants.");
      }

    } catch (RepositoryException e) {
      LOGGER
          .error(
              "Caught a repository exception when trying to get the number of participants for a calendar event.",
              e);
      throw new CalendarException(500, e.getMessage());
    }

  }

}
