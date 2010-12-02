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
package org.sakaiproject.nakamura.files.search;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.search.SearchConstants.PARAMS_ITEMS_PER_PAGE;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.files.FileUtils;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.util.RowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * Formats the files search results.
 *
 */

@Component(immediate = true, label = "FileSearchBatchResultProcessor", description = "Formatter for file searches")
@Service(value = SearchBatchResultProcessor.class)
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.batchprocessor", value = "Files") })
public class FileSearchBatchResultProcessor implements SearchBatchResultProcessor {

  public static final Logger LOGGER = LoggerFactory
      .getLogger(FileSearchBatchResultProcessor.class);

  @Reference
  protected transient SiteService siteService;

  @Reference
  private SearchServiceFactory searchServiceFactory;

  // how deep to traverse the file structure
  private int depth = 0;

  /**
   * @param siteService
   */
  public FileSearchBatchResultProcessor(SiteService siteService,
      SearchServiceFactory searchServiceFactory) {
    this.siteService = siteService;
    this.searchServiceFactory = searchServiceFactory;
  }

  public FileSearchBatchResultProcessor() {
  }

  public void setDepth(int depth) {
    this.depth = depth;
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
      // Get the query result.
      QueryResult rs = query.execute();

      // Extract the total hits from lucene
      long nitems = SearchUtil.longRequestParameter(request, PARAMS_ITEMS_PER_PAGE,
          SearchConstants.DEFAULT_PAGED_ITEMS);

      // Do the paging on the iterator.
      RowIterator iterator = searchServiceFactory.getPathFilteredRowIterator(rs.getRows());
      long start = SearchUtil.getPaging(request);
      iterator.skip(start);

      Session session = request.getResourceResolver().adaptTo(Session.class);

      long i = start;
      List<Row> savedRows = new ArrayList<Row>();
      List<String> processedResults = new ArrayList<String>();
      // Loop over the rows
      while (i < (start + nitems) && iterator.hasNext()) {
        // Grab the next row and node.
        Row row = iterator.nextRow();
        Node node = RowUtils.getNode(row, session);
        String path = node.getPath();

        // We only check nt:file's no nt:resource (those are just children anyway)
        if (node.getProperty(JcrConstants.JCR_PRIMARYTYPE).getString().equals(
            JcrConstants.NT_RESOURCE)) {
          node = node.getParent();
        }

        // We're not interested in OS X's dummy/hidden files.
        String name = node.getName();
        if (name.startsWith(".")) {
          continue;
        }

        // If by some magic we already processed this path, we ignore it.
        if (processedResults.contains(path)) {
          continue;
        }

        // Remember this node.
        processedResults.add(path);
        savedRows.add(row);
        i++;
      }

      RowIterator newIterator = searchServiceFactory.getRowIteratorFromList(savedRows);

      // Return the result set.
      SearchResultSet srs = searchServiceFactory.getSearchResultSet(newIterator);
      return srs;
    } catch (RepositoryException e) {
      throw new SearchException(500, "Unable to perform query.");
    }
  }

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
    Session session = request.getResourceResolver().adaptTo(Session.class);

    while (iterator.hasNext()) {
      Row row = iterator.nextRow();
      Node node = RowUtils.getNode(row, session);
      if (aggregator != null) {
        aggregator.add(node);
      }

      handleNode(node, session, write);
    }
  }

  /**
   * Write the nodes in the node iterator confirming this batchprocessor default output.
   *
   * @param request
   *          The request.
   * @param write
   *          The {@link JSONWriter} to output to.
   * @param iterator
   *          The {@link NodeIterator} to use.
   * @param start
   *          Where we have to start (this will skip the specified amount on the iterator)
   * @param end
   *          The point where we should end.
   * @throws RepositoryException
   * @throws JSONException
   */
  public void writeNodes(SlingHttpServletRequest request, JSONWriter write,
      NodeIterator iterator, long start, long end) throws RepositoryException,
      JSONException {

    Session session = request.getResourceResolver().adaptTo(Session.class);
    iterator.skip(start);
    for (long i = start; i < end && iterator.hasNext(); i++) {
      Node node = iterator.nextNode();
      handleNode(node, session, write);
    }
  }

  /**
   * Give a JSON representation of the file node.
   *
   * @param node
   *          The node
   * @param session
   *          The {@link Session} to use to grab more information.
   * @param write
   *          The {@link JSONWriter} to use.
   * @throws JSONException
   * @throws RepositoryException
   */
  protected void handleNode(Node node, Session session, JSONWriter write)
      throws JSONException, RepositoryException {
    String type = "";
    if (node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
      type = node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString();
    }

    if (FilesConstants.RT_SAKAI_LINK.equals(type)) {
      FileUtils.writeLinkNode(node, session, write, siteService);
    } else {
      FileUtils.writeFileNode(node, session, write, siteService, depth);
    }
  }
}
