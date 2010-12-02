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
package org.sakaiproject.nakamura.files.servlets;

import org.apache.commons.lang.CharEncoding;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceExtension;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.files.FileUtils;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.files.search.FileSearchBatchResultProcessor;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import java.io.IOException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

@ServiceDocumentation(name = "TagServlet", shortDescription = "Get information about a tag.",
    description = {
      "This servlet is able to give all the necessary information about tags.",
      "It's able to give json feeds for the childtags, parent tags or give a dump of the files who are tagged with this tag.",
      "Must specify a selector of children, parents, tagged. tidy, {number} are optional and ineffective by themselves."
    },
    bindings = {
      @ServiceBinding(type = BindingType.TYPE, bindings = { "sakai/tag" },
          extensions = @ServiceExtension(name = "json", description = "This servlet outputs JSON data."),
          selectors = {
            @ServiceSelector(name = "children", description = "Will dump all the children of this tag."),
            @ServiceSelector(name = "parents", description = "Will dump all the parents of this tag."),
            @ServiceSelector(name = "tagged", description = "Will dump all the files who are tagged with this tag."),
            @ServiceSelector(name = "tidy", description = "Optional sub-selector. Will send back 'tidy' output."),
            @ServiceSelector(name = "{number}", description = "Optional sub-selector. Specifies the depth of data to output.")
          }
      )
    },
    methods = {
      @ServiceMethod(name = "GET",  parameters = {},
          description = { "This servlet only responds to GET requests." },
          response = {
            @ServiceResponse(code = 200, description = "Succesful request, json can be found in the body"),
            @ServiceResponse(code = 500, description = "Failure to retrieve tags or files, an explanation can be found in the HTMl.")
          }
      )
    }
)
@SlingServlet(extensions = { "json" }, generateComponent = true, generateService = true,
    methods = { "GET" }, resourceTypes = { "sakai/tag" },
    selectors = {"children", "parents", "tagged" }
)
@Properties(value = {
    @Property(name = "service.description", value = "Provides support for file tagging."),
    @Property(name = "service.vendor", value = "The Sakai Foundation")
})
public class TagServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = -8815248520601921760L;

  @Reference
  protected transient SiteService siteService;

  private transient FileSearchBatchResultProcessor proc;

  @Reference
  protected transient SearchServiceFactory searchServiceFactory;

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    // digest the selectors to determine if we should send a tidy result
    // or if we need to traverse deeper into the tagged node.
    boolean tidy = false;
    int depth = 0;
    String[] selectors = request.getRequestPathInfo().getSelectors();
    String selector = null;
    for (String sel : selectors) {
      if ("tidy".equals(sel)) {
        tidy = true;
      } else if ("infinity".equals(sel)) {
        depth = -1;
      } else {
        // check if the selector is telling us the depth of detail to return
        Integer d = null;
        try { d = Integer.parseInt(sel); } catch (NumberFormatException e) {}
        if (d != null) {
          depth = d;
        } else {
          selector = sel;
        }
      }
    }

    JSONWriter write = new JSONWriter(response.getWriter());
    write.setTidy(tidy);
    Node tag = request.getResource().adaptTo(Node.class);

    try {
      if ("children".equals(selector)) {
        sendChildren(tag, write);
      } else if ("parents".equals(selector)) {
        sendParents(tag, write);
      } else if ("tagged".equals(selector)) {
        sendFiles(tag, request, write, depth);
      }
      response.setContentType("application/json");
      response.setCharacterEncoding(CharEncoding.UTF_8);
    } catch (JSONException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (RepositoryException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (SearchException e) {
      response.sendError(e.getCode(), e.getMessage());
    }

  }

  /**
   * @param tag
   * @param request
   * @throws RepositoryException
   * @throws JSONException
   * @throws SearchException
   */
  protected void sendFiles(Node tag, SlingHttpServletRequest request, JSONWriter write,
      int depth) throws RepositoryException, JSONException, SearchException {
    // We expect tags to be referencable, if this tag is not..
    // it will throw an exception.
    String uuid = tag.getIdentifier();

    // Tagging on any item will be performed by adding a weak reference to the content
    // item. Put simply a sakai:tag-uuid property with the UUID of the tag node. We use
    // the UUID to uniquely identify the tag in question, a string of the tag name is not
    // sufficient. This allows the tag to be renamed and moved without breaking the
    // relationship.
    String statement = "//*[@sakai:tag-uuid='" + uuid + "']";
    Session session = tag.getSession();
    QueryManager qm = session.getWorkspace().getQueryManager();
    Query query = qm.createQuery(statement, Query.XPATH);

    // For good measurement
    if (proc == null) {
      proc = new FileSearchBatchResultProcessor(siteService, searchServiceFactory);
    }
    proc.setDepth(depth);

    SearchResultSet rs = proc.getSearchResultSet(request, query);
    write.array();
    proc.writeNodes(request, write, null, rs.getRowIterator());
    write.endArray();
  }

  /**
   * Write all the parent tags of the passed in tag.
   *
   * @param tag
   *          The tag that should be sent and get it's children parsed.
   * @param write
   *          The JSONWriter to write to
   * @throws JSONException
   * @throws RepositoryException
   */
  protected void sendParents(Node tag, JSONWriter write) throws JSONException,
      RepositoryException {
    write.object();
    ExtendedJSONWriter.writeNodeContentsToWriter(write, tag);
    write.key("parent");
    try {
      Node parent = tag.getParent();
      if (FileUtils.isTag(parent)) {
        sendParents(parent, write);
      } else {
        write.value(false);
      }
    } catch (ItemNotFoundException e) {
      write.value(false);
    }

    write.endObject();
  }

  /**
   * Write all the child tags of the passed in tag.
   *
   * @param tag
   *          The tag that should be sent and get it's children parsed.
   * @param write
   *          The JSONWriter to write to
   * @throws JSONException
   * @throws RepositoryException
   */
  protected void sendChildren(Node tag, JSONWriter write) throws JSONException,
      RepositoryException {

    write.object();
    ExtendedJSONWriter.writeNodeContentsToWriter(write, tag);
    write.key("children");
    write.array();
    NodeIterator iterator = tag.getNodes();
    while (iterator.hasNext()) {
      Node node = iterator.nextNode();
      if (FileUtils.isTag(node)) {
        sendChildren(node, write);
      }
    }
    write.endArray();
    write.endObject();

  }
}
