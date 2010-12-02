package org.sakaiproject.nakamura.search.processors;

import static org.sakaiproject.nakamura.api.search.SearchUtil.escapeString;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.util.ISO9075;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.jcr.query.Query;

/**
 * Provides properties to process the search
 * 
 */
@Component(immediate = true, label = "PageSearchPropertyProvider", description = "Formatter for page search results.")
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "sakai.search.provider", value = "Page")
    })
@Service(value = SearchPropertyProvider.class)
public class PageSearchPropertyProvider implements SearchPropertyProvider {

  public static final String PROP_PAGE_TYPE = "sakai:type";

  public static final Logger LOG = LoggerFactory
      .getLogger(PageSearchPropertyProvider.class);

  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    LOG.info("loading properties.");
    RequestParameter pathParam = request.getRequestParameter("path");
    RequestParameter[] properties = request.getRequestParameters("properties");
    RequestParameter[] values = request.getRequestParameters("values");
    RequestParameter[] operators = request.getRequestParameters("operators");

    String path = request.getResource().getPath();
    String filter = "";

    if (properties != null && values != null && operators != null
        && properties.length == values.length && values.length == operators.length) {
      for (int i = 0; i < properties.length; i++) {
        String op = operators[i].getString();
        if (op.equals(">") || op.equals("=") || op.equals("<")) {
          filter += " and @" + properties[i].getString() + operators[i].getString() + '"'
              + values[i].getString() + '"';
        }
      }
    }

    if (pathParam != null) {
      path = pathParam.getString();
    }

    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }

    propertiesMap.put("_filter", escapeString(filter, Query.XPATH));
    propertiesMap.put("_path", ISO9075.encodePath(path));
  }

}
