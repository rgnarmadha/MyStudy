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
package org.sakaiproject.nakamura.calendar;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_CALENDAR_RT;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SIGNUP_NODE_NAME;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SIGNUP_NODE_RT;

import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Uid;

import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.calendar.CalendarException;
import org.sakaiproject.nakamura.util.IOUtils;

import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.text.ParseException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

/**
 *
 */
public class CalendarServiceImplTest {

  private CalendarServiceImpl service;
  private MockNode calendarNode;
  private CalendarSubPathProducer producer;
  private Uid uid;
  private Node eventNode;
  private Node signupNode;

  @Before
  public void setUp() {
    service = new CalendarServiceImpl();
    uid = new Uid("DD45F1DB-34ED-4BBE-A2B2-970E0668F7BC");
    PropertyList pList = new PropertyList();
    pList.add(uid);
    producer = new CalendarSubPathProducer(new VEvent(pList));
  }

  @Test
  public void testStoringCalendarInputStream() throws Exception {
    InputStream in = getClass().getClassLoader().getResourceAsStream("home.ics");
    // Do mocking
    String path = "/path/to/store/calendar";
    Session session = mockSession(path);
    // Store nodes
    service.store(in, session, path);
    // Verify if everything is correct.
    verifyMocks();
  }

  @Test
  public void testStoringCalendarString() throws Exception {
    InputStream in = getClass().getClassLoader().getResourceAsStream("home.ics");
    String calendar = IOUtils.readFully(in, "UTF-8");
    // Do mocking
    String path = "/path/to/store/calendar";
    Session session = mockSession(path);
    // Store nodes
    service.store(calendar, session, path);
    // Verify if everything is correct.
    verifyMocks();
  }

  @Test
  public void testStoringCalendarReader() throws Exception {
    String fileName = getClass().getClassLoader().getResource("home.ics").getFile();
    Reader r = new FileReader(fileName);
    // Do mocking
    String path = "/path/to/store/calendar";
    Session session = mockSession(path);
    // Store nodes
    service.store(r, session, path);
    // Verify if everything is correct.
    verifyMocks();
  }

  @Test
  public void testStoringCalendarException() {
    try {
      service.store("foo", null, null);
      fail("Should have thrown an exception.");
    } catch (CalendarException e) {
      assertEquals(500, e.getCode());
    }
  }

  /**
   * @throws RepositoryException
   * @throws ValueFormatException
   * 
   */
  private void verifyMocks() throws Exception {
    assertEquals(calendarNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString(),
        SAKAI_CALENDAR_RT);

    // Verify if all the calendar stuff is correct.
    assertEquals(calendarNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString(),
        SAKAI_CALENDAR_RT);
    assertEquals(calendarNode.getProperty("X-WR-CALNAME").getString(), "Home");

    // Event node
    verify(eventNode).setProperty(SLING_RESOURCE_TYPE_PROPERTY, "sakai/calendar-vevent");
    verify(eventNode).setProperty("sakai:vcal-UID", uid.getValue());
    verify(eventNode).setProperty("sakai:vcal-SUMMARY", "Foobar");

    // signup node
    verify(signupNode).setProperty(SLING_RESOURCE_TYPE_PROPERTY, SIGNUP_NODE_RT);

  }

  /**
   * @param path
   * @return
   * @throws RepositoryException
   * @throws ParseException
   */
  private Session mockSession(String path) throws Exception {
    Session session = mock(Session.class);

    // The top calendar node
    calendarNode = new MockNode(path);
    when(session.itemExists(path)).thenReturn(true);
    when(session.getItem(path)).thenReturn(calendarNode);

    // The event node
    DtStart start = new DtStart("20100427T130000");
    String eventPath = path + producer.getDateHashed(start) + "-" + uid.getValue();
    System.err.println(eventPath);
    eventNode = mock(Node.class);
    signupNode = mock(Node.class);
    when(session.itemExists(eventPath)).thenReturn(true);
    when(session.getItem(eventPath)).thenReturn(eventNode);
    when(eventNode.addNode(SIGNUP_NODE_NAME)).thenReturn(signupNode);

    calendarNode.setSession(session);
    return session;
  }

}
