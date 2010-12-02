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
package org.sakaiproject.nakamura.chat;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.api.chat.ChatManagerService;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;

import javax.servlet.ServletException;

/**
 * Will check if a user has any chat updates.
 */
@SlingServlet(selectors = { "chatupdate" }, resourceTypes = { "sakai/messagestore" }, generateComponent = true, methods = { "GET" })
@Reference(referenceInterface = ChatManagerService.class, name = "ChatManagerService")
@ServiceDocumentation(name = "ChatServlet", shortDescription = "Check for new chat messages.", description = "Provides a mechanism to check if the currently logged in user has new chat messages awaiting.", bindings = @ServiceBinding(type = BindingType.TYPE, bindings = "sakai/messagestore", selectors = @ServiceSelector(name = "chatupdate")), methods = { @ServiceMethod(name = "GET", response = {
    @ServiceResponse(code = 200, description = "Normal retrieval."),
    @ServiceResponse(code = 500, description = "Something went wrong trying to look for an update.") }, description = "GETs to this servlet will produce a JSON object with 3 keys. \n"
    + "<ul><li>update: A boolean that states if there is a new chat message.</li><li>time: The current server time in millisecnds.</li><li>pulltime: The current time in a JCR formatted date.<li></ul>", parameters = @ServiceParameter(name = "t", description = "This variable should hold the last time value retrieved from this servet. If this variable is ommitted it uses the current time.")) })
public class ChatServlet extends SlingSafeMethodsServlet {
  private static final Logger LOGGER = LoggerFactory.getLogger(ChatServlet.class);
  private static final long serialVersionUID = -4011626674940239621L;
  private transient ChatManagerService chatManagerService;

  // We use this format rather than ISO8601 because Jackrabbit uses a subset (8601:2000)
  // See jsr170.pdf - 6.2.5.1
  private final static FastDateFormat dateFormat;
  static {
    dateFormat = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSSZZ");
  }

  protected void bindChatManagerService(ChatManagerService chatManagerService) {
    this.chatManagerService = chatManagerService;
  }

  protected void unbindChatManagerService(ChatManagerService chatManagerService) {
    this.chatManagerService = null;
  }

  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {

    String userID = request.getRemoteUser();
    boolean hasUpdate = false;
    RequestParameter timestampParam = request.getRequestParameter("t");

    long time = System.currentTimeMillis();
    long requestTime = time;

    Long lastUpdate = chatManagerService.get(userID);

    if (lastUpdate == null) {
      // This the first time (ever) the user poll's the chat update.
      // Insert it.
      chatManagerService.put(userID, time);
      hasUpdate = true;
    } else {
      if (timestampParam != null) {
        try {
          time = Long.parseLong(timestampParam.getString());
          if (time < lastUpdate) {
            hasUpdate = true;
          }
        } catch (NumberFormatException e) {
          hasUpdate = true;
          LOGGER
              .info("User requested non-Long timestamp: {}", timestampParam.getString());
        }
      } else {
        hasUpdate = true;
      }
    }

    LOGGER.info("Returned time = {}, update = {}", time, hasUpdate);

    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(time);

    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");


    JSONWriter write = new JSONWriter(response.getWriter());
    try {
      write.object();
      write.key("update");
      write.value(hasUpdate);
      write.key("time");
      write.value(requestTime);
      write.key("pulltime");
      // We use this format rather than ISO8601 because Jackrabbit uses a subset
      // (8601:2000)
      // See jsr170.pdf - 6.2.5.1
      write.value(dateFormat.format(cal));
      write.endObject();
    } catch (JSONException e) {
      LOGGER.warn("Unable to parse JSON for user {} and time {}", userID, time);
      response.sendError(500, "Unable to parse JSON.");
    }

    // Make sure the connection is not keep-alive.
    response.setHeader("Connection", "close");
  }
}
