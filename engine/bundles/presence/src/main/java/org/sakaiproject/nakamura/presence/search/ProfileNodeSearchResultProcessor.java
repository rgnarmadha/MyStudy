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
package org.sakaiproject.nakamura.presence.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.framework.Constants;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.presence.PresenceUtils;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;

/**
 * Search result processor to write out profile information when search returns home nodes
 * (sakai/user-home).
 */
@Component
@Service
@Properties({
  @Property(name = Constants.SERVICE_VENDOR, value = "The Sakai Foundation"),
  @Property(name = SearchConstants.REG_PROCESSOR_NAMES, value = "Profile")
})
public class ProfileNodeSearchResultProcessor implements SearchResultProcessor {
  @Reference
  private SearchServiceFactory searchServiceFactory;

  @Reference
  private ProfileService profileService;

  @Reference
  private PresenceService presenceService;

  public ProfileNodeSearchResultProcessor() {
  }

  ProfileNodeSearchResultProcessor(SearchServiceFactory searchServiceFactory,
      ProfileService profileService, PresenceService presenceService) {
    if (searchServiceFactory == null || profileService == null || presenceService == null) {
      throw new IllegalArgumentException(
          "SearchServiceFactory, ProfileService and PresenceService must be set when not using as a component");
    }
    this.searchServiceFactory = searchServiceFactory;
    this.presenceService = presenceService;
    this.profileService = profileService;
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

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.search.processors.SearchResultProcessor#writeNode(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.commons.json.io.JSONWriter,
   *      org.sakaiproject.nakamura.api.search.Aggregator, javax.jcr.query.Row)
   */
  public void writeNode(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, Row row) throws JSONException, RepositoryException {
    Node homeNode = row.getNode();
    String profilePath = homeNode.getPath() + "/public/authprofile";
    Session session = homeNode.getSession();
    Node profileNode = session.getNode(profilePath);
    write.object();
    ValueMap map = profileService.getProfileMap(profileNode);
    ((ExtendedJSONWriter) write).valueMapInternals(map);

    // If this is a User Profile, then include Presence data.
    if (profileNode.hasProperty("rep:userId")) {
      PresenceUtils.makePresenceJSON(write, profileNode.getProperty("rep:userId")
          .getString(), presenceService, true);
    }
    write.endObject();
  }
}
