package org.sakaiproject.nakamura.site.search;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.api.site.SiteException;
import org.sakaiproject.nakamura.api.site.SiteService;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.RowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;

/**
 * Formats user profile node search results
 */
@Component(immediate = true, label = "%siteSearch.result.processor.label", description = "%siteSearch.result.processor.desc")
@Service
public class SiteSearchResultProcessor implements SearchResultProcessor {

  @Reference
  private SiteService siteService;

  @Reference
  private SearchServiceFactory searchServiceFactory;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SiteSearchResultProcessor.class);

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "Site")
  static final String SEARCH_PROCESSOR = "sakai.search.processor";

  @Property(value = "sakai/site")
  static final String RESOURCE_TYPE = "sakai.search.resourcetype";

  public void writeNode(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, Row row) throws JSONException, RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node resultNode = RowUtils.getNode(row, session);
    if (!siteService.isSite(resultNode)) {
      LOGGER.warn("Search result was not a site node: " + resultNode.getPath());
      throw new JSONException("Unable to write non-site node result");
    }
    if (aggregator != null) {
      aggregator.add(resultNode);
    }
    writeNode(write, resultNode);
  }

  public void writeNode(JSONWriter write, Node resultNode)
      throws JSONException, RepositoryException {
    write.object();
    write.key("member-count");
    int count;
    try {
      count = siteService.getMemberCount(resultNode);
    } catch (SiteException e) {
      count = 0;
    }
    write.value(count);
    write.key("path");
    write.value(resultNode.getPath());
    ExtendedJSONWriter.writeNodeContentsToWriter(write, resultNode);
    write.endObject();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request,
      Query query) throws SearchException {
    return searchServiceFactory.getSearchResultSet(request, query);
  }

  protected void bindSiteService(SiteService siteService) {
    this.siteService = siteService;
  }

  protected void unbindSiteService(SiteService siteService) {
    this.siteService = null;
  }

}
