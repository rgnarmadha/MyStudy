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

import static org.sakaiproject.nakamura.api.search.SearchUtil.escapeString;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;

import java.util.Map;

import javax.jcr.query.Query;

/**
 * Provides properties to process the search
 */
@Component(immediate = true, label = "%discussion.initialPostProperty.label", description = "discussion.initialPostProperty.desc")
@Service
public class DiscussionInitialPostPropertyProvider implements SearchPropertyProvider {

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "DiscussionInitialPost")
  static final String SEARCH_PROVIDER = "sakai.search.provider";

  public void loadUserProperties(SlingHttpServletRequest request, Map<String, String> propertiesMap) {
    // Make sure we don't go trough the entire repository..
    String path = request.getResource().getPath();
    
    RequestParameter pathParam = request.getRequestParameter("path");
    if (pathParam != null) {
      path = pathParam.getString();
    }
    
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    
    propertiesMap.put("_path", escapeString(ISO9075.encodePath(path), Query.XPATH));
  }

}
