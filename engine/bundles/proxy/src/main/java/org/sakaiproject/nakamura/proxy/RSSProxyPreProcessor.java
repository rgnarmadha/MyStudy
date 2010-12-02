package org.sakaiproject.nakamura.proxy;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.sakaiproject.nakamura.api.proxy.ProxyPreProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Service(value = ProxyPreProcessor.class)
@Component(label = "ProxyPreProcessor for RSS", description = "Pre processor for RSS requests.", immediate = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai foundation"),
    @Property(name = "service.description", value = "Pre processor who removes all headers from the request.") })
public class RSSProxyPreProcessor implements ProxyPreProcessor {

  public static final Logger logger = LoggerFactory.getLogger(RSSProxyPreProcessor.class);

  public String getName() {
    return "rss";
  }

  public void preProcessRequest(SlingHttpServletRequest request,
      Map<String, String> headers, Map<String, Object> templateParams) {
    headers.clear();
    headers.put("Accept", "*/*");
  }

}
