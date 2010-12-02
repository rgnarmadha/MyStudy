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
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_CALENDAR_PROPERTY_PREFIX;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SAKAI_CALENDAR_RT;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SIGNUP_NODE_NAME;
import static org.sakaiproject.nakamura.api.calendar.CalendarConstants.SIGNUP_NODE_RT;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyFactory;
import net.fortuna.ical4j.model.PropertyFactoryImpl;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.DateProperty;

import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.nakamura.api.calendar.CalendarException;
import org.sakaiproject.nakamura.api.calendar.CalendarService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.util.DateUtils;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.sakaiproject.nakamura.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.security.Principal;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 *
 */
@org.apache.felix.scr.annotations.Component(immediate = true)
@Service(value = CalendarService.class)
public class CalendarServiceImpl implements CalendarService {

  public static final Logger LOGGER = LoggerFactory.getLogger(CalendarServiceImpl.class);

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.calendar.CalendarService#export(javax.jcr.Node)
   */
  public Calendar export(Node node) throws CalendarException {
    return export(node, new String[] { VEvent.VEVENT });
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.calendar.CalendarService#export(javax.jcr.Node,
   *      java.lang.String[])
   */
  public Calendar export(Node node, String[] types) throws CalendarException {

    // Start constructing the iCal Calendar.
    Calendar c = new Calendar();
    try {
      String path = node.getPath();
      Session session = node.getSession();

      // Do a query under this node for all the sakai/calendar-event nodes.
      StringBuilder sb = new StringBuilder("/jcr:root");
      sb.append(path);
      sb.append("//*[");
      for (int i = 0; i < types.length; i++) {
        String type = types[i];
        sb.append("@sling:resourceType='");
        sb.append(SAKAI_CALENDAR_RT).append("-").append(type.toLowerCase()).append("'");
        if (i < (types.length - 1)) {
          sb.append(" or ");
        }
      }
      sb.append("]");
      String queryString = sb.toString();

      // Perform the query
      QueryManager qm = session.getWorkspace().getQueryManager();
      Query q = qm.createQuery(queryString, Query.XPATH);
      QueryResult result = q.execute();
      NodeIterator nodes = result.getNodes();

      PropertyFactory propFactory = PropertyFactoryImpl.getInstance();

      // Each node represents a VEVENT.
      while (nodes.hasNext()) {
        Node resultNode = nodes.nextNode();
        VEvent event = new VEvent();
        PropertyIterator props = resultNode.getProperties(SAKAI_CALENDAR_PROPERTY_PREFIX
            + "*");
        while (props.hasNext()) {
          javax.jcr.Property p = props.nextProperty();
          // Get the name of the property but strip out the sakai:vcal-*
          String name = p.getName().substring(11);

          // Create an iCal property and add it to the event.
          Property calProp = propFactory.createProperty(name);
          Value v = p.getValue();
          String value = v.getString();
          if (v.getType() == PropertyType.DATE) {
            value = DateUtils.rfc2445(v.getDate().getTime());
          }

          calProp.setValue(value);
          event.getProperties().add(calProp);

        }
        // Add the event to the calendar.
        c.getComponents().add(event);

      }

    } catch (RepositoryException e) {
      LOGGER.error("Caught a repositoryException when trying to export a calendar", e);
      throw new CalendarException(500, e.getMessage());
    } catch (IOException e) {
      LOGGER.error("Caught an IOException when trying to export a calendar", e);
      throw new CalendarException(500, e.getMessage());
    } catch (URISyntaxException e) {
      LOGGER.error("Caught a URISyntaxException when trying to export a calendar", e);
      throw new CalendarException(500, e.getMessage());
    } catch (ParseException e) {
      LOGGER.error("Caught a ParseException when trying to export a calendar", e);
      throw new CalendarException(500, e.getMessage());
    }

    return c;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.calendar.CalendarService#store(net.fortuna.ical4j.model.Calendar,
   *      javax.jcr.Session, java.lang.String)
   */
  public Node store(Calendar calendar, Session session, String path)
      throws CalendarException {
    Node calendarNode = null;
    try {
      calendarNode = JcrUtils.deepGetOrCreateNode(session, path);
      calendarNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, SAKAI_CALENDAR_RT);

      // Store all the properties of the calendar on the node.
      @SuppressWarnings("rawtypes")
      Iterator it = calendar.getProperties().iterator();
      while (it.hasNext()) {
        Property p = (Property) it.next();
        calendarNode.setProperty(p.getName(), p.getValue());
      }

      // Now loop over all the events and store these.
      // We could do calendar.getComponents(Component.VEVENT) but we will choose
      // everything.
      // They can be filtered in the export method.
      ComponentList list = calendar.getComponents();
      @SuppressWarnings("unchecked")
      Iterator<CalendarComponent> events = list.iterator();
      while (events.hasNext()) {
        CalendarComponent component = events.next();
        storeEvent(calendarNode, component);
      }

      // Save the entire thing.
      if (session.hasPendingChanges()) {
        session.save();
      }
    } catch (RepositoryException e) {
      LOGGER.error("Caught a repositoryException when trying to store a calendar", e);
      throw new CalendarException(500, e.getMessage());
    }

