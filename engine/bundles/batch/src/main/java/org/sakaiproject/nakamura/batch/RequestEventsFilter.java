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
package org.sakaiproject.nakamura.batch;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.util.osgi.EventUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 *
 */
@Service(value = Filter.class)
@Component(name = "org.sakaiproject.nakamura.batch.RequestEventsFilter", immediate = true, metatype = true)
@Properties(value = {
    @Property(name = "service.description", value = "Generates OSGi events for GET requests to resources."),
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "filter.scope", value = "request", propertyPrivate = true),
    @Property(name = "filter.order", intValue = { 1000 }, propertyPrivate = true) })
public class RequestEventsFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestEventsFilter.class);

  @Reference
  protected transient EventAdmin eventAdmin;

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
   *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {

    SlingHttpServletRequest request = (SlingHttpServletRequest) req;

    // check if we need to send an OSGi Event.
    // Only on GETs to resources who have the tracking property.
    if ("GET".equals(request.getMethod())) {
      try {
        ValueMap map = ResourceUtil.getValueMap(request.getResource());
        if (map.get("sakai:tracking", false)) {

          // Stick the user in the event properties.
          Dictionary<String, String> properties = new Hashtable<String, String>();
          properties.put(SlingConstants.PROPERTY_USERID, request.getRemoteUser());

          // Fire the event
          EventUtils.sendOsgiEvent(request.getResource(), properties,
              "org/apache/sling/api/resource/REQUESTED", eventAdmin);
        }
      } catch (Throwable t) {
        // We swallow everything so we don't interrupt anything.
        // We do leave a message in the error log.
        LOGGER.error("Unable to send an OSGi event for GETing  a resource.", t);
      }
    }

    // Continue the chain.
    chain.doFilter(req, resp);

  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  public void init(FilterConfig arg0) throws ServletException {
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#destroy()
   */
  public void destroy() {
  }

}
