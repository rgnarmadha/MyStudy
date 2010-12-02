/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The SF licenses this file to you under the Apache License, Version
 * 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.docproxy.url;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Properties;

import javax.jcr.Node;

import org.apache.http.localserver.LocalTestServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata;
import org.sakaiproject.nakamura.docproxy.url.requestHandlers.DocumentRequestHandler;
import org.sakaiproject.nakamura.docproxy.url.requestHandlers.MetadataRequestHandler;
import org.sakaiproject.nakamura.docproxy.url.requestHandlers.RemoveRequestHandler;
import org.sakaiproject.nakamura.docproxy.url.requestHandlers.SearchRequestHandler;
import org.sakaiproject.nakamura.docproxy.url.requestHandlers.UpdateRequestHandler;

@RunWith(MockitoJUnitRunner.class)
public class UrlRepositoryProcessorTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Node node;

  private LocalTestServer server;

  private UrlRepositoryProcessor processor;

  private DocumentRequestHandler docHandler;
  private MetadataRequestHandler metadataHandler;
  private RemoveRequestHandler removeHandler;
  private SearchRequestHandler searchHandler;
  private UpdateRequestHandler updateHandler;

  private String docPath1 = "myDoc1.ext";
  private String docPath2 = "myDoc2.ext";
  private String docPath3 = "myDoc3.ext";
  private String docPath4 = "myDoc4.ext";
  private String docPath5 = "myDoc5.ext";

  private UrlDocumentResult docResult1;
  private UrlDocumentResult docResult2;
  private UrlDocumentResult docResult3;
  private UrlDocumentResult docResult4;
  private UrlDocumentResult docResult5;

  @Before
  public void setUp() throws Exception {
    when(node.getSession().getUserID()).thenReturn("ch1411");

    HashMap<String, Object> docProps = new HashMap<String, Object>();
    docProps.put("key1", "value1");
    docResult1 = new UrlDocumentResult(docPath1, "text/plain", 1000, docProps);
    docResult2 = new UrlDocumentResult(docPath2, "text/plain", 2000, null);
    docResult3 = new UrlDocumentResult(docPath3, "text/plain", 3000, null);
    docResult4 = new UrlDocumentResult(docPath4, "text/plain", 4000, null);
    docResult5 = new UrlDocumentResult(docPath5, "text/plain", 5000, null);

    docHandler = new DocumentRequestHandler(docResult1);
    metadataHandler = new MetadataRequestHandler(docResult1);
    removeHandler = new RemoveRequestHandler();
    searchHandler = new SearchRequestHandler(docResult1, docResult2, docResult3,
        docResult4, docResult5);
    updateHandler = new UpdateRequestHandler();

    // setup the local test server
    server = new LocalTestServer(null, null);
    server.register("/document", docHandler);
    server.register("/metadata", metadataHandler);
    server.register("/remove", removeHandler);
    server.register("/search", searchHandler);
    server.register("/update", updateHandler);
    server.start();

    // setup & activate the url doc processor
    String serverUrl = "http://" + server.getServiceHostName() + ":"
        + server.getServicePort();

    Properties props = new Properties();
    props.put(UrlRepositoryProcessor.DOCUMENT_URL, serverUrl + "/document?p=");
    props.put(UrlRepositoryProcessor.METADATA_URL, serverUrl + "/metadata?p=");
    props.put(UrlRepositoryProcessor.REMOVE_URL, serverUrl + "/remove?p=");
    props.put(UrlRepositoryProcessor.SEARCH_URL, serverUrl + "/search");
    props.put(UrlRepositoryProcessor.UPDATE_URL, serverUrl + "/update?p=");
    props.put(UrlRepositoryProcessor.HMAC_HEADER, "X-HMAC");
    props.put(UrlRepositoryProcessor.SHARED_KEY, "superSecretSharedKey");

    ComponentContext context = mock(ComponentContext.class);
    when(context.getProperties()).thenReturn(props);

    processor = new UrlRepositoryProcessor();
    processor.activate(context);
  }

  @After
  public void tearDown() throws Exception {
    server.stop();
  }

  @Test
  public void testGetType() {
    assertEquals(processor.getType(), UrlRepositoryProcessor.TYPE);
  }

  @Test
  public void testGetDocument() throws Exception {
    ExternalDocumentResult doc = processor.getDocument(node, "myDoc.ext");
    assertEquals(docResult1, doc);
  }

  @Test
  public void testGetDocumentMetadata() throws Exception {
    ExternalDocumentResultMetadata metadata = processor.getDocumentMetadata(node,
        docPath1);
    assertEquals(docResult1, metadata);
  }

  @Test
  public void testRemoveDocument() throws Exception {
    processor.removeDocument(node, docPath1);
  }

  @Test
  public void testSearchDocument() throws Exception {
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put("key1", "value1");
    processor.search(node, props);
  }

  @Test
  public void testUpdateDocument() throws Exception {
    HashMap<String, Object> props = new HashMap<String, Object>();
    props.put("key1", "value1");
    String output = "Data for document";
    ByteArrayInputStream bais = new ByteArrayInputStream(output.getBytes("UTF-8"));
    long streamLength = output.length();
    processor.updateDocument(node, docPath1, props, bais, streamLength);
  }
}
