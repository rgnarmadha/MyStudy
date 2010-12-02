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
package org.sakaiproject.nakamura.api.search;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.RowIterator;

/**
 *
 */
public interface SearchBatchResultProcessor {

  String DEFAULT_BATCH_PROCESSOR_PROP = "sakai.search.processor.batch.default";

  /**
   * Process an entire result set for a query.
   *
   * @param request
   *          The request associated with this search.
   * @param write
   *          The JSONWriter where the Search Servlet already has written a partial
   *          response to.
   * @param aggregator
   *          an optional aggregator to which all nodes should be given to produce an
   *          aggregate for the set returned. May be null in which case it can be ignored.
   * @param iterator
   *          The RowIterator containing the results.
   * @throws JSONException
   * @throws RepositoryException
   */
  void writeNodes(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, RowIterator iterator) throws JSONException,
      RepositoryException;

  /**
   * Prepare the resultset
   *
   * @param request
   *          The request that triggered the search.
   * @param query
   *          The query that was used
   * @return A searchresultset containing a paged iterator, total hits
   */
  SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SearchException;
}
