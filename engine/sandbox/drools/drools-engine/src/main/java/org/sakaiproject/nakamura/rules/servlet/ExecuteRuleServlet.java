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

package org.sakaiproject.nakamura.rules.servlet;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.rules.RuleConstants;
import org.sakaiproject.nakamura.api.rules.RuleContext;
import org.sakaiproject.nakamura.api.rules.RuleExecutionErrorListener;
import org.sakaiproject.nakamura.api.rules.RuleExecutionException;
import org.sakaiproject.nakamura.api.rules.RuleExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@SlingServlet(extensions = { "json" }, methods = { "GET" }, resourceTypes = { "sling/servlet/default" }, selectors = { "execute-rule" })
@ServiceDocumentation(description = { "This servlet enables the execution of a rule on any resource within the system. The response will"
    + " be the resut map produced by the rule execution. The service has one significant parameter :ruleset which is either an absolute path to the rule set"
    + " or the name of the rule set defined by the property sakai:rule-set-name on the rule set node." })
public class ExecuteRuleServlet extends SlingSafeMethodsServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -6945945062850448064L;

  private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteRuleServlet.class);

  protected static final String PARAM_RULE_SET = ":ruleset";

  @Reference
  protected RuleExecutionService ruleExecutionService;

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    Resource resource = request.getResource();
    RuleContext ruleContext = new RequestRuleContext(request, response);
    RuleExecutionErrorListener servletErrorListener = new ServletRuleErrorListener();

    String pathToRuleSet = request.getParameter(PARAM_RULE_SET);
    if (pathToRuleSet != null) {
      if (pathToRuleSet.charAt(0) != '/') {

        // we need to fidn the rule set by name
        try {
          Session session = request.getResourceResolver().adaptTo(Session.class);
          ValueFactory valueFactory = session.getValueFactory();
          QueryManager qm = session.getWorkspace().getQueryManager();
          Query q = qm.createQuery("select * from test where "
              + RuleConstants.PROP_SAKAI_RULE_SET_NAME + " = $ruleSetName ",
              Query.JCR_SQL2);
          q.bindValue("ruleSetName", valueFactory.createValue(pathToRuleSet));
          QueryResult qr = q.execute();
          RowIterator ri = qr.getRows();
          if (ri.hasNext()) {
            Node node = ri.nextRow().getNode();
            pathToRuleSet = node.getPath();
          } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "Cant locate named rule " + pathToRuleSet);
            return;
          }
        } catch (RepositoryException e) {
          LOGGER.error(e.getMessage(), e);
          response.sendError(HttpServletResponse.SC_BAD_REQUEST,
              "Cant locate named rule " + pathToRuleSet + " :" + e.getMessage());
          return;
        }
      }
      try {
        try {
          Map<String, Object> result = ruleExecutionService.executeRuleSet(pathToRuleSet,
              request, resource, ruleContext, servletErrorListener);
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");
          JSONObject o = new JSONObject(result);
          response.getWriter().append(o.toString());

        } catch (RuleExecutionException e) {
          LOGGER.error(e.getMessage(), e);

          List<String> errors = e.getErrors();
          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          response.setContentType("application/json");
          response.setCharacterEncoding("UTF-8");

          JSONWriter jsonWriter = new JSONWriter(response.getWriter());
          jsonWriter.object();
          jsonWriter.key("errors");
          jsonWriter.value(new JSONArray(errors));
          jsonWriter.key("exception");
          jsonWriter.value(e.getMessage());
          jsonWriter.endObject();
        }
      } catch (JSONException e) {
        e.printStackTrace();
        throw new ServletException(e.getMessage(), e);
      }
    } else {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "You must specify a rule to execute on this resource in the parameter"
              + PARAM_RULE_SET);
    }
  }

}
