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
package org.sakaiproject.nakamura.doc;

import static org.junit.Assert.assertTrue;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockNodeIterator;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

/**
 *
 */
public class DocumentationWriterTest {

  private String path = "/path/to/searchnode";
  // Things that should be shown in the doc.
  private String title = "-_-title";
  private String description = "-_-description";
  private String response = "-_-reponse";
  private String shortDesc = "-_-shortDesc";
  private String parameter = "{name: \"parName\", description: \"parDescription\"}";
  // Things that should not be shown in the doc.
  private String query = "//-_-query";
  private DocumentationWriter writer;
  private ByteArrayOutputStream baos;
  private PrintWriter printWriter;

  @Before
  public void setUp() {
    baos = new ByteArrayOutputStream();
    printWriter = new PrintWriter(baos);
    writer = new DocumentationWriter("Search nodes", printWriter);
  }

  @Test
  public void testNode() throws RepositoryException, UnsupportedEncodingException {
    Node node = createNode(path, title, description, response, shortDesc, parameter,
        query);
    Session session = mock(Session.class);
    when(session.getItem(path)).thenReturn(node);

    writer.writeSearchInfo(path, session);
    printWriter.flush();
    String s = baos.toString("UTF-8");

    assertEquals(true, s.contains(path));
    assertEquals(true, s.contains(title));
    assertEquals(true, s.contains(description));
    assertEquals(true, s.contains(response));
    assertEquals(true, s.contains("parName"));
    assertEquals(false, s.contains(query));
  }

  private Node createNode(String path, String title, String description, String response,
      String shortDesc, String parameter, String query) throws RepositoryException {
    MockNode node = new MockNode(path);
    node.setProperty(SLING_RESOURCE_TYPE_PROPERTY, "sakai/search");
    node.setProperty(NodeDocumentation.TITLE, title);
    node.setProperty(NodeDocumentation.DESCRIPTION, description);
    node.setProperty(NodeDocumentation.RESPONSE, response);
    node.setProperty(NodeDocumentation.SHORT_DESCRIPTION, shortDesc);
    node.setProperty(NodeDocumentation.PARAMETERS, parameter);
    node.setProperty("sakai:query", query);

    return node;
  }

  @Test
  public void testQuery() throws RepositoryException, UnsupportedEncodingException {
    Session session = mock(Session.class);
    Workspace workSpace = mock(Workspace.class);
    QueryManager qm = mock(QueryManager.class);
    Query q = mock(Query.class);
    QueryResult result = mock(QueryResult.class);
    String queryString = "//*[@sling:resourceType='sakai/search']";

    Node node = createNode(path, title, description, response, shortDesc, parameter,
        query);
    NodeIterator nodeIterator = new MockNodeIterator(new Node[] { node });

    // Mock execution of query.
    when(session.getWorkspace()).thenReturn(workSpace);
    when(workSpace.getQueryManager()).thenReturn(qm);
    when(qm.createQuery(queryString, Query.XPATH)).thenReturn(q);
    when(q.execute()).thenReturn(result);
    when(result.getNodes()).thenReturn(nodeIterator);

    writer.writeNodes(session, queryString, "/system/doc/proxy");
    printWriter.flush();
    String s = baos.toString("UTF-8");

    assertTrue(s.contains(path));
  }

}
