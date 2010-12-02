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

import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceParameter;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.doc.ServiceSelector;
import org.sakaiproject.nakamura.api.message.CreateMessagePreProcessor;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessagingException;
import org.sakaiproject.nakamura.api.message.MessagingService;
import org.sakaiproject.nakamura.api.user.UserConstants;
import org.sakaiproject.nakamura.message.internal.InternalCreateMessagePreProcessor;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * Will create a message in the user his mesagestore (/_user/s/si/simong/messages/... folder. If the box is set to outbox and
 * the sendstate property to pending or none it will be picked up by the
 * MessagePostProcessor who will then send an OSGi event that feeds it to the correct
 * MessageHandler.
 */
@SlingServlet(resourceTypes = { "sakai/messagestore" }, selectors = { "create" }, methods = { "POST" }, generateComponent = true, generateService = true)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Endpoint to create a message") })
@Reference(name = "createMessagePreProcessor", referenceInterface = CreateMessagePreProcessor.class, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, policy = ReferencePolicy.DYNAMIC)
@ServiceDocumentation(
    name = "CreateMessageServlet",
    shortDescription = "Create a message.",
    description = "Create a message by doing a POST to messagestore.create.html . By default there are stores under each site and at /_user/u/us/user/message and /_group/g/gr/group/message",
    bindings = @ServiceBinding(type = BindingType.TYPE, 
        bindings = "sakai/messagestore", 
        selectors = @ServiceSelector(name = "create")), 
    methods = @ServiceMethod(name = "POST",
        description = "Create a message. <br />" +
        "A message will only be sent if it has the sakai:messagebox property set to 'sent' and the sakai:sendstate property set to 'pending'. " +
        "This means the actual sending of a message can be done by just doing a request to a message-node. " +
        "This servlet will only create message nodes. " + 
        "All other POST headers sent along in this request will end up as properties on the message-node. <br />" +
        "Example:<br />" +
        "curl -d\"sakai:to=internal:user1\" -d\"sakai:subject=Title\" -d\"sakai:type=internal\" -d\"sakai:body=Loremlipsum\" -d\"sakai:messagebox=outbox\" -d\"sakai:category=message\" -d\"sakai:sendstate=pending\" http://user2:test2@localhost:8080/_user/message.create.html",
        response = {
          @ServiceResponse(code = 200, description = "The servlet will send a JSON response which holds 2 keys." + 
            "<ul><li>id: The id for the newly created message.</li><li>message: This is an object which will hold all the key/values for the newly created message.</li></ul>"),
          @ServiceResponse(code = 400, description = "The request did not contain all the (correct) parameters."),
          @ServiceResponse(code = 401, description = "The user is not logged. Anonymous users are not allowed to send messages."),
          @ServiceResponse(code = 500, description = "The server was unable to create the message.")},
        parameters = {
          @ServiceParameter(name = "sakai:to", description = "Comma seperated list of recipients. A messageroute should be specified for each recipient.eg: sakai:to=interal:admin,smtp:address@email.com. Note that each messageroute has it's own checks!"),
          @ServiceParameter(name = "sakai:messagebox", description = "This specifies in which box the message is located. Note: This is just a property on the message(=node). The message will not fysicly be saved on a different location. eg: sakai:messagebox=outbox"),
          @ServiceParameter(name = "sakai:sendstate", description = "The state this message is in. eg: sakai:sendsate=pending") }))
public class CreateMessageServlet extends SlingAllMethodsServlet {

  /**
   *
   */
  private static final long serialVersionUID = 3813877071190736742L;
  private static final Logger LOGGER = LoggerFactory
      .getLogger(CreateMessageServlet.class);
  
  protected Map<String, CreateMessagePreProcessor> processors = new ConcurrentHashMap<String, CreateMessagePreProcessor>();

  @Reference
  protected transient MessagingService messagingService;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.message.AbstractMessageServlet#handleOperation(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.servlets.HtmlResponse, java.util.List)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request,
      org.apache.sling.api.SlingHttpServletResponse response)
      throws javax.servlet.ServletException, java.io.IOException {

    // This is the message store resource.
    Resource baseResource = request.getResource();
    Session session = request.getResourceResolver().adaptTo(Session.class);
    
    // Current user.
    String user = request.getRemoteUser();

    // Anonymous users are not allowed to post anything.
    if (user == null || UserConstants.ANON_USERID.equals(request.getRemoteUser())) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
          "Anonymous users can't send messages.");
      return;
    }

    // This is the only check we always do, because to much code handling depends on it.
    if (request.getRequestParameter(MessageConstants.PROP_SAKAI_TYPE) == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "No type for this message specified.");
      return;
    }

    // Get the sakai:type and depending on this type we call a preprocessor.
    // If no preprocessor is found we use the internal one.
    String type = request.getRequestParameter(MessageConstants.PROP_SAKAI_TYPE)
        .getString();
    CreateMessagePreProcessor preProcessor = null;
    preProcessor = processors.get(type);
    if (preProcessor == null) {
      preProcessor = new InternalCreateMessagePreProcessor();
    }

    LOGGER.debug("Using preprocessor: {}", preProcessor.getType());

    // Check if the request is properly formed for this sakai:type.
    try {
      preProcessor.checkRequest(request);
    } catch (MessagingException ex) {
      response.sendError(ex.getCode(), ex.getMessage());
      return;
    }

    RequestParameterMap mapRequest = request.getRequestParameterMap();
    Map<String, Object> mapProperties = new HashMap<String, Object>();

    for (Entry<String, RequestParameter[]> e : mapRequest.entrySet()) {
      RequestParameter[] parameter = e.getValue();
      if (parameter.length == 1) {
        mapProperties.put(e.getKey(), parameter[0].getString());
      }
      else {
        String[] arr = new String[parameter.length];
        for (int i = 0;i<parameter.length;i++) {
          arr[i] = parameter[i].getString();
        }
        mapProperties.put(e.getKey(), arr);
      }
    }
    mapProperties.put(JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
        MessageConstants.SAKAI_MESSAGE_RT);
    mapProperties.put(MessageConstants.PROP_SAKAI_READ, true);
    mapProperties.put(MessageConstants.PROP_SAKAI_FROM, user);

    // Create the message.
    Node msg = null;
    String path = null;
    String messageId = null;
    try {
      msg = messagingService.create(session, mapProperties);
      if (msg == null) {
        throw new MessagingException("Unable to create the message.");
      }
      path = msg.getPath();
      messageId = msg.getProperty(MessageConstants.PROP_SAKAI_ID).getString();

      LOGGER.debug("Got message node as " + msg);
    } catch (MessagingException e) {
      LOGGER.warn("MessagingException: " + e.getMessage());
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    } catch (RepositoryException e) {
      LOGGER.warn("RepositoryException: " + e.getMessage());
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
      return;
    }

    baseResource.getResourceMetadata().setResolutionPath("/");
    baseResource.getResourceMetadata().setResolutionPathInfo(path);

    final String finalPath = path;
    final ResourceMetadata rm = baseResource.getResourceMetadata();

    // Wrap the request so it points to the message we just created.
    ResourceWrapper wrapper = new ResourceWrapper(request.getResource()) {
      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getPath()
       */
      @Override
      public String getPath() {
        return finalPath;
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getResourceType()
       */
      @Override
      public String getResourceType() {
        return "sling/servlet/default";
      }

      /**
       * {@inheritDoc}
       * 
       * @see org.apache.sling.api.resource.ResourceWrapper#getResourceMetadata()
       */
      @Override
      public ResourceMetadata getResourceMetadata() {
        return rm;
      }

    };

    RequestDispatcherOptions options = new RequestDispatcherOptions();
    SlingHttpServletResponseWrapper wrappedResponse = new SlingHttpServletResponseWrapper(
        response) {
      ServletOutputStream servletOutputStream = new ServletOutputStream() {

        @Override
        public void write(int b) throws IOException {
        }
      };
      PrintWriter pw = new PrintWriter(servletOutputStream);

      /**
       * {@inheritDoc}
       * 
       * @see javax.servlet.ServletResponseWrapper#flushBuffer()
       */
      @Override
      public void flushBuffer() throws IOException {
      }

      /**
       * {@inheritDoc}
       * 
       * @see javax.servlet.ServletResponseWrapper#getOutputStream()
       */
      @Override
      public ServletOutputStream getOutputStream() throws IOException {
        return servletOutputStream;
      }

      /**
       * {@inheritDoc}
       * 
       * @see javax.servlet.ServletResponseWrapper#getWriter()
       */
      @Override
      public PrintWriter getWriter() throws IOException {
        return pw;
      }
    };
    options.setReplaceSelectors("");
    LOGGER.debug("Sending the request out again.");
    request.getRequestDispatcher(wrapper, options).forward(request, wrappedResponse);
    response.reset();
    try {
      Node messageNode = (Node) session.getItem(path);

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      JSONWriter write = new JSONWriter(response.getWriter());
      write.object();
      write.key("id");
      write.value(messageId);
      write.key("message");
      ExtendedJSONWriter.writeNodeToWriter(write, messageNode);
      write.endObject();
    } catch (JSONException e) {
      throw new ServletException(e.getMessage(), e);
    } catch (RepositoryException e) {
      throw new ServletException(e.getMessage(), e);
    }
  }

  protected void bindCreateMessagePreProcessor(CreateMessagePreProcessor preProcessor) {
    processors.put(preProcessor.getType(), preProcessor);
  }

  protected void unbindCreateMessagePreProcessor(CreateMessagePreProcessor preProcessor) {
    processors.remove(preProcessor.getType());
  }
}