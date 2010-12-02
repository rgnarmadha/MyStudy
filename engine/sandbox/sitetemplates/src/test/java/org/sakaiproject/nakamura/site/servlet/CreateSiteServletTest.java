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
package org.sakaiproject.nakamura.site.servlet;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONObject;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.site.ACMEGroupStructure;
import org.sakaiproject.nakamura.site.create.GroupToCreate;
import org.sakaiproject.nakamura.site.create.SiteTemplateBuilder;
import org.sakaiproject.nakamura.util.JcrUtils;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
public class CreateSiteServletTest extends AbstractSiteServletTest {

  private CreateSiteServlet servlet;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.site.servlet.AbstractSiteServletTest#makeRequest()
   */
  @Override
  public void makeRequest() throws Exception {
    servlet = new CreateSiteServlet();
    servlet.siteService = siteService;
    servlet.doPost(request, response);
  }

  public void testAnonymous() throws Exception {
    Session session = mock(Session.class);
    when(session.getUserID()).thenReturn(UserConstants.ANON_USERID);
    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);

    when(request.getResourceResolver()).thenReturn(resolver);

    makeRequest();
    verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
  }

  public void testPaths() throws Exception {
    // Test if the container is really a sakai/sites node.

  }

  public void testCreateSimpleStructure() throws Exception {
    long time = System.currentTimeMillis();

    ACMEGroupStructure acme = createAcmeStructure("" + time);

    // The basic properties of the site.
    String json = "{";
    json += "\"groups\" : {";
    json += "    \"collaborators\" : {";
    json += "        \"name\" : \"g-mysite-collaborators\",";
    json += "        \"members\" : [\"" + acme.acmeManagers.getID() + "\"]";
    json += "    },";
    json += "     \"viewers\" : {";
    json += "        \"name\" : \"g-mysite-viewers\",";
    json += "         \"members\" : [\"" + acme.acmeDevelopers.getID() + "\", \""
        + acme.acmeResearchers.getID() + "\"]";
    json += "     }";
    json += "  },";
    json += "\"site\" : {";
    json += "      \"properties\" : {";
    json += "         \"title\" : \"My site " + time + "\",";
    json += "          \"id\" : \"mysite-" + time + "\",";
    json += "          \"status\" : \"offline\"";
    json += "      },";
    json += "   \"pages\" : [";
    json += "       {";
    json += "          \"id\" : \"week-i\",";
    json += "          \"content\": \"First week content\"";
    json += "      },";
    json += "     {";
    json += "         \"id\" : \"week-ii\",";
    json += "          \"content\": \"First week content\"";
    json += "       }";
    json += "    ]";
    json += "    }";
    json += "  }";

    Session session = loginAdministrative();
    String templatePath = createTemplateStructure("" + time);
    Node templateNode = JcrUtils.deepGetOrCreateNode(session, templatePath);

    JSONObject jsonObject = new JSONObject(json);
    SiteTemplateBuilder builder = new SiteTemplateBuilder(templateNode, jsonObject);

    List<GroupToCreate> groups = builder.getGroups();
    assertEquals(2, groups.size());

  }

}
