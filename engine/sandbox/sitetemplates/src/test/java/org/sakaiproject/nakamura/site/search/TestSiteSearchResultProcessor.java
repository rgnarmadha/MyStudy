package org.sakaiproject.nakamura.site.search;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.junit.Test;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.site.SiteException;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.site.search.SiteSearchResultProcessor;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

public class TestSiteSearchResultProcessor extends AbstractEasyMockTest {

  @Test
  public void testResultCountLimit() throws RepositoryException, JSONException, SiteException {
    SiteSearchResultProcessor siteSearchResultProcessor = new SiteSearchResultProcessor();
    SiteService siteService = createMock(SiteService.class);
    siteSearchResultProcessor.bindSiteService(siteService);
    expect(siteService.isSite(isA(Item.class))).andReturn(true).anyTimes();
    expect(siteService.getMemberCount(isA(Node.class))).andReturn(20).anyTimes();
    simpleResultCountCheck(siteSearchResultProcessor);
  }

  @Test
  public void testNonSiteNode() throws RepositoryException, JSONException {
    SiteSearchResultProcessor siteSearchResultProcessor = new SiteSearchResultProcessor();
    SiteService siteService = createMock(SiteService.class);
    siteSearchResultProcessor.bindSiteService(siteService);
    Row row = createMock(Row.class);
    Value val = createMock(Value.class);
    expect(val.getString()).andReturn("");
    expect(row.getValue("jcr:path")).andReturn(val);
    expect(siteService.isSite(isA(Item.class))).andReturn(false);
    Node resultNode = createMock(Node.class);
    expect(resultNode.getPath()).andReturn("");
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Session session = createMock(Session.class);
    expect(request.getResourceResolver()).andReturn(resourceResolver);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    expect(session.getItem("")).andReturn(resultNode);
    replay();
    try {
      siteSearchResultProcessor.writeNode(request, null, null, row);
      fail();
    } catch (JSONException e) {
      assertEquals("Unable to write non-site node result", e.getMessage());
    }
  }
  
  protected void simpleResultCountCheck(SearchResultProcessor processor) throws RepositoryException, JSONException
  {
    int itemCount = 12;
    QueryResult queryResult = createMock(QueryResult.class);
    RowIterator results = createMock(RowIterator.class);
    expect(queryResult.getRows()).andReturn(results);
    expect(results.getSize()).andReturn(500L).anyTimes();
    Row dummyRow = createMock(Row.class);Value val = createMock(Value.class);
    expect(val.getString()).andReturn("").times(itemCount);
    expect(dummyRow.getValue("jcr:path")).andReturn(val).times(itemCount);
    expect(results.hasNext()).andReturn(true).anyTimes();
    expect(results.nextRow()).andReturn(dummyRow).times(itemCount);
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Session session = createMock(Session.class);    
    Node resultNode = createMock(Node.class);
    expect(resultNode.getPath()).andReturn("/path/to/node").anyTimes();
    expect(resultNode.getName()).andReturn("node").anyTimes();
    expect(request.getResourceResolver()).andReturn(resourceResolver).times(itemCount);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session).times(itemCount);
    expect(session.getItem("")).andReturn(resultNode).times(itemCount);
    PropertyIterator propIterator = createMock(PropertyIterator.class);
    expect(propIterator.hasNext()).andReturn(false).anyTimes();
    expect(resultNode.getProperties()).andReturn(propIterator).anyTimes();
    
    replay();
    JSONWriter write = new JSONWriter(new PrintWriter(new ByteArrayOutputStream()));
    write.array();
    RowIterator iterator = queryResult.getRows();
    int i=0;
    while (iterator.hasNext() && i < itemCount) {
      processor.writeNode(request, write, null, iterator.nextRow());
      i++;
    }
    write.endArray();
    verify();
  }
}
