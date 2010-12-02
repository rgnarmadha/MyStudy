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
package org.sakaiproject.nakamura.cluster;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.cluster.ClusterServer;
import org.sakaiproject.nakamura.api.cluster.ClusterTrackingService;
import org.sakaiproject.nakamura.api.cluster.ClusterUser;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.proxy.ProxyClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>
 * This REST end point is restricted to users that can read the resource and optionally to
 * requests that have embeded a shared trusted token in their request. It is presented
 * with a user cookie, and responds with the user object for that cookie.
 * </p>
 * <p>
 * Trusted tokens are stored in the multi value property
 * <code>sakai:shared-token<code> and, if this is present,
 * requests must provide one of those tokens in the http header <code>Sakai-Trust-Token</code>
 * .
 * </p>
 * <p>
 * The servlet translates the cookie SAKAI-TRACKING on client requests into a User object.
 * This cookie is provided in a request parameter <code>c</code>
 * </p>
 * <p>
 * The response is of the form:
 * </p>
 *
 * <pre>
 * {
 *   &quot;server&quot;: &quot;16935@x43543-2.local&quot;,  // the server generating the response
 *   &quot;user&quot;: {
 *     &quot;lastUpdate&quot;: 1253499599589,
 *     &quot;homeServer&quot;: &quot;otherServerId&quot;,
 *     &quot;id&quot;: &quot;ieb&quot;,
 *     &quot;principal&quot;: &quot;principal:ieb&quot;,
 *     &quot;properties&quot;: {
 *       &quot;prop1&quot;: [
 *         &quot;tokenA&quot;,
 *         &quot;tokenB&quot;,
 *         &quot;tokenC&quot;
 *       ],
 *       &quot;prop2&quot;: &quot;tokenA&quot;,
 *       &quot;prop3&quot;: [
 *       ]
 *     },
 *     &quot;principals&quot;: [
 *       &quot;principal:A&quot;,
 *       &quot;principal:B&quot;
 *     ],
 *     &quot;declaredMembership&quot;: [
 *       &quot;group:A&quot;,
 *       &quot;group:B&quot;
 *     ],
 *     &quot;membership&quot;: [
 *       &quot;indirectgroup:A&quot;,
 *       &quot;indirectgroup:B&quot;
 *     ]
 *   }
 * }
 * </pre>
 */
@SlingServlet(generateComponent = true, generateService = true, selectors = { "cookie" }, extensions = { "json" }, resourceTypes = { "sakai/cluster-users" })
@ServiceDocumentation(name = "ClusterUserServlet", shortDescription = "Translates the value of cookie SAKAI-TRACKING into a User object.", description = "Translates the value of cookie SAKAI-TRACKING into a User object. This REST end point is restricted to users that can read the resource and optionally to requests that have embeded a shared trusted token in their request. It is presented with a user cookie, and responds with the user object for that cookie. Trusted tokens are stored in the multi value property sakai:shared-token and, if this is present, requests must provide one of those tokens in the http header Sakai-Trust-Token.", bindings = { @ServiceBinding(type = BindingType.TYPE, bindings = "sakai/cluster-users", selectors = { @ServiceSelector(name = "cookie", description = "") }, extensions = { @ServiceExtension(name = "json", description = "") }) }, methods = { @ServiceMethod(name = "GET", description = "<p>Sample JSON response:</p><pre>"
    + "curl http://localhost:8080/var/cluster/user.cookie.json?c=8070-10-87-32-111.localhost.indiana.edu-c8029d4b68a88a0e3aa3d0f60ff7de5530295cf1"
    + "{\n"
    + "  \"server\": \"8070-10-87-32-111.localhost.indiana.edu\",\n"
    + "  \"user\": {\n"
    + "    \"lastUpdate\": 1259875554162,\n"
    + "    \"homeServer\": \"8070-10-87-32-111.localhost.indiana.edu\",\n"
    + "    \"id\": \"admin\",\n"
    + "    \"principal\": \"admin\",\n"
    + "    \"properties\": {\n"
    + "      \"firstName\": \"Lance\",\n"
    + "      \"rep:userId\": \"admin\",\n"
    + "      \"email\": \"lance@foo.bar\",\n"
    + "      \"lastName\": \"Speelmon\",\n"
    + "      \"rep:groups\": \"1a94507c-232a-4ae5-a042-50b5608a0460\",\n"
    + "      \"rep:principalName\": \"admin\",\n"
    + "      \"jcr:primaryType\": \"rep:User\"\n"
    + "    },\n"
    + "    \"principals\": [\n"
    + "      \"admin\"\n"
    + "    ],\n"
    + "    \"declaredMembership\": [\n"
    + "      \"administrators\"\n"
    + "    ],\n"
    + "    \"membership\": [\n"
    + "      \"administrators\"\n"
    + "    ]\n"
    + "}"
    + "</pre>", parameters = { @ServiceParameter(name = "c", description = { "The value of cookie SAKAI-TRACKING." }) }, response = {
    @ServiceResponse(code = 200, description = "On sucess a JSON tree of the User object."),
    @ServiceResponse(code = 400, description = "Cookie is not provided in the request."),
    @ServiceResponse(code = 404, description = "Cookie is not registered."),
    @ServiceResponse(code = 0, description = "Any other status codes returned have meanings as per the RFC") }) })
