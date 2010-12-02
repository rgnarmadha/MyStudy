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
package org.sakaiproject.nakamura.search.processors;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.api.search.ValidatingRowIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

/**
 *
 */
@Component
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = SearchConstants.REG_BATCH_PROCESSOR_NAMES, value = "GroupMembers")
})
public class GroupMembersSearchResultProcessor extends NodeSearchBatchResultProcessor {
  private static final Logger logger = LoggerFactory
      .getLogger(GroupMembersSearchResultProcessor.class);
 
  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  @Override
  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SearchException {
    try {
      // Get the query result.
      QueryResult rs = query.execute();

      @SuppressWarnings("unchecked")
      final Set<String> memberIds = (Set<String>) request.getAttribute("memberIds");

      // Do the paging on the iterator.
      
      ValidatingRowIterator iterator = new ValidatingRowIterator(rs.getRows()) {

        @Override
        protected boolean isValid(Node node) {
          try {
            if (memberIds != null && node.hasProperty("rep:userId")) {
              boolean isMember = false;
              String username = node.getProperty("rep:userId").getString();
              if (memberIds.contains(username)) {
                isMember = true;
              }
              return isMember;
            } else {
              return true;
            }
          } catch (RepositoryException e) {
            return false;
          }
        }
      };

      // Extract the total hits from lucene
      long start = SearchUtil.getPaging(request);
      iterator.skip(start);

      // Return the result set.
      int maxResults = (int) SearchUtil.longRequestParameter(request,
          SearchConstants.PARAM_MAX_RESULT_SET_COUNT,
          SearchConstants.DEFAULT_PAGED_ITEMS);
      
      SearchResultSet srs = searchServiceFactory.getSearchResultSet(iterator, maxResults);
      return srs;
    } catch (RepositoryException e) {
      logger.error("Unable to perform query.", e);
      throw new SearchException(500, "Unable to perform query.");
    }
  }
}
