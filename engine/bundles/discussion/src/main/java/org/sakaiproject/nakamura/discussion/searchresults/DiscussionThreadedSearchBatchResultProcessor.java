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
package org.sakaiproject.nakamura.discussion.searchresults;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.discussion.DiscussionConstants;
import org.sakaiproject.nakamura.api.discussion.Post;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchBatchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.RowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

/**
 * Formats message node search results
 */
@Component(immediate = true, label = "%discussion.threadedSearchBatch.label", description = "%discussion.threadedSearchBatch.desc")
@Service
public class DiscussionThreadedSearchBatchResultProcessor implements
    SearchBatchResultProcessor {

  public static final Logger LOG = LoggerFactory
      .getLogger(DiscussionThreadedSearchBatchResultProcessor.class);

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "DiscussionThreaded")
  static final String SEARCH_BATCHPROCESSOR = "sakai.search.batchprocessor";

  @Reference
  protected transient PresenceService presenceService;

  @Reference
  protected transient ProfileService profileService;
  
  @Reference
  protected transient SearchServiceFactory searchServiceFactory;


  public void writeNodes(SlingHttpServletRequest request, JSONWriter writer,
      Aggregator aggregator, RowIterator iterator) throws JSONException,
      RepositoryException {

    Session session = request.getResourceResolver().adaptTo(Session.class);
    List<String> basePosts = new ArrayList<String>();
    Map<String,List<Post>> postChildren = new HashMap<String, List<Post>>();
    Map<String,Post> allPosts = new HashMap<String, Post>();
    for (; iterator.hasNext();) {
      Node node = RowUtils.getNode(iterator.nextRow(), session);
      if (aggregator != null) {
        aggregator.add(node);
      }
      
      Post p = new Post(node);
      allPosts.put(node.getProperty(MessageConstants.PROP_SAKAI_ID).getString(), p);

      if (node.hasProperty(DiscussionConstants.PROP_REPLY_ON)) {
        // This post is a reply on another post.
        String replyon = node.getProperty(DiscussionConstants.PROP_REPLY_ON).getString();
        if (!postChildren.containsKey(replyon)) {
          postChildren.put(replyon, new ArrayList<Post>());
        }
        
        postChildren.get(replyon).add(p);
        
        

      } else {
        // This post is not a reply to another post, thus it is a basepost.
        basePosts.add(p.getPostId());
      }
    }

    // Now that we have all the base posts, we can sort the replies properly
    for (String parentId : postChildren.keySet()) {
      Post parentPost = allPosts.get(parentId);
      if (parentPost != null) {
        List<Post> childrenList = parentPost.getChildren();
        List<Post> childrenActual = postChildren.get(parentId);
        childrenList.addAll(childrenActual);
      }
    }
    
    // The posts are sorted, now return them as json.
    for (String basePostId : basePosts) {
      allPosts.get(basePostId).outputPostAsJSON((ExtendedJSONWriter) writer, presenceService, profileService);
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


      // Return the result set.
      return searchServiceFactory.getSearchResultSet(iterator);
    } catch (RepositoryException e) {
      throw new SearchException(500, "Unable to execute query.");
    }
  }

}
