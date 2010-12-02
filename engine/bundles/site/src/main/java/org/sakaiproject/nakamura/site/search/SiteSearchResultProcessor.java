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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.RowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;

/**
 * Formats user profile node search results
 */
@Component(immediate = true, label = "%siteSearch.result.processor.label", description = "%siteSearch.result.processor.desc")
@Service
public class SiteSearchResultProcessor implements SearchResultProcessor {

  @Reference
  private SiteService siteService;

  @Reference
  private SearchServiceFactory searchServiceFactory;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SiteSearchResultProcessor.class);

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "Site")
  static final String SEARCH_PROCESSOR = "sakai.search.processor";

  @Property(value = "sakai/site")
  static final String RESOURCE_TYPE = "sakai.search.resourcetype";

  public void writeNode(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, Row row) throws JSONException, RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node resultNode = RowUtils.getNode(row, session);
    if (!siteService.isSite(resultNode)) {
      LOGGER.warn("Search result was not a site node: " + resultNode.getPath());
      throw new JSONException("Unable to write non-site node result");
    }
    if (aggregator != null) {
      aggregator.add(resultNode);
    }
    writeNode(request, write, resultNode);
  }

  public void writeNode(SlingHttpServletRequest request, JSONWriter write, Node resultNode)
      throws JSONException, RepositoryException {
    write.object();
    write.key("member-count");
    write.value(String.valueOf(siteService.getMemberCount(resultNode)));
    int maxTraversalDepth = SearchUtil.getTraversalDepth(request);
    ExtendedJSONWriter.writeNodeTreeToWriter(write, resultNode, true, maxTraversalDepth);
    write.endObject();
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

  protected void bindSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  protected void unbindSiteService(SiteService siteService) {
    this.siteService = null;
  }

}
