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

import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class MockEventAdmin implements EventAdmin {

  private List<Event> events = new ArrayList<Event>();

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.event.EventAdmin#postEvent(org.osgi.service.event.Event)
   */
  public void postEvent(Event event) {
    events.add(event);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.event.EventAdmin#sendEvent(org.osgi.service.event.Event)
   */
  public void sendEvent(Event event) {
    events.add(event);
  }

  public List<Event> getEvents() {
    return events;
  }

}
