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
package org.sakaiproject.nakamura.calendar.search;

import static org.sakaiproject.nakamura.api.search.SearchUtil.escapeString;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.nakamura.api.calendar.CalendarConstants;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;
import org.sakaiproject.nakamura.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

@Service
@Component(immediate = true, label = "CalenderSearchPropertyProvider", description = "Provides some calendar search properties.")
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Provides properties to process the calendar searches."),
    @Property(name = "sakai.search.provider", value = "Calendar") })
public class CalenderSearchPropertyProvider implements SearchPropertyProvider {

  public static final Logger LOGGER = LoggerFactory
      .getLogger(CalenderSearchPropertyProvider.class);

  protected static final String PATH_PARAM = "calendar-path";
  protected static final String START_DAY_PARAM = "start";
  protected static final String END_DAY_PARAM = "end";

  protected static final SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchPropertyProvider#loadUserProperties(org.apache.sling.api.SlingHttpServletRequest,
   *      java.util.Map)
   */
  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {

    // Add the path.
    addCalendarPath(request, propertiesMap);

    // Add the day.
    addStartEnd(request, propertiesMap);

    // Participants stuff
    addCalendarEventPath(request, propertiesMap);
  }

  /**
   * @param request
   * @param propertiesMap
   */
  protected void addCalendarEventPath(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    try {
      RequestParameter eventParam = request.getRequestParameter("event-path");
      if (eventParam != null) {
        String eventPath = eventParam.getString("UTF-8");
        eventPath = ISO9075.encodePath(eventPath);
        propertiesMap.put("_event-path", escapeString(eventPath, Query.XPATH));
      }
    } catch (UnsupportedEncodingException e) {
      LOGGER
          .error(
              "Caught an UnsupportedEncodingException when trying to provide properties for the calendar search templates.",
              e);
    }
  }

  /**
   * Adds the date range values to the search template.
   * 
   * <pre>
   * {@code
   *  - _date-start
   *  - _date-end
   *  }
   * </pre>
   * 
   * If there is no 'start' and 'end' request parameter, then the current day is used. A
   * day starts at 00:00 and ends the next day at 00:00
   * 
   * @param request
   *          The request that has been done on the search node.
   * @param propertiesMap
   *          The map where the key-values should be added to.
   */
  protected void addStartEnd(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    try {
      // Default is today
      Calendar cStart = getDayCalendar();
      Calendar cEnd = getDayCalendar();
      cEnd.add(Calendar.DAY_OF_MONTH, 1);

      // If a parameter is specified, we try to parse it and use that one.
      RequestParameter startParam = request.getRequestParameter(START_DAY_PARAM);
      RequestParameter endParam = request.getRequestParameter(END_DAY_PARAM);
      if (startParam != null && endParam != null) {
        String start = startParam.getString("UTF-8");
        String end = endParam.getString("UTF-8");
        cStart.setTime(format.parse(start));
        cEnd.setTime(format.parse(end));
      }

      // Calculate the beginning and the end date.
      String beginning = DateUtils.iso8601jcr(cStart);
      String end = DateUtils.iso8601jcr(cEnd);

      // Add to map.
      propertiesMap.put("_date-start", escapeString(beginning, Query.XPATH));
      propertiesMap.put("_date-end", escapeString(end, Query.XPATH));
    } catch (UnsupportedEncodingException e) {
      LOGGER
          .error(
              "Caught an UnsupportedEncodingException when trying to provide properties for the calendar search templates.",
              e);
    } catch (ParseException e) {
      LOGGER
          .error(
              "Caught a ParseException when trying to provide properties for the calendar search templates.",
              e);
    }

  }

  /**
   * @return
   */
  protected Calendar getDayCalendar() {
    Calendar c = Calendar.getInstance();
    c.set(Calendar.HOUR, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    return c;
  }

  /**
   * @param request
   * @param propertiesMap
   */
  protected void addCalendarPath(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    try {
      String user = request.getRemoteUser();
      Session session = request.getResourceResolver().adaptTo(Session.class);
      Authorizable au = PersonalUtils.getAuthorizable(session, user);
      String path = PersonalUtils.getHomeFolder(au) + "/"
          + CalendarConstants.SAKAI_CALENDAR_NODENAME;

      RequestParameter pathParam = request.getRequestParameter(PATH_PARAM);
      if (pathParam != null) {
        path = pathParam.getString("UTF-8");
      }

      propertiesMap.put("_calendar-path", escapeString(path, Query.XPATH));

    } catch (RepositoryException e) {
      LOGGER
          .error(
              "Caught a RepositoryException when trying to provide properties for the calendar search templates.",
              e);
    } catch (UnsupportedEncodingException e) {
      LOGGER
          .error(
              "Caught an UnsupportedEncodingException when trying to provide properties for the calendar search templates.",
              e);
    }
  }
}
