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
package org.sakaiproject.nakamura.search.processors;

import junit.framework.Assert;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.easymock.EasyMock;
import org.junit.Test;
import org.sakaiproject.nakamura.search.AggregateCount;
import org.sakaiproject.nakamura.search.SearchServiceFactoryImpl;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.StringWriter;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 *
 */
public class NodeSearchBatchResultProcessorTest extends AbstractEasyMockTest {

  @Test
  public void test() throws JSONException, RepositoryException {
    SlingHttpServletRequest request = createNiceMock(SlingHttpServletRequest.class);
    ResourceResolver resourceResolver = createNiceMock(ResourceResolver.class);
    Session session = createNiceMock(Session.class);
    Value value = createNiceMock(Value.class);
    Node node = createNiceMock(Node.class);
    RowIterator iterator = createNiceMock(RowIterator.class);
    PropertyIterator propertyIterator = createNiceMock(PropertyIterator.class);

    Row row = createNiceMock(Row.class);

    EasyMock.expect(request.getResourceResolver()).andReturn(resourceResolver).anyTimes();
    EasyMock.expect(resourceResolver.adaptTo(Session.class)).andReturn(session)
        .anyTimes();
    EasyMock.expect(row.getValue("jcr:path")).andReturn(value).anyTimes();
    EasyMock.expect(value.getString()).andReturn("/test").anyTimes();
    EasyMock.expect(session.getItem("/test")).andReturn(node).anyTimes();

    EasyMock.expect(iterator.hasNext()).andReturn(true);
    EasyMock.expect(iterator.nextRow()).andReturn(row);

    EasyMock.expect(node.getProperties()).andReturn(propertyIterator);

    RequestPathInfo pathInfo = createNiceMock(RequestPathInfo.class);
    EasyMock.expect(request.getRequestPathInfo()).andReturn(pathInfo);


    replay();
    AggregateCount aggregator = new AggregateCount(new String[] { "test" }, false);

    StringWriter stringWriter = new StringWriter();
    JSONWriter write = new JSONWriter(stringWriter);

    NodeSearchBatchResultProcessor nodeSearchBatchResultProcessor = new NodeSearchBatchResultProcessor(new SearchServiceFactoryImpl());
    nodeSearchBatchResultProcessor.writeNodes(request, write, aggregator, iterator);

    String output = stringWriter.toString();
    Assert.assertTrue(output.length() > 0);
    // Make sure that the output is valid JSON.
    new JSONObject(output);

    verify();
  }
}
