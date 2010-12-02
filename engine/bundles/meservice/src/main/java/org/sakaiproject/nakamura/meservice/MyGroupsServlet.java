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
package org.sakaiproject.nakamura.meservice;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.user.UserConstants;

import java.util.Iterator;
import java.util.TreeMap;

import javax.jcr.RepositoryException;

@ServiceDocumentation(
  name = "My Groups Servlet",
  description = "Gets the groups where the current user is a member",
  bindings = {
    @ServiceBinding(type = BindingType.TYPE, bindings = { "system/me/groups" })
  },
  methods = {
    @ServiceMethod(
      name = "GET",
      description = {"Get the groups for this user with paging",
      		"curl \"http://ian:ianboston@localhost:8080/system/me/managedgroups.tidy.json?page=0&items=10&q=*&facet=manage\"" +
          "<pre>" +
          "{" +
          "  \"items\": 10," +
          "  \"results\": [{" +
          "      \"sakai:managers-group\": \"test-managers\"," +
          "      \"jcr:path\": \"/~test/public/authprofile\"," +
          "      \"sakai:group-title\": \"test\"," +
          "      \"sakai:group-joinable\": \"no\"," +
          "      \"sakai:pages-visible\": \"public\"," +
          "      \"jcr:uuid\": \"7f448088-db33-40de-a7f3-b4979d3575b1\"," +
          "      \"jcr:mixinTypes\": [" +
          "        \"mix:referenceable\"" +
          "      ]," +
          "      \"sling:resourceType\": \"sakai/group-profile\"," +
          "      \"sakai:group-id\": \"test\"," +
          "      \"basic\": {" +
          "        \"jcr:path\": \"/~test/public/authprofile/basic\"," +
          "        \"jcr:name\": \"basic\"," +
          "        \"access\": \"everybody\"," +
          "        \"elements\": {" +
          "          \"jcr:path\": \"/~test/public/authprofile/basic/elements\"," +
          "          \"lastName\": {" +
          "            \"jcr:path\": \"/~test/public/authprofile/basic/elements/lastName\"," +
          "            \"jcr:name\": \"lastName\"," +
          "            \"value\": \"unknown\"," +
          "            \"jcr:primaryType\": \"nt:unstructured\"" +
          "          }," +
          "          \"email\": {" +
          "            \"jcr:path\": \"/~test/public/authprofile/basic/elements/email\"," +
          "            \"jcr:name\": \"email\"," +
          "            \"value\": \"unknown\"," +
          "            \"jcr:primaryType\": \"nt:unstructured\"" +
          "          }," +
          "          \"jcr:name\": \"elements\"," +
          "          \"firstName\": {" +
          "            \"jcr:path\": \"/~test/public/authprofile/basic/elements/firstName\"," +
          "            \"jcr:name\": \"firstName\"," +
          "            \"value\": \"unknown\"," +
          "            \"jcr:primaryType\": \"nt:unstructured\"" +
          "          }," +
          "          \"jcr:primaryType\": \"nt:unstructured\"" +
          "        }," +
          "        \"jcr:primaryType\": \"nt:unstructured\"" +
          "      }," +
          "      \"path\": \"/t/te/test\"," +
          "      \"jcr:name\": \"authprofile\"," +
          "      \"sakai:group-visible\": \"public\"," +
          "      \"jcr:primaryType\": \"nt:unstructured\"" +
          "    }" +
          "  ]," +
          "  \"total\": 2" +
          "}" +
          "</pre>"},
      response = {
        @ServiceResponse(code = 200, description = "All processing finished successfully."),
        @ServiceResponse(code = 500, description = "Exception occurred during processing.")
      },
      parameters= {
      @ServiceParameter(name = "items", description = { "The number of items per page in the result set." }),
      @ServiceParameter(name = "page", description = { "The page number to start listing the results on." }),
      @ServiceParameter(name = "q", description = { "The Query to filter on." }) 
      }
    )
  }
)
@SlingServlet(paths = { "/system/me/groups" }, generateComponent = true, generateService = true, methods = { "GET" })
@Reference(name="profileService", referenceInterface=ProfileService.class)
public class MyGroupsServlet extends AbstractMyGroupsServlet {
  private static final long serialVersionUID = 8809581334593701801L;

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.meservice.AbstractMyGroupsServlet#getGroups(org.apache.jackrabbit.api.security.user.Authorizable, org.apache.jackrabbit.api.security.user.UserManager)
   */
  @Override
  protected TreeMap<String, Group> getGroups(Authorizable member, UserManager userManager)
      throws RepositoryException {
    TreeMap<String, Group> groups = new TreeMap<String, Group>();
    Iterator<Group> allGroupsIter = member.memberOf();
    while (allGroupsIter.hasNext()) {
      Group group = allGroupsIter.next();
      // Until KERN-950 is fixed, we don't have a foolproof way to know whether
      // a Jackrabbit Group should be considered a Sakai Group entity.
      // We skip Managers-holders but otherwise just return a Profile if
      // we find one.
      if (!group.hasProperty(UserConstants.PROP_MANAGED_GROUP)) {
        groups.put(group.getID(), group);
      }
    }
    return groups;
  }

}
