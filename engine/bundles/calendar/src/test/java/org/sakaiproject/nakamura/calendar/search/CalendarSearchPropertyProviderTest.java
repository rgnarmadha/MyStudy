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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.mockito.MockitoTestUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

/**
 *
 */
public class CalendarSearchPropertyProviderTest {

  @Test
  public void testLoadProperties() throws Exception {
    CalenderSearchPropertyProvider provider = new CalenderSearchPropertyProvider();

    // Mock the request parameters
    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    RequestParameter startParam = mock(RequestParameter.class);
    RequestParameter endParam = mock(RequestParameter.class);
    RequestParameter eventPathParam = mock(RequestParameter.class);

    when(startParam.getString("UTF-8")).thenReturn("20100427");
    when(endParam.getString("UTF-8")).thenReturn("20100428");
    when(eventPathParam.getString("UTF-8")).thenReturn("/path/to/random/calendar");

    when(request.getRequestParameter(CalenderSearchPropertyProvider.START_DAY_PARAM))
        .thenReturn(startParam);
    when(request.getRequestParameter(CalenderSearchPropertyProvider.END_DAY_PARAM))
        .thenReturn(endParam);
    when(request.getRequestParameter("event-path")).thenReturn(eventPathParam);

    // User
    Authorizable au = MockitoTestUtils.createAuthorizable("jack", false);

    ResourceResolver resolver = mock(ResourceResolver.class);
    JackrabbitSession session = mock(JackrabbitSession.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);
    UserManager um = mock(UserManager.class);
    when(um.getAuthorizable("jack")).thenReturn(au);
    when(session.getUserManager()).thenReturn(um);

    when(request.getResourceResolver()).thenReturn(resolver);
    when(request.getRemoteUser()).thenReturn("jack");

    Map<String, String> map = new HashMap<String, String>();

    provider.loadUserProperties(request, map);

    assertEquals("/path/to/random/calendar", map.get("_event-path"));
    Calendar startDate = parseDate(map.get("_date-start"));
    assertEquals(2010, startDate.get(Calendar.YEAR));
    assertEquals(4, startDate.get(Calendar.MONTH) + 1);
    assertEquals(27, startDate.get(Calendar.DATE));
    Calendar endDate = parseDate(map.get("_date-end"));
    assertEquals(2010, endDate.get(Calendar.YEAR));
    assertEquals(4, endDate.get(Calendar.MONTH) + 1);
    assertEquals(28, endDate.get(Calendar.DATE));
  }

  public Calendar parseDate(String s) throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(sdf.parse(s).getTime());
    return c;
  }

}
