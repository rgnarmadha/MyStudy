package org.sakaiproject.nakamura.search;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.sakaiproject.nakamura.api.search.SearchConstants.PARAMS_PAGE;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_AGGREGATE;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_BATCHRESULTPROCESSOR;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_LIMIT_RESULTS;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_PROPERTY_PROVIDER;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_QUERY_LANGUAGE;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_QUERY_TEMPLATE;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_RESULTPROCESSOR;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchConstants;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.jcr.Workspace;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

public class SearchServletTest extends AbstractEasyMockTest {

  private SlingHttpServletRequest request;
  private SlingHttpServletResponse response;
  private SearchServlet searchServlet;
  private StringWriter stringWriter;
  private SearchResultProcessor proc;

  private static final String SQL_QUERY = "select * from \\y where x = '{q}'";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    searchServlet = new SearchServlet();
    searchServlet.searchServiceFactory = new SearchServiceFactoryImpl();
    searchServlet.init();
  }

  public void testNotInVar() throws ServletException, IOException {
    Node node = createMock(Node.class);

    Resource resource = createMock(Resource.class);
    expect(resource.getPath()).andReturn("/_user/b/bo/bob/private/evilsearchtemplate");
    expect(resource.adaptTo(Node.class)).andReturn(node);

    request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);

    response = createMock(SlingHttpServletResponse.class);
    response.sendError(HttpServletResponse.SC_FORBIDDEN,
        "Search templates can only be executed if they are located under "
            + SearchConstants.SEARCH_PATH_PREFIX);
    replay();

    searchServlet.doGet(request, response);

    verify();
  }

  @Test
  public void testNoQueryTemplate() throws ValueFormatException,
      RepositoryException, IOException, ServletException {
    Node node = createMock(Node.class);
    expect(node.hasProperty(SAKAI_QUERY_TEMPLATE)).andReturn(false);

    Resource resource = createMock(Resource.class);
    expect(resource.getPath()).andReturn("/var/dummy");
    expect(resource.adaptTo(Node.class)).andReturn(node);

    request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);

    response = createMock(SlingHttpServletResponse.class);
    replay();

    searchServlet.doGet(request, response);

    verify();
  }

  @Test
  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"DLS_DEAD_LOCAL_STORE"})
  public void testGoodQuery() throws ValueFormatException, RepositoryException,
      IOException, ServletException {

    Row row = createMock(Row.class);

    Node queryNode = prepareNodeSessionWithQueryManagerAndResultNode(
        row, "select * from y where x = 'foo' and u = 'admin' ");

    addStringPropertyToNode(queryNode, SAKAI_QUERY_TEMPLATE, "select * from y where x = 'foo' and u = '{_userId}' ");
    addStringPropertyToNode(queryNode, SAKAI_QUERY_LANGUAGE, Query.SQL);
    expect(queryNode.hasProperty(SAKAI_PROPERTY_PROVIDER)).andReturn(false).anyTimes();
    expect(queryNode.hasProperty(SAKAI_BATCHRESULTPROCESSOR)).andReturn(false).anyTimes();
    expect(queryNode.hasProperty(SAKAI_AGGREGATE)).andReturn(false).anyTimes();
    expect(queryNode.hasProperty(SAKAI_LIMIT_RESULTS)).andReturn(false).anyTimes();

    Resource resource = createMock(Resource.class);
    expect(resource.getPath()).andReturn("/var/dummy");
    expect(resource.adaptTo(Node.class)).andReturn(queryNode);

    request = createMock(SlingHttpServletRequest.class);
    expect(request.getRemoteUser()).andReturn("admin");
    expect(request.getResource()).andReturn(resource);
    expect(request.getRequestParameter(PARAMS_PAGE)).andReturn(null).anyTimes();
    addStringRequestParameter(request, "items", "25");
    addStringRequestParameter(request, "q", "foo");

    PropertyIterator propertyIterator = createNiceMock(PropertyIterator.class);
    expect(queryNode.getProperties()).andReturn(propertyIterator);

    
    RequestPathInfo info = createMock(RequestPathInfo.class);
    expect(request.getRequestPathInfo()).andReturn(info);
    expect(info.getSelectors()).andReturn(new String[] {"tidy", "2"});

    Authorizable au = createAuthorizable("admin", false, true);

    UserManager um = createMock(UserManager.class);
    expect(um.getAuthorizable("admin")).andReturn(au);

    JackrabbitSession session = createMock(JackrabbitSession.class);
    expect(session.getUserManager()).andReturn(um);

    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session);
    expect(request.getResourceResolver()).andReturn(resourceResolver);

    executeQuery(queryNode);
  }

  @Test
  public void testDefaultLanguageAndBadItemCount() throws ValueFormatException,
      RepositoryException, IOException, ServletException {
    executeSimpleQueryWithNoResults("foo", "NAN",
        "select * from y where x = 'foo'");
  }

  @Test
  public void testSqlEscaping() throws RepositoryException, IOException,
      ServletException {
    executeSimpleQueryWithNoResults("fo'o", "NAN",
        "select * from y where x = 'fo\\''o'");
  }

  @Test
  public void testRepositoryExceptionHandling() throws Exception {
    Node queryNode = createMock(Node.class);

    addStringPropertyToNode(queryNode, SAKAI_QUERY_TEMPLATE, SQL_QUERY);
    expect(queryNode.hasProperty(SAKAI_QUERY_LANGUAGE)).andThrow(
        new RepositoryException());

    Resource resource = createMock(Resource.class);
    expect(resource.getPath()).andReturn("/var/dummy");
    expect(resource.adaptTo(Node.class)).andReturn(queryNode);

    request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);

    response = createMock(SlingHttpServletResponse.class);
    response.sendError(500, null);
    expectLastCall();

    searchServlet = new SearchServlet();

    replay();

    searchServlet.doGet(request, response);

    verify();
  }

  private void executeSimpleQueryWithNoResults(String queryParameter,
      String itemCount, String expectedSqlQuery) throws RepositoryException,
      IOException, ServletException {
    Node queryNode = prepareNodeSessionWithQueryManagerAndResultNode(null,
        expectedSqlQuery);

    addStringPropertyToNode(queryNode, SAKAI_QUERY_TEMPLATE, SQL_QUERY);
    expect(queryNode.hasProperty(SAKAI_QUERY_LANGUAGE)).andReturn(false);
    expect(queryNode.hasProperty(SAKAI_PROPERTY_PROVIDER)).andReturn(false).anyTimes();
    expect(queryNode.hasProperty(SAKAI_BATCHRESULTPROCESSOR)).andReturn(false).anyTimes();
    expect(queryNode.hasProperty(SAKAI_AGGREGATE)).andReturn(false).anyTimes();
    expect(queryNode.hasProperty(SAKAI_LIMIT_RESULTS)).andReturn(false).anyTimes();

    Resource resource = createMock(Resource.class);
    expect(resource.getPath()).andReturn("/var/dummy");
    expect(resource.adaptTo(Node.class)).andReturn(queryNode);

    PropertyIterator propertyIterator = createNiceMock(PropertyIterator.class);
    expect(queryNode.getProperties()).andReturn(propertyIterator);

    ResourceResolver resourceResolver = createMock(ResourceResolver.class);
    Session session = createMock(Session.class);
    request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);
    expect(request.getRemoteUser()).andReturn("bob").anyTimes();
    expect(request.getRequestParameter(PARAMS_PAGE)).andReturn(null).anyTimes();
    expect(resourceResolver.adaptTo(Session.class)).andReturn(session).anyTimes();
    expect(request.getResourceResolver()).andReturn(resourceResolver).anyTimes();
    addStringRequestParameter(request, "items", itemCount);
    addStringRequestParameter(request, "q", queryParameter);

    RequestPathInfo info = createMock(RequestPathInfo.class);
    expect(request.getRequestPathInfo()).andReturn(info);
    expect(info.getSelectors()).andReturn(new String[0]);

    executeQuery(queryNode);
  }

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"DLS_DEAD_LOCAL_STORE"})
  private Node prepareNodeSessionWithQueryManagerAndResultNode(Row resultRow,
      String expectedQuery) throws RepositoryException {
    Node queryNode = createMock(Node.class);

    final RowIterator iterator = createMock(RowIterator.class);
    if (resultRow == null) {
      expect(iterator.hasNext()).andReturn(false);
    } else {
      expect(iterator.hasNext()).andReturn(true);
      expect(iterator.nextRow()).andReturn(resultRow);
      expect(iterator.hasNext()).andReturn(false);
    }
    @SuppressWarnings("unused")
    QueryResult queryResult = createMock(QueryResult.class);
    //expect(queryResult.getRows()).andReturn(iterator);

    proc = new SearchResultProcessor() {
      public void writeNode(SlingHttpServletRequest request, JSONWriter write,
          Aggregator aggregator, Row row) throws JSONException, RepositoryException {
        // TODO Auto-generated method stub
      }

      public SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
          Query query) throws SearchException {
        return new SearchResultSetImpl(iterator,100);
      }
    };

    Query query = createMock(Query.class);
    // expect(query.execute()).andReturn(queryResult);

    QueryManager queryManager = createMock(QueryManager.class);
    expect(queryManager.createQuery(expectedQuery, Query.SQL)).andReturn(query);

    Workspace workspace = createMock(Workspace.class);
    expect(workspace.getQueryManager()).andReturn(queryManager);

    Session session = createMock(Session.class);
    expect(session.getWorkspace()).andReturn(workspace);

    expect(queryNode.getSession()).andReturn(session);

    return queryNode;
  }

  private void executeQuery(Node queryNode) throws IOException,
      ServletException, RepositoryException {
    stringWriter = new StringWriter();
    response = createMock(SlingHttpServletResponse.class);
    response.setContentType("application/json");
    expectLastCall();
    response.setCharacterEncoding("UTF-8");
    expectLastCall();
    expect(response.getWriter()).andReturn(new PrintWriter(stringWriter));
    searchServlet = new SearchServlet();
    searchServlet.defaultSearchProcessor = proc;
    expect(queryNode.hasProperty(SAKAI_RESULTPROCESSOR)).andReturn(false)
        .anyTimes();

    replay();

    searchServlet.doGet(request, response);
    stringWriter.close();

    verify();
  }
}
