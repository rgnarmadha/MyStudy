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
package org.sakaiproject.nakamura.site.search;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.fail;

import junit.framework.Assert;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.StringWriter;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 *
 */
public class SiteContentSearchResultProcessorTest extends AbstractEasyMockTest {

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testResultWithResourceType() throws Exception {

    String excerpt = "foobar";
    RequestParameter itemsParam = createMock(RequestParameter.class);
    expect(itemsParam.getString()).andReturn("5").anyTimes();
    RequestParameter pageParam = createMock(RequestParameter.class);
    expect(pageParam.getString()).andReturn("0").anyTimes();

    SiteContentSearchResultProcessor siteContentSearchResultProcessor = new SiteContentSearchResultProcessor();
    SiteService siteService = createMock(SiteService.class);
    siteContentSearchResultProcessor.bindSiteService(siteService);
    Row row = createMock(Row.class);
    Value valPath = createMock(Value.class);
    expect(valPath.getString()).andReturn("/sites/physics-101").anyTimes();
    Value valExcerpt = createMock(Value.class);
    expect(valExcerpt.getString()).andReturn(excerpt);
    expect(row.getValue("jcr:path")).andReturn(valPath).anyTimes();
    expect(row.getValue("rep:excerpt(jcr:content)")).andReturn(valExcerpt);
    Node resultNode = createMock(Node.class);
    expect(row.getNode()).andReturn(resultNode);
    expect(resultNode.getPath()).andReturn("/sites/physics-101").anyTimes();
    expect(resultNode.getName()).andReturn("physics-101").anyTimes();
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getRequestParameter("items")).andReturn(itemsParam).anyTimes();
    expect(request.getRequestParameter("page")).andReturn(pageParam).anyTimes();
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Session session = createMock(Session.class);
    expect(request.getResourceResolver()).andReturn(resourceResolver)
        .anyTimes();
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session)
        .anyTimes();
    expect(session.getItem("/sites/physics-101")).andReturn(resultNode)
        .anyTimes();

    expect(siteService.isSite(resultNode)).andReturn(true);
    expect(siteService.getMemberCount(resultNode)).andReturn(5);
    expect(
        resultNode
            .hasProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))
        .andReturn(true);

    Property pageProperty = createMock(Property.class);
    expect(pageProperty.getString()).andReturn("sakai/page").anyTimes();

    expect(
        resultNode
            .getProperty(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY))
        .andReturn(pageProperty).anyTimes();

    BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
    expectServiceTrackerCalls(bundleContext, SearchResultProcessor.class
        .getName());
    EasyMock.replay(bundleContext);

    ComponentContext componentContext = EasyMock
        .createMock(ComponentContext.class);
    expect(componentContext.getBundleContext()).andReturn(bundleContext);
    EasyMock.replay(componentContext);

    siteContentSearchResultProcessor.activate(componentContext);

    SearchResultProcessorTracker tracker = new SearchResultProcessorTracker(
        bundleContext);
    SearchResultProcessor proc = new SearchResultProcessor() {
      public void writeNode(SlingHttpServletRequest request, JSONWriter write,
          Aggregator aggregator, Row row) throws JSONException, RepositoryException {
        write.value("value");
      }

      public SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
          Query query) throws SearchException {
        return null;
      }

    };

    tracker.putProcessor(proc, new String[] { "sakai/page" });
    siteContentSearchResultProcessor.tracker = tracker;

    // Writing properties
    PropertyIterator propIterator = createMock(PropertyIterator.class);
    expect(propIterator.hasNext()).andReturn(false).anyTimes();
    expect(resultNode.getProperties()).andReturn(propIterator).anyTimes();

    StringWriter w = new StringWriter();
    JSONWriter write = new JSONWriter(w);
    RowIterator iterator = createMock(RowIterator.class);
    expect(iterator.hasNext()).andReturn(true);
    expect(iterator.nextRow()).andReturn(row);
    expect(iterator.hasNext()).andReturn(false);
    iterator.skip(0);

    RequestPathInfo pathInfo = createNiceMock(RequestPathInfo.class);
    expect(request.getRequestPathInfo()).andReturn(pathInfo);

    replay();

    try {
      siteContentSearchResultProcessor.writeNodes(request, write, null,
          iterator);

      JSONObject o = new JSONObject(w.toString());
      JSONObject site = o.getJSONObject("site");
      Assert.assertEquals(site.get("jcr:path"), "/sites/physics-101");
      Assert.assertEquals(o.get("excerpt"), excerpt);
      Assert.assertNotNull(site);

    } catch (JSONException e) {
      fail("This should not have thrown an exception.");
    }

  }

  private void expectServiceTrackerCalls(BundleContext bundleContext,
      String className) throws InvalidSyntaxException {
    Filter filter = EasyMock.createNiceMock(Filter.class);
    EasyMock.replay(filter);
    EasyMock.expect(
        bundleContext.createFilter("(objectClass=" + className + ")"))
        .andReturn(filter).anyTimes();
    bundleContext.addServiceListener((ServiceListener) EasyMock.anyObject(),
        EasyMock.eq("(objectClass=" + className + ")"));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.expect(bundleContext.getServiceReferences(className, null))
        .andReturn(new ServiceReference[0]).anyTimes();
  }
}
