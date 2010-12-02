/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.user.servlet;


import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * Sling GET Servlet for checking for the existence of a user.
 * </p>
 * <h2>REST Service Description</h2>
 * <p>
 * Checks for the existence of a user with the given id. This servlet responds at
 * <code>/system/userManager/user.exists.html?userid=</code>
 * </p>
 * <h4>Methods</h4>
 * <ul>
 * <li>GET</li>
 * </ul>
 * <h4>GET Parameters</h4>
 * <dl>
 * <dt>userid</dt>
 * <dd>The name of the user to check for</dd>
 * <dt>*</dt>
 * <dd>Any additional parameters become properties of the group node (optional)</dd>
 * </dl>
 * <h4>Response</h4>
 * <dl>
 * <dt>204</dt>
 * <dd>Success, user exists.</dd>
 * <dt>404</dt>
 * <dd>User does not exist.</dd>
 * </dl>
 * <h4>Example</h4>
 *
 * <code>
 * curl http://localhost:8080/system/userManager/user.exists.html?userid=foo
 * </code>
 *
 * <h4>Notes</h4>
 */
@ServiceDocumentation(name="User Exists Servlet",
    description="Tests for existence of user. This servlet responds at /system/userManager/user.exists.html",
    shortDescription="Tests for existence of user",
    bindings=@ServiceBinding(type=BindingType.PATH,bindings="/system/userManager/user.exists.html",
        selectors=@ServiceSelector(name="exists", description="Tests for existence of user."),
        extensions=@ServiceExtension(name="html", description="GETs produce HTML with request status.")),
    methods=@ServiceMethod(name="GET",
        description={"Checks for existence of user with id supplied in the userid parameter."},
        parameters={
        @ServiceParameter(name="userid", description="The id of the user to check for (required)")},
        response={
        @ServiceResponse(code=204,description="Success, user exists."),
        @ServiceResponse(code=404,description="Bad request: the required userid parameter was missing.")
        }))
@Component(immediate=true, metatype=true, label="Sakai Nakamura :: User Existence Check Servlet",
    description="Returns 204 if userid exists, 404 if not")
@Service(value=javax.servlet.Servlet.class)
@Properties(value = {
    @Property(name="sling.servlet.resourceTypes", value="sling/users"),
    @Property(name="sling.servlet.methods", value="GET"),
    @Property(name="sling.servlet.selectors", value="exists")
})
public class UserExistsServlet extends SlingSafeMethodsServlet {
  private static final long serialVersionUID = 7051557537133012560L;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(UserExistsServlet.class);

  @Property(label="Delay (MS)",
      description="Number of milliseconds to delay before responding; 0 to return as quickly as possible",
      longValue=UserExistsServlet.USER_EXISTS_DELAY_MS_DEFAULT)
  public static final String USER_EXISTS_DELAY_MS_PROPERTY = "user.exists.delay.ms";
  public static final long USER_EXISTS_DELAY_MS_DEFAULT = 200;
  protected long delayMs;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    long start = System.currentTimeMillis();
    try {
      Session session = request.getResourceResolver().adaptTo(Session.class);
      RequestParameter idParam = request.getRequestParameter("userid");
      if (idParam == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "This request must have a 'userid' parameter.");
        return;
      }

      if ("".equals(idParam)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "The 'userid' parameter must not be blank.");
        return;
      }
      String id = idParam.getString();
      LOGGER.debug("Checking for existence of {}", id);
      if (session != null) {
          UserManager userManager = AccessControlUtil.getUserManager(session);
          if (userManager != null) {
              Authorizable authorizable = userManager.getAuthorizable(id);
              if (authorizable != null) {
                  response.setStatus(HttpServletResponse.SC_NO_CONTENT);
              } else response.sendError(HttpServletResponse.SC_NOT_FOUND);
          }
      }
    } catch (Exception e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
      return;
    } finally {
      LOGGER.debug("checking for existence took {} ms", System.currentTimeMillis() - start);
      if (delayMs > 0) {
        long remainingTime = delayMs - (System.currentTimeMillis() - start);
        if (remainingTime > 0) {
          try {
            Thread.sleep(remainingTime);
          } catch (InterruptedException e) {
          }
        }
      }
    }
  }

  @Activate @Modified
  protected void modified(Map<?, ?> props) {
    delayMs = OsgiUtil.toLong(props.get(USER_EXISTS_DELAY_MS_PROPERTY),
        USER_EXISTS_DELAY_MS_DEFAULT);
  }
}
