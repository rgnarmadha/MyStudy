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
package org.sakaiproject.nakamura.proxy;

import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.proxy.ProxyPostProcessor;
import org.sakaiproject.nakamura.api.proxy.ProxyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Will check if the response we get from an RSS file is valid. It will do basic checks
 * such as checking if the Content-Length is < 10K and Content-Type is a valid type.
 */
@Service(value = ProxyPostProcessor.class)
@Component(label = "ProxyPostProcessor for RSS", description = "Post processor who checks if requests are valid RSS requests.", immediate = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai foundation"),
    @Property(name = "service.description", value = "Post processor who checks if requests are valid RSS requests."),
    @Property(name = RSSProxyPostProcessor.EVENTS_THRESHOLD, intValue = RSSProxyPostProcessor.DEFAULT_EVENTS_THRESHOLD),
    @Property(name = RSSProxyPostProcessor.MAX_LENGTH, intValue = RSSProxyPostProcessor.DEFAULT_MAX_LENGTH)
})
public class RSSProxyPostProcessor implements ProxyPostProcessor {

  public static final int DEFAULT_MAX_LENGTH = 10000000;
  public static final int DEFAULT_EVENTS_THRESHOLD = 100;

  static final String EVENTS_THRESHOLD = "sakai.rss.elements.threshold";
  static final String MAX_LENGTH = "sakai.rss.length.max";

  private XMLInputFactory xmlInputFactory;
  private int eventsThreshold;
  private int maxLength;

  // Maximum size is 10 megabyte.
  public static final Logger logger = LoggerFactory
      .getLogger(RSSProxyPostProcessor.class);

  private List<String> contentTypes;

  private Map<String, Set<String>> formats;

  @Activate
  protected void activate(Map<?, ?> props) {
    eventsThreshold = OsgiUtil.toInteger(props.get(EVENTS_THRESHOLD),
        DEFAULT_EVENTS_THRESHOLD);
    maxLength = OsgiUtil.toInteger(props.get(MAX_LENGTH), DEFAULT_MAX_LENGTH);

    xmlInputFactory = new WstxInputFactory();
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, true);
    xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);

    contentTypes = new ArrayList<String>();
    contentTypes.add("application/rss+xml");
    contentTypes.add("application/rdf+xml");
    contentTypes.add("application/atom+xml");
    contentTypes.add("text/xml");
    contentTypes.add("application/xhtml+xml");
    contentTypes.add("application/xml");
    contentTypes.add("text/plain");

    // Formats are stored as:
    //   key = First tag after prologue. Include version if pertinent.
    //   value = Set of required tags.
    formats = new HashMap<String, Set<String>>();

    // RSS 0.91
    HashSet<String> rss091 = new HashSet<String>();
    rss091.add("channel");
    rss091.add("title");
    rss091.add("description");
    rss091.add("link");
    rss091.add("language");
    formats.put("rss-0.91", rss091);

    // RSS 1.0
    HashSet<String> rss10 = new HashSet<String>();
    rss10.add("channel");
    rss10.add("title");
    rss10.add("description");
    rss10.add("link");
    formats.put("rdf", rss10);

    // RSS 2.0
    HashSet<String> rss20 = new HashSet<String>();
    rss20.add("channel");
    rss20.add("title");
    rss20.add("description");
    rss20.add("link");
    formats.put("rss-2.0", rss20);

    // ATOM 1.0
    HashSet<String> atom10 = new HashSet<String>();
    atom10.add("id");
    atom10.add("title");
    atom10.add("updated");
    formats.put("feed", atom10);
  }

  @Deactivate
  protected void deactivate(ComponentContext ctxt) {
    this.xmlInputFactory = null;

    contentTypes = null;
  }

  public String getName() {
    return "rss";
  }

  public static final Logger log = LoggerFactory.getLogger(RSSProxyPostProcessor.class);

  public void process(Map<String, Object> templateParams,
      SlingHttpServletResponse response, ProxyResponse proxyResponse) throws IOException {
    if ( proxyResponse.getResultCode() == HttpServletResponse.SC_PRECONDITION_FAILED ) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "This RSS feed is too big ");
      return;
    }

    Map<String, String[]> headers = proxyResponse.getResponseHeaders();

    // Check if the content-length is smaller than the maximum (if any).
    String[] contentLengthHeader = headers.get("Content-Length");
    if (contentLengthHeader != null) {
      int length = Integer.parseInt(contentLengthHeader[0]);
      if (length > maxLength) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "This RSS feed is too big. The maximum for a feed is: " + maxLength);
        return;
      }
    }

    // Check if the Content-Type we get is valid (if any).
    String[] contentTypeHeader = headers.get("Content-Type");
    if (contentTypeHeader != null) {
      String contentType = contentTypeHeader[0];
      if (contentType.contains(";")) {
        contentType = contentType.substring(0, contentType.indexOf(';'));
      }
      if (!contentTypes.contains(contentType)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "This URL doesn't send a proper Content-Type back");
        return;
      }
    }

    boolean isValid = false;
    InputStream in = proxyResponse.getResponseBodyAsInputStream();
    InputStreamReader reader = new InputStreamReader(in);

    // XMLStreamWriter writer = null;
    XMLEventWriter writer = null;
    ByteArrayOutputStream out = null;

    int i = 0;
    try {
      XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(reader);
      // Create a temporary outputstream where we can write to.
      out = new ByteArrayOutputStream();

      Set<String> checkedElements = null;

      XMLOutputFactory outputFactory = new WstxOutputFactory();
      writer = outputFactory.createXMLEventWriter(out);

      while (eventReader.hasNext()) {
        XMLEvent e = eventReader.nextEvent();
        // Stream it to an output stream.
        writer.add(e);

        if (!isValid) {
          if (e.getEventType() == XMLEvent.START_ELEMENT) {
            StartElement el = e.asStartElement();
            String name = el.getName().getLocalPart().toLowerCase();
            if (checkedElements == null) {
              // get the right format to validate against
              String formatKey = name;
              Attribute attr = el.getAttributeByName(new QName("version"));
              if (attr != null) {
                formatKey += "-" + attr.getValue();
              }
              checkedElements = new HashSet<String>(formats.get(formatKey));
            } else {
              checkedElements.remove(name);

              if (checkedElements.isEmpty()) {
                isValid = true;
              }
            }
          }

          if (i > eventsThreshold) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                "This file is too complex.");
            return;
          }
          i++;
        }
      }

      if (!isValid) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "Invalid RSS file.");
        return;
      }

      // Check if we are not streaming a gigantic file..
      if (out.size() > maxLength) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
            "This file is too big.");
        return;
      }

      for (Entry<String, String[]> h : proxyResponse.getResponseHeaders().entrySet()) {
        for (String v : h.getValue()) {
          response.setHeader(h.getKey(), v);
        }
      }
      // We always return 200 when we get to this point.
      response.setStatus(200);
      response.setHeader("Content-Length", Integer.toString(out.size()));
      // Write the cached stream to the output.
      out.writeTo(response.getOutputStream());

    } catch (XMLStreamException e) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "This is not a valid XML file.");
    } catch (Exception e) {
      logger.warn("Exception reading RSS feed.", e);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          "General exception caught.");
    } finally {
      out.close();
      reader.close();
      try {
        writer.close();
      } catch (XMLStreamException e) {
        // Not much we can do?
        e.printStackTrace();
      }
    }

  }
}
