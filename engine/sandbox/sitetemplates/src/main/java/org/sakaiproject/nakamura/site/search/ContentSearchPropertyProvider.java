package org.sakaiproject.nakamura.site.search;

import static org.sakaiproject.nakamura.api.search.SearchUtil.escapeString;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.sakaiproject.nakamura.api.search.SearchPropertyProvider;

import java.util.Map;

import javax.jcr.query.Query;

@Component(immediate = true, name = "ContentSearchPropertyProvider", label = "ContentSearchPropertyProvider", description = "Provides general properties for the content search")
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Provides general properties for the content search"),
    @Property(name = "sakai.search.provider", value = "Content")
})
@Service(value = SearchPropertyProvider.class)
public class ContentSearchPropertyProvider implements SearchPropertyProvider {

  private static final String SITE_PARAM = "site";

  public void loadUserProperties(SlingHttpServletRequest request,
      Map<String, String> propertiesMap) {
    RequestParameter siteParam = request.getRequestParameter(SITE_PARAM);
    if (siteParam != null) {
      String site = " AND @id = '" + escapeString(siteParam.getString(), Query.XPATH) + "'";
      propertiesMap.put("_site", site);
    }
  }
}