public class ClusterUserServlet extends SlingSafeMethodsServlet {

  // TODO: deny doesnt work on the /var/cluster/user node for some reason, check the acl
  // etc.

  /**
   *
   */
  private static final long serialVersionUID = 5013072672247175850L;
  /**
   * The logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUserServlet.class);

  @Reference
  private transient ClusterTrackingService clusterTrackingService;

  @Reference
  private transient ProxyClientService proxyClientService;

  private transient UserManager testingUserManager;
  private Set<String> blacklist = new HashSet<String>();
  private HttpClient httpClient;
  protected boolean testing = false;

  public ClusterUserServlet() {
    initBlacklist();
  }

  /**
   * Constructor for testing purposes only.
   *
   * @param clusterTrackingService
   */
  protected ClusterUserServlet(ClusterTrackingService clusterTrackingService,
      UserManager userManager) {
    this.clusterTrackingService = clusterTrackingService;
    this.testingUserManager = userManager;
    initBlacklist();
  }

  protected void activate(ComponentContext componentContext) {
    httpClient = new HttpClient(proxyClientService.getHttpConnectionManager());
  }

  /**
   *
   */
  private void initBlacklist() {
    blacklist.add("jcr:uuid");
    blacklist.add("rep:password");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @SuppressWarnings("unchecked")
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      Node node = request.getResource().adaptTo(Node.class);

      String trackingCookie = request.getParameter("c");

      if (StringUtils.isEmpty(trackingCookie)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Must provide cookie to check.");
        return;
      }

      ClusterUser clusterUser = clusterTrackingService.getUser(trackingCookie);
      if (clusterUser == null) {

        if (!testing) {
          // work out the remote server and try there.
          ClusterServer clusterServer = clusterTrackingService.getServer(trackingCookie);
          if (clusterServer == null
              || clusterServer.getServerId() == clusterTrackingService.getCurrentServerId()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND,
                "Cookie could not be found");
            return;
          }
          GetMethod method = new GetMethod(clusterServer.getSecureUrl() + node.getPath()
              + ".cookie.json?c=" + URLEncoder.encode(trackingCookie, "UTF-8"));
          method.setFollowRedirects(true);
          method.setDoAuthentication(false);
          for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames
              .hasMoreElements();) {
            String headerName = headerNames.nextElement();
            for (Enumeration<String> headerValue = request.getHeaders(headerName); headerValue
                .hasMoreElements();) {
              method.addRequestHeader(headerName, headerValue.nextElement());
            }
          }
          try {
            int status = httpClient.executeMethod(method);

            if (status == 200) {
              OutputStream out = response.getOutputStream();
              out.write(method.getResponseBody());
              return;
            }
          } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
          }
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Cookie is not registered");
        return;
      }

      String serverId = clusterTrackingService.getCurrentServerId();
      UserManager userManager = null;
      if (this.testingUserManager != null) {
        userManager = testingUserManager;
      } else {
        userManager = AccessControlUtil.getUserManager(node.getSession());
      }
      User user = (User) userManager.getAuthorizable(clusterUser.getUser());
      JSONWriter jsonWriter = new JSONWriter(response.getWriter());
      jsonWriter.setTidy(true);
      jsonWriter.object();
      jsonWriter.key("server").value(serverId); // server

      jsonWriter.key("user").object();
      jsonWriter.key("lastUpdate").value(clusterUser.getLastModified());
      jsonWriter.key("homeServer").value(clusterUser.getServerId());
      jsonWriter.key("id").value(user.getID());
      jsonWriter.key("principal").value(user.getPrincipal().getName());
      jsonWriter.key("properties").object();
      for (Iterator<?> pi = user.getPropertyNames(); pi.hasNext();) {
        String propertyName = (String) pi.next();
        if (!blacklist.contains(propertyName)) {
          jsonWriter.key(propertyName);
          Value[] propertyValues = user.getProperty(propertyName);
          if (propertyValues.length == 1) {
            jsonWriter.value(propertyValues[0].getString());
          } else {
            jsonWriter.array();
            for (Value v : propertyValues) {
              jsonWriter.value(v.getString());
            }
            jsonWriter.endArray();
          }
        }
      }
      jsonWriter.endObject(); // properties


      jsonWriter.key("declaredMembership").array();
      for (Iterator<?> gi = user.declaredMemberOf(); gi.hasNext();) {
        jsonWriter.value(((Authorizable) gi.next()).getID());
      }
      jsonWriter.endArray();

      jsonWriter.key("membership").array();
      for (Iterator<?> gi = user.memberOf(); gi.hasNext();) {
        jsonWriter.value(((Authorizable) gi.next()).getID());
      }
      jsonWriter.endArray();

      jsonWriter.endObject(); // user
      jsonWriter.endObject();

    } catch (RepositoryException e) {
      LOGGER.error("Failed to get users " + e.getMessage(), e);
      throw new ServletException(e.getMessage());
    } catch (JSONException e) {
      LOGGER.error("Failed to get users " + e.getMessage(), e);
      throw new ServletException(e.getMessage());
    }
  }
}
