/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sakaiproject.nakamura.mailman.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.cyberneko.html.parsers.DOMParser;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.message.MessageRoute;
import org.sakaiproject.nakamura.api.proxy.ProxyClientService;
import org.sakaiproject.nakamura.mailman.MailmanManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLCollection;
import org.w3c.dom.html.HTMLTableElement;
import org.w3c.dom.html.HTMLTableRowElement;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

@Component(immediate = true, metatype = true, label = "%mail.manager.impl.label", description = "%mail.manager.impl.desc")
@Service(value = MailmanManager.class)
public class MailmanManagerImpl implements MailmanManager, ManagedService {
  
  private static final Logger LOGGER = LoggerFactory.getLogger(MailmanManagerImpl.class);

  @SuppressWarnings("unused")
  @Property(value = "The Sakai Foundation")
  private static final String SERVICE_VENDOR = "service.vendor";

  @SuppressWarnings("unused")
  @Property(value = "Handles management of mailman integration")
  private static final String SERVICE_DESCRIPTION = "service.description";
  
  @Reference
  private ProxyClientService proxyClientService;
  
  @Property(value = "example.com")
  private static final String MAILMAN_HOST = "mailman.host";
  
  @Property(value = "/cgi-bin/mailman")
  private static final String MAILMAN_PATH = "mailman.path";
  
  @Property(value = "password")
  private static final String LIST_ADMIN_PASSWORD = "mailman.listadmin.password";

  private ImmutableMap<String, String> configMap = ImmutableMap.of();
  
  private String getMailmanUrl(String stub) {
    return "http://" + configMap.get(MAILMAN_HOST) + configMap.get(MAILMAN_PATH) + stub;
  }
  
  public void createList(String listName, String ownerEmail, String password) throws MailmanException {
    HttpClient client = new HttpClient(proxyClientService.getHttpConnectionManager());
    PostMethod post = new PostMethod(getMailmanUrl("/create"));
    NameValuePair[] parametersBody = new NameValuePair[] {
        new NameValuePair("listname", listName),
        new NameValuePair("owner", ownerEmail),
        new NameValuePair("password", password),
        new NameValuePair("confirm", password),
        new NameValuePair("auth", configMap.get(LIST_ADMIN_PASSWORD)),
        new NameValuePair("langs", "en"),
        new NameValuePair("notify", "1"),
        new NameValuePair("autogen", "0"),
        new NameValuePair("moderate", "0"),
        new NameValuePair("doit", "Create List")
    };
    post.setRequestBody(parametersBody);
    try {
      int result = client.executeMethod(post);
      if (result != HttpServletResponse.SC_OK) {
        throw new MailmanException("Unable to create list");
      }
    } catch (HttpException e) {
      throw new MailmanException("HTTP Exception communicating with mailman server", e);
    } catch (IOException e) {
      throw new MailmanException("IOException communicating with mailman server", e);
    } finally {
      post.releaseConnection();
    }
  }

  public void deleteList(String listName, String listPassword) throws MailmanException {
    HttpClient client = new HttpClient(proxyClientService.getHttpConnectionManager());
    PostMethod post = new PostMethod(getMailmanUrl("/rmlist/" + listName));
    NameValuePair[] parametersBody = new NameValuePair[] {
        new NameValuePair("password", listPassword),
        new NameValuePair("delarchives", "0"),
        new NameValuePair("doit", "Delete this list")
    };
    post.setRequestBody(parametersBody);
    try {
      int result = client.executeMethod(post);
      if (result != HttpServletResponse.SC_OK) {
        throw new MailmanException("Unable to create list");
      }
    } catch (HttpException e) {
      throw new MailmanException("HTTP Exception communicating with mailman server", e);
    } catch (IOException e) {
      throw new MailmanException("IOException communicating with mailman server", e);
    } finally {
      post.releaseConnection();
    }
  }

