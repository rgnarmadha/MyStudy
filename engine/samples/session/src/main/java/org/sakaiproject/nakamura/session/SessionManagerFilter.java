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

package org.sakaiproject.nakamura.session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.session.SessionManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * The <code>SakaiRequestFilter</code> class is a request level filter, which manages the
 * Sakai Cache and Transaction services..
 */
@Service(value=Filter.class)
@Component(immediate=true, metatype=false)
@Properties(value={@Property(name="service.description", value="Session Manager Support Filter"),
    @Property(name="service.vendor",value="The Sakai Foundation"),
    @Property(name="filter.scope",value="request", propertyPrivate=true),
    @Property(name="filter.order",intValue={10}, propertyPrivate=true)})
public class SessionManagerFilter implements Filter {
  private static final Logger LOGGER = LoggerFactory.getLogger(SessionManagerFilter.class);

  private static final boolean debug = LOGGER.isDebugEnabled();

  private boolean timeOn = false;


  
  @Reference
  private SessionManagerService sessionManagerService;


  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
   */
  public void init(FilterConfig config) throws ServletException {

  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#destroy()
   */
  public void destroy() {

  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
   *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
   */
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest hrequest = (HttpServletRequest) request;
    sessionManagerService.bindRequest(hrequest);
    String requestedSessionID = hrequest.getRequestedSessionId();
    try {
      if (timeOn) {
        long start = System.currentTimeMillis();
        try {
          chain.doFilter(request, response);

        } finally {
          long end = System.currentTimeMillis();
          LOGGER.info("Request took " + hrequest.getMethod() + " "
              + hrequest.getPathInfo() + " " + (end - start) + " ms");
        }
      } else {
        chain.doFilter(request, response);
      }
      /*
       * try { if (jcrService.hasActiveSession()) { Session session =
       * jcrService.getSession(); session.save(); } } catch (AccessDeniedException e) {
       * throw new SecurityException(e.getMessage(), e); } catch (Exception e) {
       * LOGGER.warn(e.getMessage(), e); }
       */
    } finally {
      sessionManagerService.unbindRequest(hrequest);
    }
    if (debug) {
      HttpSession hsession = hrequest.getSession(false);
      if (hsession != null && !hsession.getId().equals(requestedSessionID)) {
        LOGGER.debug("New Session Created with ID " + hsession.getId());
      }
    }
  }
}
