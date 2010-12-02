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
package org.sakaiproject.nakamura.site.create;

import static org.sakaiproject.nakamura.api.site.SiteConstants.AUTHORIZABLES_SITE_PRINCIPAL_NAME;
import static org.sakaiproject.nakamura.api.site.SiteConstants.GROUPS_PROPERTY_MANAGERS;
import static org.sakaiproject.nakamura.api.site.SiteConstants.GROUPS_PROPERTY_VIEWERS;
import static org.sakaiproject.nakamura.api.site.SiteConstants.RT_GROUPS;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONObject;

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;
import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 *
 */
public class SiteTemplateBuilder extends TemplateBuilder {

  private List<GroupToCreate> groups;

  public SiteTemplateBuilder(Node templateNode, JSONObject json)
      throws RepositoryException {
    super(templateNode, json);

    defaultPropertiesToIgnore.add("sakai:template-group");
    defaultPropertiesToIgnore.add("sakai:template-groups");

    // Read in the entire template.
    readTemplate();

    // Get the groups out of the map.
    readGroups();
  }

  /**
   * @return The groups that should be created by matching the template and the provided
   *         JSON input.
   */
  public List<GroupToCreate> getGroups() {
    return this.groups;
  }

  /**
   * @return The site node structure represented as Map of Maps. A property value is
   *         either Value or Value[].
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> getSiteMap() {
    return (Map<String, Object>) getMap().get("site");
  }

  /**
   * Reads the groups
   * 
   * @throws RepositoryException
   */
  @SuppressWarnings("unchecked")
  private void readGroups() throws RepositoryException {
    groups = new ArrayList<GroupToCreate>();

    // This is a list of properties we will ignore for the group nodes.
    // Either because they are JCR specific or because we use them for something else,
    // ex: principal name, managers, viewers,..
    List<String> propertiesToIgnore = new ArrayList<String>();
    propertiesToIgnore.addAll(defaultPropertiesToIgnore);
    propertiesToIgnore.add(GROUPS_PROPERTY_MANAGERS);
    propertiesToIgnore.add(GROUPS_PROPERTY_VIEWERS);
    propertiesToIgnore.add(AUTHORIZABLES_SITE_PRINCIPAL_NAME);
    propertiesToIgnore.add(JCR_PRIMARYTYPE);
    propertiesToIgnore.add(SLING_RESOURCE_TYPE_PROPERTY);

    // We should've read everything under the template node in the readSiteTemplate()
    // method, including the groups.
    // All of the required info should be in the siteMap under the key'groups'.
    Map<String, Object> groupMap = (Map<String, Object>) getMap().get("groups");
    for (Entry<String, Object> entry : groupMap.entrySet()) {
      if (entry.getValue() instanceof Map) {
        Map<String, Object> groupNode = (Map<String, Object>) entry.getValue();
        if (groupNode.containsKey(SLING_RESOURCE_TYPE_PROPERTY)) {
          Value val = (Value) groupNode.get(SLING_RESOURCE_TYPE_PROPERTY);
          if (!RT_GROUPS.equals(val.getString())) {
            // If this is not a group we skip it.
            continue;
          }

          // This entry represents a group.
          // Get the principal for it.
          final String principalName = ((Value) groupNode
              .get(AUTHORIZABLES_SITE_PRINCIPAL_NAME)).getString();
          Principal principal = new Principal() {

            public String getName() {
              return principalName;
            }
          };

          // The properties this group will get.
          Map<String, Object> properties = new HashMap<String, Object>();
          for (Entry<String, Object> groupProperty : groupNode.entrySet()) {
            if (!propertiesToIgnore.contains(groupProperty.getKey())) {
              properties.put(groupProperty.getKey(), groupProperty.getValue());
            }
          }

          GroupToCreate g = new GroupToCreate();
          g.setPrincipal(principal);
          g.setManagers((Value[]) groupNode.get(GROUPS_PROPERTY_MANAGERS));
          g.setViewers((Value[]) groupNode.get(GROUPS_PROPERTY_VIEWERS));
          g.setProperties(properties);

          groups.add(g);

        }
      }
    }
  }

}
