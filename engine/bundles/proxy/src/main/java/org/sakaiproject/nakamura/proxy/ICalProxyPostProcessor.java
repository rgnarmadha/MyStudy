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
package org.sakaiproject.nakamura.proxy;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DateProperty;

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.proxy.ProxyPostProcessor;
import org.sakaiproject.nakamura.api.proxy.ProxyResponse;
import org.sakaiproject.nakamura.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Will convert iCal to JSON.
 */
@Service(value = ProxyPostProcessor.class)
@org.apache.felix.scr.annotations.Component(label = "ProxyPostProcessor for iCal", description = "Post processor which converts iCal data to JSON.", immediate = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai foundation"),
    @Property(name = "service.description", value = "Post processor which converts iCal data to JSON.") })
public class ICalProxyPostProcessor implements ProxyPostProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(ICalProxyPostProcessor.class);

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.proxy.ProxyPostProcessor#getName()
   */
  public String getName() {
    return "iCal";
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.proxy.ProxyPostProcessor#process(org.apache.sling.api.SlingHttpServletResponse,
   *      org.sakaiproject.nakamura.api.proxy.ProxyResponse)
   */
  public void process(Map<String, Object> templateParams,
      SlingHttpServletResponse response, ProxyResponse proxyResponse) throws IOException {
    try {
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      InputStream in = proxyResponse.getResponseBodyAsInputStream();
      CalendarBuilder builder = new CalendarBuilder();
      Calendar calendar = builder.build(in);

      // calendar.validate();

      JSONWriter write = new JSONWriter(response.getWriter());
      write.setTidy(true);

      write.object();

      write.key("vcalendar");
      write.object();

      write.key("vevents");
      write.array();

      ComponentList list = calendar.getComponents(Component.VEVENT);
      int size = list.size();
      int i = 0;
      while (i < size) {
        VEvent event = (VEvent) list.get(i);
        outputComponent(event, write);
        i++;
      }
      write.endArray();

      write.endObject();

      write.endObject();

    } catch (ParserException e) {
      LOG.error("Failed to parse iCal stream.", e);
    } catch (JSONException e) {
      LOG.error("Failed to convert iCal stream to JSON.", e);
    }

  }

  /**
   * @param c
   * @throws JSONException
   */
  private void outputComponent(VEvent event, JSONWriter write) throws JSONException {
    write.object();
    PropertyList pList = event.getProperties();
    int i = 0;
    int size = pList.size();
    while (i < size) {
      net.fortuna.ical4j.model.Property p = (net.fortuna.ical4j.model.Property) pList
          .get(i);
      write.key(p.getName());
      // Check if it is a date
      String value = p.getValue();
      if (p instanceof DateProperty) {
        DateProperty start = (DateProperty) p;
        value = DateUtils.iso8601(start.getDate());
      }

      write.value(value);
      i++;
    }
    write.endObject();
  }

}
