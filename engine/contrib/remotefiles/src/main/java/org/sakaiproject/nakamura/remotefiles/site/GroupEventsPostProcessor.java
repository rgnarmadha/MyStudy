package org.sakaiproject.nakamura.remotefiles.site;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
//import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.sakaiproject.nakamura.api.site.SiteService.SiteEvent;
import org.sakaiproject.nakamura.remotefiles.RemoteFilesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Component(enabled = true, immediate = true, metatype = true)
@Service(value = EventHandler.class)
@Properties(value = {
    @Property(name = "service.vendor", value = "Sakai Foundation"),
    @Property(name = "service.description", value = "Provides a place to respond when sites are created and memberships updated"),
    @Property(name = "event.topics", value = "org/sakaiproject/nakamura/api/site/event/created") })
// SiteService.SiteEvent.created.getTopic()
public class GroupEventsPostProcessor implements EventHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GroupEventsPostProcessor.class);

  @Reference
  RemoteFilesRepository remoteFilesRepository;

  @Activate
  protected void activate(Map<?, ?> props) {
    //sitesReplacement = OsgiUtil.toString(SITES_REPLACEMENT, DEFAULT_SITES_REPLACEMENT);
  }

  public void handleEvent(Event event) {
    String sitePath = (String) event.getProperty(SiteEvent.SITE);
    String userId = (String) event.getProperty(SiteEvent.USER);
    try {
      remoteFilesRepository.createGroup(sitePath, userId);
      remoteFilesRepository.createDirectory(userId, null, "/sites", sitePath);
    } catch (Exception e1) {
      LOGGER.warn("failed to create remote files group when creating site: " + e1.getMessage());
    }
  }

}
