package org.sakaiproject.nakamura.connections;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.sakaiproject.nakamura.api.profile.ProfileService;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.api.search.SearchUtil;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;

/**
 * Formats connection search results
 */
@Component(immediate = true, description = "Formatter for connection search results", label = "ConnectionSearchResultProcessor")
@Properties(value = { @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.processor", value = "Connection") })
@Service(value = SearchResultProcessor.class)
public class ConnectionSearchResultProcessor implements SearchResultProcessor {

  @Reference
  protected transient ProfileService profileService;

  @Reference
  protected SearchServiceFactory searchServiceFactory;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ConnectionSearchResultProcessor.class);

  public void writeNode(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, Row row) throws JSONException, RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node node = row.getNode();
    if (aggregator != null) {
      aggregator.add(node);
    }
    String targetUser = node.getName();

    UserManager um = AccessControlUtil.getUserManager(session);
    Authorizable au = um.getAuthorizable(targetUser);

    write.object();
    write.key("target");
    write.value(targetUser);
    write.key("profile");
    LOGGER.info("Getting info for {} ", targetUser);
    ValueMap map = profileService.getProfileMap(au, session);
    ((ExtendedJSONWriter) write).valueMap(map);
    write.key("details");
    int maxTraversalDepth = SearchUtil.getTraversalDepth(request);
    ExtendedJSONWriter.writeNodeTreeToWriter(write, node, maxTraversalDepth);
    write.endObject();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.search.SearchResultProcessor#getSearchResultSet(org.apache.sling.api.SlingHttpServletRequest,
   *      javax.jcr.query.Query)
   */
  public SearchResultSet getSearchResultSet(SlingHttpServletRequest request, Query query)
      throws SearchException {
    return searchServiceFactory.getSearchResultSet(request, query);
  }
}
