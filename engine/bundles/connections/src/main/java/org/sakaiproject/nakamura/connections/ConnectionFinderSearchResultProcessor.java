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
package org.sakaiproject.nakamura.connections;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;

/**
 * Formats connection search results. We get profile nodes from the query and make a
 * uniformed result.
 */
@Component(immediate = true, description = "Formatter for connection search results", label = "ConnectionFinderSearchResultProcessor")
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.processor", value = "ConnectionFinder") })
@Service(value = SearchResultProcessor.class)
public class ConnectionFinderSearchResultProcessor implements SearchResultProcessor {

  @Reference
  protected SearchServiceFactory searchServiceFactory;

  public void writeNode(SlingHttpServletRequest request, JSONWriter write, Aggregator aggregator, Row row)
      throws JSONException, RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node profileNode = row.getNode();
    String user = request.getRemoteUser();
    String targetUser = profileNode.getProperty("rep:userId").getString();
    UserManager um = AccessControlUtil.getUserManager(session);
    Authorizable auMe = um.getAuthorizable(user);
    Authorizable auTarget = um.getAuthorizable(targetUser);
    String contactNodePath = ConnectionUtils.getConnectionPath(auMe, auTarget);
    Node node = (Node) session.getItem(contactNodePath);
    if (aggregator != null) {
      aggregator.add(node);
    }

    int maxTraversalDepth = SearchUtil.getTraversalDepth(request);

    write.object();
    write.key("target");
    write.value(targetUser);
    write.key("profile");
    ExtendedJSONWriter.writeNodeTreeToWriter(write, profileNode, maxTraversalDepth);
    write.key("details");
    ExtendedJSONWriter.writeNodeTreeToWriter(write, node, maxTraversalDepth);
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
}
