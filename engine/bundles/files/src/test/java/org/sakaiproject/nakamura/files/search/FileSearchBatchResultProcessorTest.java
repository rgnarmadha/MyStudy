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
package org.sakaiproject.nakamura.files.search;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.core.query.lucene.SingleColumnQueryResult;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.files.FilesConstants;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.search.RowIteratorImpl;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 *
 */
public class FileSearchBatchResultProcessorTest {

  private FileSearchBatchResultProcessor processor;
  private SiteService siteService;

  @Before
  public void setUp() {
    processor = new FileSearchBatchResultProcessor();
    siteService = mock(SiteService.class);

    processor.siteService = siteService;
  }

  public void testGetResultSet() throws SearchException, RepositoryException {
    Session session = mock(Session.class);
    Row fileProperA = createRow(session, "/path/to/fileA");
    Row fileProperB = createRow(session, "/path/to/fileB");
    Row fileProperC = createRow(session, "/path/to/fileC");
    Row fileDS_STORE = createRow(session, "/path/to/.DS_STORE");

    List<Row> rows = new ArrayList<Row>();
    rows.add(fileProperA);
    rows.add(fileProperB);
    rows.add(fileProperC);
    rows.add(fileDS_STORE);
    rows.add(fileProperC);

    Query q = mock(Query.class);
    SingleColumnQueryResult result = mock(SingleColumnQueryResult.class);
    when(result.getTotalSize()).thenReturn(5);
    when(result.getRows()).thenReturn(new RowIteratorImpl(rows));
    when(q.execute()).thenReturn(result);

    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    ResourceResolver resolver = mock(ResourceResolver.class);
    when(resolver.adaptTo(Session.class)).thenReturn(session);
    when(request.getResourceResolver()).thenReturn(resolver);

    SearchResultSet set = processor.getSearchResultSet(request, q);

    RowIterator resultIterator = set.getRowIterator();
    int i = 0;
    while (resultIterator.hasNext()) {
      resultIterator.nextRow();
      i++;
    }

    assertEquals(3, i);

  }

  @Test
  public void testWriteFile() throws JSONException, RepositoryException,
      UnsupportedEncodingException {
    Node node = new MockNode("/path/to/file.doc");

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter w = new PrintWriter(baos);
    JSONWriter write = new JSONWriter(w);
    Session session = mock(Session.class);

    processor.handleNode(node, session, write);

    w.flush();
    String s = baos.toString("UTF-8");
    JSONObject o = new JSONObject(s);
    assertEquals("file.doc", o.getString("jcr:name"));
    assertEquals("/path/to/file.doc", o.getString("jcr:path"));
  }

  @Test
  public void testWriteLink() throws JSONException, RepositoryException,
      UnsupportedEncodingException {
    Session session = mock(Session.class);
    Node node = new MockNode("/path/to/link/file.doc");
    node.setProperty(SLING_RESOURCE_TYPE_PROPERTY, FilesConstants.RT_SAKAI_LINK);
    node.setProperty(FilesConstants.SAKAI_LINK, "randomuuid");

    Node fileNode = new MockNode("/path/to/actual/file.doc");
    when(session.getNodeByIdentifier("randomuuid")).thenReturn(fileNode);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter w = new PrintWriter(baos);
    JSONWriter write = new JSONWriter(w);

    processor.handleNode(node, session, write);

    w.flush();
    String s = baos.toString("UTF-8");
    JSONObject o = new JSONObject(s);
    boolean hasFile = o.has("file");
    assertEquals("file.doc", o.getString("jcr:name"));
    assertEquals("/path/to/link/file.doc", o.getString("jcr:path"));
    assertEquals(true, hasFile);
  }

  /**
   * @param string
   * @return
   * @throws RepositoryException
   * @throws ItemNotFoundException
   */
  private Row createRow(Session session, String path) throws RepositoryException {
    Row row = mock(Row.class);
    Value pathValue = new MockValue(path);
    when(row.getValue("jcr:path")).thenReturn(pathValue);
    Node node = new MockNode(path);
    node.setProperty(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_FILE);
    when(session.getItem(path)).thenReturn(node);

    return row;
  }

}
