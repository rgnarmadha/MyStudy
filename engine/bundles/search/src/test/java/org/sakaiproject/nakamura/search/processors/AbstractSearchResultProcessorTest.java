package org.sakaiproject.nakamura.search.processors;

import static org.easymock.EasyMock.expect;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

public abstract class AbstractSearchResultProcessorTest extends AbstractEasyMockTest {

  protected void simpleResultCountCheck(SearchResultProcessor processor) throws RepositoryException, JSONException {
    int itemCount = 12;
    QueryResult queryResult = createMock(QueryResult.class);
    RowIterator results = createMock(RowIterator.class);
    expect(queryResult.getRows()).andReturn(results);
    expect(results.getSize()).andReturn(500L).anyTimes();
    Row row = createMock(Row.class);
    expect(results.hasNext()).andReturn(true).anyTimes();
    expect(results.nextRow()).andReturn(row).times(itemCount);
    PropertyIterator propertyIterator = createMock(PropertyIterator.class);
    expect(propertyIterator.hasNext()).andReturn(false).anyTimes();
    Node dummyNode = createMock(Node.class);
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(row.getNode()).andReturn(dummyNode).anyTimes();

    RequestPathInfo info = createMock(RequestPathInfo.class);
    expect(request.getRequestPathInfo()).andReturn(info).anyTimes();
    expect(info.getSelectors()).andReturn(new String[] {"tidy"}).anyTimes();

    expect(dummyNode.getProperties()).andReturn(propertyIterator).anyTimes();
    expect(dummyNode.getPath()).andReturn("/apath").anyTimes();
    expect(dummyNode.getName()).andReturn("apath").anyTimes();
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
