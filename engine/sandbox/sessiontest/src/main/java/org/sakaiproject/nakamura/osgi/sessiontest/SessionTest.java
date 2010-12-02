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

package org.sakaiproject.nakamura.osgi.sessiontest;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The <code>SessionTestServlet</code> TODO
 * 
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.component immediate="true"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="service.description" value="Simple servlet"
 * @scr.property name="sling.servlet.paths" value="/test/sessiontest"
 */
public class SessionTest extends HttpServlet {

  private static final Logger LOG = LoggerFactory.getLogger(SessionTest.class);
  private static final long serialVersionUID = 1L;

  protected void activate(ComponentContext context)
  {
     LOG.info("Component was activated. Context: " + context);
  }
  
  @SuppressWarnings("unchecked")
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    LOG.info("Got GET");
    resp.setStatus(HttpServletResponse.SC_OK);
    Enumeration<String> enu = req.getAttributeNames();
    while (enu.hasMoreElements())
    {
      String key = enu.nextElement();
      Object value = req.getAttribute(key);
      LOG.info("HEADER: " + key + ": " + value); 
    }
    PrintWriter writer = new PrintWriter(resp.getOutputStream());
    Session jcrSession = (Session) req.getAttribute("javax.jcr.Session");
    writer.write("<html><head><title>Test</title></head><body><h1>Test1</h1><p>SessionID: " + req.getSession().getId()
        + "</p><p>Text was: " + req.getSession().getAttribute("text") + "</p>");
    writer.write("<p>Session is:" + jcrSession + "</p>");
    if (jcrSession != null)
    {
      writer.write("<p>User is:" + jcrSession.getUserID() + "</p>");
    }
    writer.write("<form method='post'><input name='text'/><input type='submit' value='go'/></form>");
    writer.write("</body></html>");
    writer.close();
  }
  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    String text = req.getParameter("text");
    LOG.info("'" + text + "' was posted");
    req.getSession().setAttribute("text", text);
    resp.setHeader("Location", req.getRequestURL().toString());
    resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
    PrintWriter writer = new PrintWriter(resp.getOutputStream());
    writer.write("<html><body><p>Redirected!</p></body></html>");
    writer.close();
  }

  protected void deactivate(ComponentContext context)
  {
    LOG.info("Component was deactivated. Context: " + context);
  }
  
}
