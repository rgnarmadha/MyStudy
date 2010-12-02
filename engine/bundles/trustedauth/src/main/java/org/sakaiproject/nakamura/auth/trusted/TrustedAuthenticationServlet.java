/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.auth.trusted;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ops4j.pax.web.service.WebContainer;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.sakaiproject.nakamura.api.auth.trusted.TrustedTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Servlet for storing authentication credentials from requests using an external trusted
 * mechanism such as CAS.
 * </p>
 * <p>
 * This servlet does not perform the authentication itself but looks for information in
 * the request from the authentication authority. This information is then stored in the
 * session for use by the authentication handler on subsequent calls.
 * </p>
 * <p>
 * This servlet is mounted outside sling. In essence we Trust the external authentication and 
 * simply store the trusted user in a a trusted token in the form of a cookie.
 * </p>
 */
@Component(immediate = true, metatype = true)
@Service
public final class TrustedAuthenticationServlet extends HttpServlet implements HttpContext {
  /**
   * 
   */
  private static final long serialVersionUID = 4265672306115024805L;

  private static final Logger LOG = LoggerFactory
      .getLogger(TrustedAuthenticationServlet.class);
  
  private static final String PARAM_DESTINATION = "d";

  @Property(value = "Trusted Authentication Servlet", propertyPrivate = true)
  static final String DESCRIPTION_PROPERTY = "service.description";

  @Property(value = "The Sakai Foundation", propertyPrivate = true)
  static final String VENDOR_PROPERTY = "service.vendor";

  /** Property for the path to which to register this servlet. */
  @Property(value = "/system/trustedauth")
  static final String REGISTRATION_PATH = "sakai.auth.trusted.path.registration";

  /**
   * Property for the default destination to go to if no destination is specified.
   */
  @Property(value = "/dev")
  static final String DEFAULT_DESTINATION = "sakai.auth.trusted.destination.default";

  private static final Logger LOGGER = LoggerFactory.getLogger(TrustedAuthenticationServlet.class);

  /** Reference to web container to register this servlet. */
  @Reference
  protected transient WebContainer webContainer;

  @Reference
  protected transient TrustedTokenService trustedTokenService;

  /** The registration path for this servlet. */
  private String registrationPath;

  /** The default destination to go to if none is specified. */
  private String defaultDestination;

  @SuppressWarnings("rawtypes")
  @Activate
  protected void activate(ComponentContext context) {
    Dictionary props = context.getProperties();
    registrationPath = (String) props.get(REGISTRATION_PATH);
    defaultDestination = (String) props.get(DEFAULT_DESTINATION);

    // we MUST register this servlet and not let it be picked up by Sling since we want to bypass
    // the normal security and simply trust the remote user value in the request.
    try {
      webContainer.registerServlet(registrationPath, this, null, this);
    } catch (NamespaceException e) {
      LOG.error(e.getMessage(), e);
      throw new ComponentException(e.getMessage(), e);
    } catch (ServletException e) {
      LOG.error(e.getMessage(), e);
      throw new ComponentException(e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="BC_VACUOUS_INSTANCEOF",justification="Could be injected from annother bundle")
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    
    if (trustedTokenService instanceof TrustedTokenServiceImpl) {
      ((TrustedTokenServiceImpl) trustedTokenService).injectToken(req, resp);
      LOGGER.debug(" Might have Injected token ");
      String destination = req.getParameter(PARAM_DESTINATION);

      if (destination == null) {
        destination = defaultDestination;
      }
      // ensure that the redirect is safe and not susceptible to
      resp.sendRedirect(destination.replace('\n', ' ').replace('\r', ' '));
    } else {
      LOGGER.debug("Trusted Token Service is not the correct implementation and so cant inject tokens. ");
    }
  }

  public String getMimeType(String mimetype) {
    return null;
  }

  public URL getResource(String name) {
    return getClass().getResource(name);
  }

  /**
   * (non-Javadoc) This servlet handles its own security since it is going to trust the
   * external remote user. If we dont do this the SLing handleSecurity takes over and causes problems.
   * 
   * @see org.osgi.service.http.HttpContext#handleSecurity(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  public boolean handleSecurity(HttpServletRequest arg0, HttpServletResponse arg1)
      throws IOException {
    return true;
  }

 
}
