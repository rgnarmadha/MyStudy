/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.message;

import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_FROM;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_PREVIOUS_MESSAGE;
import static org.sakaiproject.nakamura.api.message.MessageConstants.PROP_SAKAI_TO;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.message.MessageProfileWriter;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.api.search.SearchException;
import org.sakaiproject.nakamura.api.search.SearchResultProcessor;
import org.sakaiproject.nakamura.api.search.SearchResultSet;
import org.sakaiproject.nakamura.api.search.SearchServiceFactory;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.sakaiproject.nakamura.util.RowUtils;
import org.sakaiproject.nakamura.util.StringUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.Row;

/**
 * Formats message node search results
 */
@Component(immediate = true, label = "MessageSearchResultProcessor", description = "Processor for message search results.")
@Service
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Processor for message search results."),
    @Property(name = "sakai.search.processor", value = "Message"),
    @Property(name = "sakai.seach.resourcetype", value = "sakai/message") })
public class MessageSearchResultProcessor implements SearchResultProcessor {

  @Reference
  protected transient MessagingService messagingService;
  @Reference
  protected transient SearchServiceFactory searchServiceFactory;

  protected MessageProfileWriterTracker tracker;

  protected void activate(ComponentContext context) {
    BundleContext bundleContext = context.getBundleContext();
    tracker = new MessageProfileWriterTracker(bundleContext);
    tracker.open();
  }

  protected void deactivate(ComponentContext context) {
    if (tracker != null) {
      tracker.close();
      tracker = null;
    }
  }

  /**
   * Parses the message to a usable JSON format for the UI.
   * 
   * @param write
   * @param resultNode
   * @throws JSONException
   * @throws RepositoryException
   */
  public void writeNode(SlingHttpServletRequest request, JSONWriter write,
      Aggregator aggregator, Row row) throws JSONException, RepositoryException {
    Session session = request.getResourceResolver().adaptTo(Session.class);
    Node resultNode = RowUtils.getNode(row, session);
    if (aggregator != null) {
      aggregator.add(resultNode);
    }
    writeNode(request, write, resultNode);
  }

  public void writeNode(SlingHttpServletRequest request, JSONWriter write, Node resultNode)
      throws JSONException, RepositoryException {

    write.object();

    // Write out all the properties on the message.
    ExtendedJSONWriter.writeNodeContentsToWriter(write, resultNode);

    // Add some extra properties.
    write.key("id");
    write.value(resultNode.getName());

    Session session = resultNode.getSession();

    // Write out all the recipients their information on this message.
    // We always return this as an array, even if it is only 1 recipient.
    MessageProfileWriter defaultProfileWriter = tracker.getMessageProfileWriterByType("internal");
    if (resultNode.hasProperty(PROP_SAKAI_TO)) {
      String toVal = resultNode.getProperty(PROP_SAKAI_TO).getString();
      String[] rcpts = StringUtils.split(toVal, ',');
      write.key("userTo");
      write.array();
      for (String rcpt : rcpts) {
        String[] values = StringUtils.split(rcpt, ':');
        MessageProfileWriter writer = null;
        // usually it should be type:user. But in case the handler changed this..
        String user = values[0];
        if (values.length == 2) {
          user = values[1];
          String type = values[0];
          writer = tracker.getMessageProfileWriterByType(type);
        }
        if (writer == null) {
          writer = defaultProfileWriter;
        }
        writer.writeProfileInformation(session, user, write);
      }
      write.endArray();
    }

    // Although in most cases the sakai:from field will only contain 1 value.
    // We add in the option to support multiple cases.
    // For now we expect it to always be the user who sends the message.
    if (resultNode.hasProperty(PROP_SAKAI_FROM)) {
      String fromVal = resultNode.getProperty(PROP_SAKAI_FROM).getString();
      String[] senders = StringUtils.split(fromVal, ',');
      write.key("userFrom");
      write.array();
      for (String sender : senders) {
        defaultProfileWriter.writeProfileInformation(session, sender, write);
      }
      write.endArray();
    }

    // Write the previous message.
    if (resultNode.hasProperty(PROP_SAKAI_PREVIOUS_MESSAGE)) {
      write.key("previousMessage");
      parsePreviousMessages(request, write, resultNode);
    }
    write.endObject();
  }

  /**
   * Parse a message we have replied on.
   * 
   * @param request
   * @param node
   * @param write
   * @param excerpt
   * @throws JSONException
   * @throws RepositoryException
   */
  private void parsePreviousMessages(SlingHttpServletRequest request, JSONWriter write,
      Node node) throws JSONException, RepositoryException {

    Session s = node.getSession();
    String id = node.getProperty(PROP_SAKAI_PREVIOUS_MESSAGE).getString();
    String path = messagingService.getFullPathToMessage(s.getUserID(), id, s);
    Node previousMessage = (Node) s.getItem(path);
    writeNode(request, write, previousMessage);
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
