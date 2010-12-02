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
import static org.sakaiproject.nakamura.api.search.SearchConstants.PARAMS_ITEMS_PER_PAGE;

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
import org.sakaiproject.nakamura.api.search.SearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.RowUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

@Component(immediate = true, name = "SiteContentSearchResultProcessor", label = "SiteContentSearchResultProcessor")
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Formats search results for content nodes in sites."),
    @Property(name = "sakai.search.batchprocessor", value = "SiteContent") })
@Service(value = SearchBatchResultProcessor.class)
@Reference(referenceInterface = SiteService.class, name = "SiteService")
public class SiteContentSearchResultProcessor implements SearchBatchResultProcessor {

  private SiteService siteService;
  protected SearchResultProcessorTracker tracker;

  @SuppressWarnings(value = "NP_UNWRITTEN_FIELD", justification = "Injected by OSGi")
  @Reference
  private transient SearchServiceFactory searchServiceFactory;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchBatchResultProcessor#writeNodes(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter,
   *      org.sakaiproject.nakamura.api.search.Aggregator, javax.jcr.query.RowIterator)
   */
  public void writeNodes(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, RowIterator iterator) throws JSONException,
      RepositoryException {

    long toSkip = SearchUtil.getPaging(request);
    iterator.skip(toSkip);
    long items = SearchUtil.longRequestParameter(request, PARAMS_ITEMS_PER_PAGE,
        SearchConstants.DEFAULT_PAGED_ITEMS);

    long i = 0;
    while (iterator.hasNext() && i < items) {
      Row row = iterator.nextRow();
      writeNode(request, write, aggregator, row);
      i++;
    }

  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request, Query query)
      throws SearchException {
    try {
      // Perform the query
      QueryResult qr = query.execute();
      RowIterator iterator = qr.getRows();

      // Do another query to get the files.
      Session session = request.getResourceResolver().adaptTo(Session.class);
      QueryManager qm = session.getWorkspace().getQueryManager();
      String statement = "//element(*, sakai:site)//*[@sling:resourceType='sakai/link']/jcr:deref(@jcr:reference, '*')[jcr:contains(.,'*u*')] order by @jcr:score descending";
      Query q = qm.createQuery(statement, Query.XPATH);
      QueryResult filesQueryResult = q.execute();
      RowIterator filesIterator = filesQueryResult.getRows();

      RowIterator mergedIterator = searchServiceFactory.getMergedRowIterator(iterator, filesIterator);

      return searchServiceFactory.getSearchResultSet(mergedIterator);

    } catch (RepositoryException e) {
      throw new SearchException(500, "Unable to do files query.");
    }
  }

  private void writeDefaultNode(JSONWriter write, Aggregator aggregator, Row row,
      Node siteNode, Session session) throws JSONException, RepositoryException {
    Node node = RowUtils.getNode(row, session);
    if (aggregator != null) {
      aggregator.add(node);
    }
    write.object();
    // We don't dump the member-count, since that is very expensive.
    write.key("site");
    ExtendedJSONWriter.writeNodeToWriter(write, siteNode);
    write.key("excerpt");
    write.value(RowUtils.getDefaultExcerpt(row));
    write.key("data");
    ExtendedJSONWriter.writeNodeToWriter(write, node);
    write.endObject();
  }

  private void writeNode(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, Row row) throws JSONException, RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node node = RowUtils.getNode(row, session);

    Node siteNode = node;
    boolean foundSite = false;
    while (!siteNode.getPath().equals("/")) {

      if (siteService.isSite(siteNode)) {
        foundSite = true;
        break;
      }
      siteNode = siteNode.getParent();
    }
    if (foundSite) {
      if (node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
        String type = node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString();

        // From looking at the type we determine how we should represent this node.
        SearchResultProcessor processor = tracker.getSearchResultProcessorByType(type);
        if (processor != null) {
          write.object();
          write.key("site");
          ExtendedJSONWriter.writeNodeToWriter(write, siteNode);
          write.key("type");
          write.value(node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString());
          write.key("excerpt");
          write.value(RowUtils.getDefaultExcerpt(row));
          write.key("data");
          processor.writeNode(request, write, aggregator, row);
          write.endObject();
        } else {
          // No processor found, just dump the properties
          writeDefaultNode(write, aggregator, row, siteNode, session);
        }

      } else {
        // No type, just dump the properties
        writeDefaultNode(write, aggregator, row, siteNode, session);
      }
    }
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

  protected void bindSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  protected void unbindSiteService(SiteService siteService) {
    this.siteService = null;
  }

}
