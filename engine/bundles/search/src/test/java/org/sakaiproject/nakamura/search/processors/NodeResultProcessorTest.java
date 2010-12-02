package org.sakaiproject.nakamura.search.processors;

import org.apache.sling.commons.json.JSONException;
import org.junit.Test;
import org.sakaiproject.nakamura.search.SearchServiceFactoryImpl;

import javax.jcr.RepositoryException;

public class NodeResultProcessorTest extends AbstractSearchResultProcessorTest {

  @Test
  public void testResultCountLimit() throws RepositoryException, JSONException {
    NodeSearchResultProcessor nodeSearchResultProcessor = new NodeSearchResultProcessor(new SearchServiceFactoryImpl());
    simpleResultCountCheck(nodeSearchResultProcessor);
  }
}
