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
package org.sakaiproject.nakamura.scriptrunner;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * This servlet will proxy through to other websites and fetch its data.
 * 
 * @scr.component immediate="true" label="%scriptrunner.get.operation.name"
 *                description="%scriptrunner.get.operation.description"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="sling.servlet.resourceTypes" value="sling/servlet/default"
 * @scr.property name="sling.servlet.selectors" value="runscript"
 * @scr.property name="sling.servlet.methods" value="GET"
 */
public class ScriptRunnerServlet extends SlingAllMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -2592752610533380047L;

  /**
   * Perform the actual request. {@inheritDoc}
   * 
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  protected void doGet(SlingHttpServletRequest req,
      SlingHttpServletResponse resp) throws IOException {

    Servlet servlet = ((SlingHttpServletRequest) req).getResource().adaptTo(Servlet.class);
    if (servlet != null) {
      try {
        servlet.service((ServletRequest) req, (ServletResponse) resp);
      } catch (ServletException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    } else {
      // send error
      PrintWriter p = resp.getWriter();
      p.print("Failed miserably");
      p.flush();
    }
    
		return;   

  }

}
