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
package org.sakaiproject.nakamura.site.search;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.RowUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;

@Component(immediate = true, name = "ContentSearchResultProcessor", label = "ContentSearchResultProcessor")
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Formats search results for content nodes in sites."),
    @Property(name = "sakai.search.processor", value = "Content") })
@Service(value = SearchResultProcessor.class)
public class ContentSearchResultProcessor implements SearchResultProcessor {

  private SearchResultProcessorTracker tracker;

  @SuppressWarnings(value = "NP_UNWRITTEN_FIELD", justification = "Injected by OSGi")
  @Reference
  private transient SearchServiceFactory searchServiceFactory;

  public void writeNode(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, Row row) throws JSONException, RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node node = RowUtils.getNode(row, session);
    if (node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
      String type = node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString();

      // From looking at the type we determine how we should represent this node.
      SearchResultProcessor processor = tracker
          .getSearchResultProcessorByType(type);
      if (processor != null) {
        write.object();
        write.key("path");
        write.value(node.getPath());
        write.key("type");
        write.value(node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString());
        write.key("excerpt");
        write.value(RowUtils.getDefaultExcerpt(row));
        write.key("data");
        processor.writeNode(request, write, aggregator, row);
        write.endObject();
      } else {
        // No processor found, just dump the properties
        dumpProperties(request, write, aggregator, row);
      }

    } else {
      // No type, just dump the properties
      dumpProperties(request, write, aggregator, row);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SearchException {
    return searchServiceFactory.getSearchResultSet(request, query);
  }

  private void dumpProperties(SlingHttpServletRequest request,
      JSONWriter write, Aggregator aggregator, Row row) throws JSONException,
      RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node node = RowUtils.getNode(row, session);
    if (aggregator != null) {
      aggregator.add(node);
    }
    write.object();
    write.key("path");
    write.value(node.getPath());
    write.key("excerpt");
    write.value(RowUtils.getDefaultExcerpt(row));
    write.key("data");
    ExtendedJSONWriter.writeNodeToWriter(write, node);
    write.endObject();
  }

  protected void activate(ComponentContext context) {
    BundleContext bundleContext = context.getBundleContext();
    tracker = new SearchResultProcessorTracker(bundleContext);
    tracker.open();
  }

  protected void deactivate(ComponentContext context) {
    if (tracker != null) {
      tracker.close();
      tracker = null;
    }
  }

}