  public boolean listHasMember(String listName, String listPassword, String memberEmail) throws MailmanException {
    HttpClient client = new HttpClient(proxyClientService.getHttpConnectionManager());
    GetMethod get = new GetMethod(getMailmanUrl("/admin/" + listName + "members"));
    NameValuePair[] parameters = new NameValuePair[] {
        new NameValuePair("findmember", memberEmail),
        new NameValuePair("setmemberopts_btn", ""),
        new NameValuePair("adminpw", listPassword)
    };
    get.setQueryString(parameters);
    try {
      int result = client.executeMethod(get);
      if (result != HttpServletResponse.SC_OK) {
        throw new MailmanException("Unable to search for member");
      }
      Document dom = parseHtml(get);
      NodeList inputs = dom.getElementsByTagName("INPUT");
      String unsubString = URLEncoder.encode(memberEmail, "utf8") + "_unsub";
      for (int i=0; i<inputs.getLength(); i++) {
        Node input = inputs.item(i);
        try {
          if (input.getAttributes().getNamedItem("name").getTextContent().equals(unsubString)) {
            return true;
          }
        } catch (NullPointerException npe) {
        }
      }
    } catch (SAXException e) {
      throw new MailmanException("Error parsing mailman response", e);
    } catch (HttpException e) {
      throw new MailmanException("HTTP Exception communicating with mailman server", e);
    } catch (IOException e) {
      throw new MailmanException("IOException communicating with mailman server", e);
    } finally {
      get.releaseConnection();
    }
    return false;
  }
  
  public boolean addMember(String listName, String listPassword, String memberEmail) throws MailmanException {
    HttpClient client = new HttpClient(proxyClientService.getHttpConnectionManager());
    GetMethod get = new GetMethod(getMailmanUrl("/admin/" + listName + "/members/add"));
    NameValuePair[] parameters = new NameValuePair[] {
      new NameValuePair("subscribe_or_invite", "0"),
      new NameValuePair("send_welcome_msg_to_this_batch", "0"),
      new NameValuePair("notification_to_list_owner", "0"),
      new NameValuePair("subscribees_upload", memberEmail),
      new NameValuePair("adminpw", listPassword)
    };
    get.setQueryString(parameters);
    try {
      int result = client.executeMethod(get);
      if (result != HttpServletResponse.SC_OK) {
        throw new MailmanException("Unable to add member");
      }
      Document dom = parseHtml(get);
      NodeList inputs = dom.getElementsByTagName("h5");
      if (inputs.getLength() == 0) {
        throw new MailmanException("Unable to read result status");
      }
      return "Successfully subscribed:".equals(inputs.item(0).getTextContent());
    } catch (SAXException e) {
      throw new MailmanException("Error parsing mailman response", e);
    } catch (HttpException e) {
      throw new MailmanException("HTTP Exception communicating with mailman server", e);
    } catch (IOException e) {
      throw new MailmanException("IOException communicating with mailman server", e);
    } finally {
      get.releaseConnection();
    }
  }

  public boolean removeMember(String listName, String listPassword, String memberEmail) throws MailmanException {
    HttpClient client = new HttpClient(proxyClientService.getHttpConnectionManager());
    GetMethod get = new GetMethod(getMailmanUrl("/admin/" + listName + "/members/remove"));
    NameValuePair[] parameters = new NameValuePair[] {
      new NameValuePair("send_unsub_ack_to_this_batch", "0"),
      new NameValuePair("send_unsub_notifications_to_list_owner", "0"),
      new NameValuePair("unsubscribees_upload", memberEmail),
      new NameValuePair("adminpw", listPassword)
    };
    get.setQueryString(parameters);
    try {
      int result = client.executeMethod(get);
      if (result != HttpServletResponse.SC_OK) {
        throw new MailmanException("Unable to add member");
      }
      Document dom = parseHtml(get);
      NodeList inputs = dom.getElementsByTagName("h5");
      if (inputs.getLength() == 0) {
        inputs = dom.getElementsByTagName("h3");
        if (inputs.getLength() == 0) {
          throw new MailmanException("Unable to read result status");
        }
      }
      return "Successfully Unsubscribed:".equals(inputs.item(0).getTextContent());
    } catch (HttpException e) {
      throw new MailmanException("HTTP Exception communicating with mailman server", e);
    } catch (IOException e) {
      throw new MailmanException("IOException communicating with mailman server", e);
    } catch (SAXException e) {
      throw new MailmanException("Error parsing mailman response", e);
    } finally {
      get.releaseConnection();
    }
  }

