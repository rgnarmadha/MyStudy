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
package org.sakaiproject.nakamura.docproxy;

import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_PROCESSOR;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.api.docproxy.DocProxyUtils;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;
import org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(
  name = "External Document Search Servlet",
  description = "Searches external documents",
  bindings = {
    @ServiceBinding(
      type = BindingType.TYPE,
      bindings = {"sakai/external-repository-search"},
      extensions = {@ServiceExtension(name = "json", description = "javascript object notation")}
    )
  },
  methods = @ServiceMethod(
    name = "GET",response = {
      @ServiceResponse(code = 200, description = "All processing finished successfully."),
      @ServiceResponse(code = 500, description = "Exception occurred during processing.")
    }
  )
)
@SlingServlet(resourceTypes = { "sakai/external-repository-search" }, generateComponent = true, generateService = true, extensions = { "json" })
public class ExternalDocumentSearchServlet extends SlingSafeMethodsServlet {

  private static final String SEARCH_DEFINITION_REGEX = "{(\\w+)(|(\\w+))?}";
protected ExternalRepositoryProcessorTracker tracker;
  protected static final Logger LOGGER = LoggerFactory
      .getLogger(ExternalDocumentSearchServlet.class);
  private static final long serialVersionUID = 8016289526361989976L;

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    try {
      // Grab the search node.
      Node node = request.getResource().adaptTo(Node.class);

      // Grab the node that holds the repository information.
      Node proxyNode = DocProxyUtils.getProxyNode(node);

      // Grab the correct processor
      String type = proxyNode.getProperty(REPOSITORY_PROCESSOR).getString();
      ExternalRepositoryProcessor processor = tracker.getProcessorByType(type);
      if (processor == null) {
        LOGGER.warn("No processor found for type - {}", type);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "Could not handle this repository type.");
        return;
      }

      // Handle properties.
      Map<String, Object> searchProperties = new HashMap<String, Object>();
      handleProperties(searchProperties, node, request);

      // Process search
      Iterator<ExternalDocumentResult> results = processor.search(proxyNode,
          searchProperties);

      // Do the default search paging.
      long toSkip = SearchUtil.getPaging(request);
      while (toSkip > 0) {
        if (results.hasNext()) {
          results.next();
          toSkip--;
        } else {
          throw new NoSuchElementException();
        }
      }

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");


      ExtendedJSONWriter write = new ExtendedJSONWriter(response.getWriter());
      write.array();
      long nitems = SearchUtil.longRequestParameter(request,
          SearchConstants.PARAMS_ITEMS_PER_PAGE, SearchConstants.DEFAULT_PAGED_ITEMS);
      for (long i = 0; i < nitems && results.hasNext(); i++) {
        ExternalDocumentResult result = results.next();
        DocProxyUtils.writeMetaData(write, result);
      }
      write.endArray();

    } catch (RepositoryException e) {
      LOGGER.error(
          "Got a repository exception when trying to grab search node information.", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to perform search.");
    } catch (JSONException e) {
      LOGGER
          .error("Got a JSON exception when trying to grab search node information.", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "Failed to perform search.");
    } catch (DocProxyException e) {
      LOGGER.error(
          "Got a DocProxy exception when trying to grab search node information.", e);
      response.sendError(e.getCode(), e.getMessage());
    }
  }

  /**
   * Fill's in the values for the search properties.
   *
   * @param searchProperties
   * @param vals
   * @param request
   * @throws RepositoryException
   */
  protected void handleProperties(Map<String, Object> searchProperties, Node node,
      SlingHttpServletRequest request) throws RepositoryException {
    PropertyIterator props = node.getProperties("sakai:search-prop-*");
    while (props.hasNext()) {
      Property p = props.nextProperty();
      handleProperty(searchProperties, p, request);
    }

  }

  /**
   * @param searchProperties
   * @param p
   * @param request
   * @throws RepositoryException
   */
  protected void handlePropertyWithRegex(Map<String, Object> searchProperties, Property p,
      SlingHttpServletRequest request) throws RepositoryException {
    String searchPropertyName = p.getName().replace("sakai:search-prop-", "");
    String searchPropertyValue = "";
    String searchPropertyDefintion = p.getString();
    String searchPropertyRequestParameterName =
	parseRequestParamName(searchPropertyDefintion);
    if (request.getParameter(searchPropertyRequestParameterName) != null) {
	searchPropertyValue = request.getParameter(searchPropertyRequestParameterName);
    } else {
	searchPropertyValue = parseSearchPropertyDefaultValue(searchPropertyDefintion);
    }

    searchProperties.put(searchPropertyName, searchPropertyValue);
  }

  private String parseRequestParamName(String searchPropertyDefintion) {
    // examples with and without a specified default value
    // {color} {color|black}
    Pattern searchDefinitionPattern = Pattern.compile(SEARCH_DEFINITION_REGEX);
    Matcher searchDefinitionMatcher = searchDefinitionPattern
        .matcher(searchPropertyDefintion);
    return searchDefinitionMatcher.group(1);
  }

  private String parseSearchPropertyDefaultValue(String searchPropertyDefinition) {
    Pattern searchDefinitionPattern = Pattern.compile(SEARCH_DEFINITION_REGEX);
    Matcher searchDefinitionMatcher = searchDefinitionPattern
        .matcher(searchPropertyDefinition);
    return searchDefinitionMatcher.group(3);
  }

	protected void handleProperty(Map<String, Object> searchProperties, Property p,
	      SlingHttpServletRequest request) throws RepositoryException {
    String searchPropertyName = p.getName().replace("sakai:search-prop-", "");
    String propertyValue = p.getString();

    StringBuilder sb = new StringBuilder();
    boolean escape = false;
    int vstart = -1;
    char[] ca = propertyValue.toCharArray();
    String defaultValue = null;
    for (int i = 0; i < ca.length; i++) {
      char c = ca[i];
      if (escape) {
        sb.append(c);
        escape = false;
      } else if (vstart >= 0) {
        if (c == '}') {
          String v = new String(ca, vstart + 1, i - vstart - 1);
          defaultValue = null;
          // Take care of default values
          if (v.contains("|")) {
            String[] val = v.split("\\|");
            v = val[0];
            defaultValue = val[1];
          }
          RequestParameter rp = request.getRequestParameter(v);
          if (rp != null) {
            sb.append(rp.getString());
          } else if (rp == null && defaultValue != null) {
            sb.append(defaultValue);
          }
          vstart = -1;
        }
      } else {
        switch (c) {
        case '{':
          vstart = i;
          break;
        case '\\':
          escape = true;
          break;
        default:
          sb.append(c);
        }
      }
    }

    searchProperties.put(searchPropertyName, sb.toString());
  }

  protected void activate(ComponentContext context) {
    BundleContext bundleContext = context.getBundleContext();
    tracker = new ExternalRepositoryProcessorTracker(bundleContext,
        ExternalRepositoryProcessor.class.getName(), null);
    tracker.open();
  }

  protected void deactivate(ComponentContext context) {
    if (tracker != null) {
      tracker.close();
      tracker = null;
    }
  }

}
