package org.sakaiproject.nakamura.mailman.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.message.MessageRouter;
import org.sakaiproject.nakamura.api.message.MessageRoutes;
import org.sakaiproject.nakamura.mailman.MailmanManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;

@Component(inherit = true, immediate = true, label = "%mail.manager.router.label")
@Service
public class MailmanMessageRouter implements MessageRouter {

  private static final Logger LOGGER = LoggerFactory.getLogger(MailmanMessageRouter.class);
  
  @SuppressWarnings("unused")
  @Property(value = "The Sakai Foundation")
  private static final String SERVICE_VENDOR = "service.vendor";

  @SuppressWarnings("unused")
  @Property(value = "Manages Routing for group mailing lists.")
  private static final String SERVICE_DESCRIPTION = "service.description";

  @Reference
  private MailmanManager mailmanManager;
  
  public int getPriority() {
    return 1;
  }

  public void route(Node n, MessageRoutes routing) {
    LOGGER.info("Mailman routing message: " + n);
    List<MessageRoute> toRemove = new ArrayList<MessageRoute>();
    List<MessageRoute> toAdd = new ArrayList<MessageRoute>();
    for (MessageRoute route : routing) {
      if ("internal".equals(route.getTransport()) && route.getRcpt().startsWith("g-")) {
        LOGGER.info("Found an internal group message. Routing to SMTP");
        toRemove.add(route);
        toAdd.add(mailmanManager.generateMessageRouteForGroup(route.getRcpt()));
      }
    }
    routing.removeAll(toRemove);
    routing.addAll(toAdd);
  }

}
