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

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ValidationException;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.sakaiproject.nakamura.api.calendar.CalendarException;
import org.sakaiproject.nakamura.api.calendar.CalendarService;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;

import java.io.IOException;

import javax.jcr.Node;
import javax.servlet.ServletException;

@ServiceDocumentation(
    bindings = {
        @ServiceBinding(
            type = BindingType.TYPE,
            bindings = {"sakai/calendar"},
            extensions = {
                @ServiceExtension(name = "ics", description = {"Generates a calendar feed of the underlying JCR node structure."})
            }
        )
    },
    methods = {
        @ServiceMethod(
            name = "GET",
            description = {
                "This servlet will generate the underlying JCR node structure into a valid ical format.",
                "If no selector is specified it will output components of type VEvent.",
                "More components can be looked for by adding in the type as a selecter.",
                "eg: http://localhost:8080/path/to/calendar.vevent.vtodo.vjournal.ics"
            },
            response = {
                @ServiceResponse(code = 200, description = "Generates a calendar feed of the underlying JCR node structure."),
                @ServiceResponse(code = 500, description = "Something went wrong trying the serialize the underlying node structure, the failure is placed in the HTML.")
            }
        ) 
    },
    name = "CalendarServlet",
    description = "Serializes an underlying JCR structure into valid icalendar data",
    shortDescription = "Serializes an underlying JCR structure into valid icalendar data"
)
@SlingServlet(methods = { "GET" }, resourceTypes = { "sakai/calendar" }, extensions = { "ics" }, selectors = {}, generateComponent = true, generateService = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Serializes a JCR node structure into ical.") })
public class CalendarIcsServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -3279889579407055346L;

  @Reference
  protected transient CalendarService calendarService;

  /**
   * {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    
    response.setContentType("text/calendar");
    response.setCharacterEncoding("UTF-8");

    Node node = request.getResource().adaptTo(Node.class);
    try {
      String[] types = getSelectors(request);
      // Construct the Calendar from the node structure.
      Calendar iCal = calendarService.export(node, types);

      // Output the calendar, we don't do any validation.
      CalendarOutputter outputter = new CalendarOutputter(false);
      outputter.output(iCal, response.getOutputStream());
    } catch (CalendarException e) {
      e.printStackTrace();
      response.sendError(e.getCode(), e.getMessage());
    } catch (ValidationException e) {
      e.printStackTrace();
      response.sendError(500, "Failed to output proper ical.");
    }

  }

  /**
   * @param request
   * @return
   */
  protected String[] getSelectors(SlingHttpServletRequest request) {
    String[] types = request.getRequestPathInfo().getSelectors();
    if (types.length == 0) {
      // If there is no selector that specifies which type that is requested, we default
      // to VEVENT.
      return new String[] { Component.VEVENT };
    }
    return types;
  }

}
