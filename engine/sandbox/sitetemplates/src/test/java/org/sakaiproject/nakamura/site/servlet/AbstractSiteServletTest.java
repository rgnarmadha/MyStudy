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
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.site.AbstractSiteTest;
import org.sakaiproject.nakamura.site.SiteServiceImpl;
import org.sakaiproject.nakamura.util.JcrUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
public abstract class AbstractSiteServletTest extends AbstractSiteTest {

  protected SlingHttpServletRequest request;
  protected SlingHttpServletResponse response;
  protected SiteService siteService;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    siteService = new SiteServiceImpl();
    request = mock(SlingHttpServletRequest.class);
    response = mock(SlingHttpServletResponse.class);
  }

  public abstract void makeRequest() throws Exception;

  /**
   * template + groups + group0
   * 
   * + group1 + site + @@site.properties.status==online@@ + statusnode
   * -sling:resourceType=statusfoo + pages + @@site.pages(...)@@ + @@site.pages(...).id@@
   * - @@site.pages(...).id@@ - @@site.pages(...).content@@ -
   * sling:resourceType=sakai/page
   * 
   * @param uniqueidentifier
   * @throws Exception
   */
  public String createTemplateStructure(String uniqueidentifier) throws Exception {
    Session session = loginAdministrative();
    String path = "/test/templates/tpl-" + uniqueidentifier;

    Node templateNode = JcrUtils.deepGetOrCreateNode(session, path);

    // The group nodes
    Node groupsNode = templateNode.addNode("groups");
    groupsNode.setProperty("sling:resourceType", "sakai/template-groups");
    Node g0 = groupsNode.addNode("group0");
    g0.setProperty("sakai:template-group-principalname", "@@groups.collaborators.name@@");
    g0.setProperty("sakai:template-group-managers",
        new String[] { "@@groups.collaborators.members(...)@@" });
    g0.setProperty("sakai:template-group-viewers",
        new String[] { "@@groups.collaborators.members(...)@@" });
    g0.setProperty("sakai:template-group-issitemanager", true);
    g0.setProperty("sling:resourceType", "sakai/template-group");

    Node g1 = groupsNode.addNode("group1");
    g1.setProperty("sakai:template-group-principalname", "@@groups.collaborators.name@@");
    g1.setProperty("sakai:template-group-managers",
        new String[] { "@@groups.collaborators.members(...)@@" });
    g1.setProperty("sakai:template-group-viewers",
        new String[] { "@@groups.collaborators.members(...)@@" });
    g1.setProperty("sakai:template-group-issitemanager", true);
    g1.setProperty("sling:resourceType", "sakai/template-group");

    // The actual site.
    Node siteNode = templateNode.addNode("site");
    siteNode.setProperty("id", "@@site.properties.id@@");
    siteNode.setProperty("name", "@@site.properties.title@@");
    siteNode.setProperty("sling:resourceType", "sakai/site");

    // We create a small If structure in here..
    Node ifNode = siteNode.addNode("@@site.properties.status==online?@@");
    Node statusNode = ifNode.addNode("statusnode");
    statusNode.setProperty("sling:resourceType", "statusfoo");

    // The site pages
    Node pagesNode = siteNode.addNode("pages");
    Node pagesArrayNode = pagesNode.addNode("@@site.pages(...)@@");
    Node pagesArrayItemNode = pagesArrayNode.addNode("@@site.pages(...).id@@");
    pagesArrayItemNode.setProperty("sling:resourceType", "sakai/page");
    pagesArrayItemNode.setProperty("sakai:page-content", "@@site.pages(...).content@@");
    pagesArrayItemNode.setProperty("sakai:page-id", "@@site.pages(...).id@@");

    // We ACL control a random bit.
    Node fooNode = siteNode.addNode("foobar");
    Node aceNode = fooNode.addNode("ace0");
    aceNode.setProperty("sling:resourceType", "sakai/template-ace");
    aceNode.setProperty("sakai:template-ace-principal", "@@groups.collaborators.name@@");
    aceNode.setProperty("sakai:template-ace-granted", new String[] { "jcr:all" });
    aceNode.setProperty("sakai:template-ace-denied", new String[] {});
    Node ace1Node = fooNode.addNode("ace1");
    ace1Node.setProperty("sling:resourceType", "sakai/template-ace");
    ace1Node.setProperty("sakai:template-ace-principal", "@@groups.viewers.name@@");
    ace1Node.setProperty("sakai:template-ace-granted", new String[] {});
    ace1Node.setProperty("sakai:template-ace-denied", new String[] { "jcr:all" });

    if (session.hasPendingChanges()) {
      session.save();
    }

    return path;
  }

  /**
   * @return
   * @throws RepositoryException
   * @throws IOException
   */
  public Node createGoodSite(Session adminSession) throws Exception {
    long time = System.currentTimeMillis();
    String sitePath = "/" + time + "/sites/goodsite";
    Node siteNode = createSite(adminSession, sitePath);

    if (adminSession.hasPendingChanges()) {
      adminSession.save();
    }
    return siteNode;
  }

  public byte[] makeGetRequestReturningBytes() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(baos);
    when(response.getWriter()).thenReturn(writer);
    makeRequest();
    writer.flush();
    return baos.toByteArray();
  }

  public String makeGetRequestReturningString() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter writer = new PrintWriter(baos);
    when(response.getWriter()).thenReturn(writer);
    makeRequest();
    writer.flush();
    return baos.toString("utf-8");
  }

  public JSONArray makeGetRequestReturningJSON() throws Exception, JSONException {
    String jsonString = new String(makeGetRequestReturningBytes());
    return new JSONArray(jsonString);
  }
}