  private Document parseHtml(HttpMethodBase method) throws SAXException, IOException {
    DOMParser parser = new DOMParser();
    parser.parse(new InputSource(method.getResponseBodyAsStream()));
    return parser.getDocument();
  }

  public List<String> getLists() throws MailmanException {
    HttpClient client = new HttpClient(proxyClientService.getHttpConnectionManager());
    GetMethod get = new GetMethod(getMailmanUrl("/admin"));
    List<String> lists = new ArrayList<String>();
    try {
      int result = client.executeMethod(get);
      if (result != HttpServletResponse.SC_OK) {
        LOGGER.warn("Got " + result + " from http request");
        throw new MailmanException("Unable to list mailinglists");
      }
      DOMParser parser = new DOMParser();
      parser.parse(new InputSource(get.getResponseBodyAsStream()));
      Document doc = parser.getDocument();
      NodeList tableNodes = doc.getElementsByTagName("table");
      if (tableNodes.getLength() < 2) {
        throw new MailmanException("Unrecognised page format.");
      }
      HTMLTableElement mainTable = (HTMLTableElement) tableNodes.item(0);
      HTMLCollection rows = mainTable.getRows();
      for (int i=4; i<rows.getLength(); i++) {
        lists.add(parseListNameFromRow((HTMLTableRowElement)rows.item(i)));
      }
    } catch (SAXException e) {
      throw new MailmanException("Error parsing mailman response", e);
    } catch (HttpException e) {
      throw new MailmanException("HTTP Exception communicating with mailman server", e);
    } catch (IOException e) {
      throw new MailmanException("IOException communicating with mailman server", e);
    } finally {
      get.releaseConnection();
    }
    return lists;
  }

  private String parseListNameFromRow(HTMLTableRowElement item) throws MailmanException {
    HTMLCollection cells = item.getCells();
    if (cells.getLength() != 2) {
      throw new MailmanException("Unexpected table row format");
    }
    return cells.item(0).getTextContent();
  }

  void setServer(String mailmanHost) {
    Builder<String,String> builder = ImmutableMap.builder();
    builder.putAll(configMap);
    builder.put(MAILMAN_HOST, mailmanHost);
    configMap = builder.build();
  }

  void setMailmanPath(String mailmanPath) {
    Builder<String,String> builder = ImmutableMap.builder();
    builder.putAll(configMap);
    builder.put(MAILMAN_PATH, mailmanPath);
    configMap = builder.build();
  }

  ProxyClientService getProxyClientService() {
    return proxyClientService;
  }

  void setProxyClientService(ProxyClientService proxyClientService) {
    this.proxyClientService = proxyClientService;
  }

  public boolean isServerActive() throws MailmanException {
    HttpClient client = new HttpClient(proxyClientService.getHttpConnectionManager());
    GetMethod get = new GetMethod(getMailmanUrl("/admin"));
    try {
      int result = client.executeMethod(get);
      if (result != HttpServletResponse.SC_OK) {
        LOGGER.warn("Got " + result + " from http request");
        return false;
      }
      return true;
    } catch (HttpException e) {
      throw new MailmanException("HTTP Exception communicating with mailman server", e);
    } catch (IOException e) {
      throw new MailmanException("IOException communicating with mailman server", e);
    } finally {
      get.releaseConnection();
    }
  }

  protected void activate(ComponentContext componentContext) {
    LOGGER.info("Got component initialization");
    Builder<String, String> builder = ImmutableMap.builder();
    for (Enumeration<?> e = componentContext.getProperties().keys(); e.hasMoreElements();) {
      String key = (String)e.nextElement();
      Object value = componentContext.getProperties().get(key);
      if (value instanceof String) {
        builder.put(key, (String) value);
      }
    }
    configMap = builder.build();
  }

  @SuppressWarnings("unchecked")
  public void updated(Dictionary config) throws ConfigurationException {
    LOGGER.info("Got config update");
    Builder<String, String> builder = ImmutableMap.builder();
    for (Enumeration<?> e = config.keys(); e.hasMoreElements();) {
      String k = (String) e.nextElement();
      builder.put(k, (String) config.get(k));
    }
    configMap = builder.build();
  }

  public MessageRoute generateMessageRouteForGroup(String groupName) {
    return new MailmanMessageRoute(groupName + "@" + configMap.get(MAILMAN_HOST), "smtp");
  }

}