    return calendarNode;

  }

  /**
   * @param calendarNode
   * @param event
   * @throws RepositoryException
   */
  protected void storeEvent(Node calendarNode, CalendarComponent component)
      throws RepositoryException {

    // Get the start date.
    CalendarSubPathProducer producer = new CalendarSubPathProducer(component);

    // TODO Hash the events.
    String path = calendarNode.getPath() + PathUtils.getSubPath(producer);
    Node eventNode = JcrUtils.deepGetOrCreateNode(calendarNode.getSession(), path);
    eventNode.setProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
        SAKAI_CALENDAR_RT + "-" + producer.getType());

    @SuppressWarnings("unchecked")
    Iterator<Property> it = component.getProperties().iterator();
    while (it.hasNext()) {
      Property p = it.next();
      if (p instanceof DateProperty) {
        Date d = ((DateProperty) p).getDate();
        java.util.Calendar value = java.util.Calendar.getInstance();
        value.setTime(d);
        eventNode.setProperty(SAKAI_CALENDAR_PROPERTY_PREFIX + p.getName(), value);
      } else {
        eventNode.setProperty(SAKAI_CALENDAR_PROPERTY_PREFIX + p.getName(), p.getValue());
      }
    }

    handlePrivacy(eventNode, component);

    // If this is an event, we add a signup node.
    if (component instanceof VEvent && !eventNode.hasNode(SIGNUP_NODE_NAME)) {
      Node signupNode = eventNode.addNode(SIGNUP_NODE_NAME);
      signupNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, SIGNUP_NODE_RT);
    }
  }

  /**
   * @param eventNode
   * @param component
   * @throws RepositoryException
   */
  protected void handlePrivacy(Node eventNode, CalendarComponent component)
      throws RepositoryException {
    // Default = public.
    if (component.getProperty(Clazz.CLASS) != null) {
      Clazz c = (Clazz) component.getProperty(Clazz.CLASS);
      if (c == Clazz.PRIVATE) {
        Session session = eventNode.getSession();
        PrincipalManager principalManager = AccessControlUtil
            .getPrincipalManager(session);
        Principal user = principalManager.getPrincipal(session.getUserID());
        Principal everyone = principalManager.getEveryone();
        Principal anon = new Principal() {
          public String getName() {
            return UserConstants.ANON_USERID;
          }
        };

        String[] granted = new String[] { "jcr:read", "jcr:write",
            "jcr:removeChildNodes", "jcr:modifyProperties", "jcr:addChildNodes",
            "jcr:removeNode" };

        // Grant access to the current user.
        AccessControlUtil.replaceAccessControlEntry(session, eventNode.getPath(), user,
            granted, null, null, null);

        // Deny everybody else
        AccessControlUtil.replaceAccessControlEntry(session, eventNode.getPath(),
            everyone, null, new String[] { "jcr:all" }, null, null);
        AccessControlUtil.replaceAccessControlEntry(session, eventNode.getPath(), anon,
            null, new String[] { "jcr:all" }, null, null);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.calendar.CalendarService#store(java.lang.String,
   *      javax.jcr.Session, java.lang.String)
   */
  public Node store(String calendar, Session session, String path)
      throws CalendarException {
    ByteArrayInputStream in = new ByteArrayInputStream(calendar.getBytes());
    return store(in, session, path);

  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.calendar.CalendarService#store(java.io.InputStream,
   *      javax.jcr.Session, java.lang.String)
   */
  public Node store(InputStream in, Session session, String path)
      throws CalendarException {
    try {
      CalendarBuilder builder = new CalendarBuilder();
      Calendar calendar = builder.build(in);
      return store(calendar, session, path);
    } catch (IOException e) {
      LOGGER.error(
          "Caught an IOException when trying to store a Calendar (InputStream).", e);
      throw new CalendarException(500, e.getMessage());
    } catch (ParserException e) {
      LOGGER.error(
          "Caught a ParserException when trying to store a Calendar (InputStream).", e);
      throw new CalendarException(500, e.getMessage());
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.calendar.CalendarService#store(java.io.Reader,
   *      javax.jcr.Session, java.lang.String)
   */
  public Node store(Reader reader, Session session, String path) throws CalendarException {
    try {
      CalendarBuilder builder = new CalendarBuilder();
      Calendar calendar = builder.build(reader);
      return store(calendar, session, path);
    } catch (IOException e) {
      LOGGER.error("Caught an IOException when trying to store a Calendar (Reader).", e);
      throw new CalendarException(500, e.getMessage());
    } catch (ParserException e) {
      LOGGER.error("Caught a ParserException when trying to store a Calendar (Reader).",
          e);
      throw new CalendarException(500, e.getMessage());
    }
  }

}
