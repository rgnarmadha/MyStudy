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

import org.apache.commons.codec.binary.Base64;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.OptingServlet;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.sakaiproject.nakamura.api.doc.BindingType;
import org.sakaiproject.nakamura.api.doc.ServiceBinding;
import org.sakaiproject.nakamura.api.doc.ServiceDocumentation;
import org.sakaiproject.nakamura.api.doc.ServiceMethod;
import org.sakaiproject.nakamura.api.doc.ServiceResponse;
import org.sakaiproject.nakamura.api.proxy.ProxyClientException;
import org.sakaiproject.nakamura.api.proxy.ProxyClientService;
import org.sakaiproject.nakamura.api.proxy.ProxyPostProcessor;
import org.sakaiproject.nakamura.api.proxy.ProxyPreProcessor;
import org.sakaiproject.nakamura.api.proxy.ProxyResponse;
import org.sakaiproject.nakamura.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

/**
 * This servlet binds to a resource that defines an end point.
 *
 */
@Service(value = Servlet.class)
@SlingServlet(resourceTypes = { "sakai/proxy" }, methods = { "GET", "POST", "PUT",
    "HEAD", "OPTIONS" },generateComponent=true, generateService=true)
@ServiceDocumentation(name = "ResourceProxyServlet", shortDescription = "This servlet binds to a resource that defines an end point.", description = "This servlet binds to a resource that defines an end point.", bindings = { @ServiceBinding(type = BindingType.TYPE, bindings = "sakai/proxy") }, methods = {
		@ServiceMethod(name = "GET", description = "Proxied GET request", response = {
				@ServiceResponse(code = 403, description = "Proxying templates may only be stored in /var/proxy"),
				@ServiceResponse(code = 500, description = "ProxyClientException or RepositoryException") }),
		@ServiceMethod(name = "POST", description = "Proxied POST request", response = {
				@ServiceResponse(code = 403, description = "Proxying templates may only be stored in /var/proxy"),
				@ServiceResponse(code = 500, description = "ProxyClientException or RepositoryException") }),
		@ServiceMethod(name = "PUT", description = "Proxied PUT request", response = {
				@ServiceResponse(code = 403, description = "Proxying templates may only be stored in /var/proxy"),
				@ServiceResponse(code = 500, description = "ProxyClientException or RepositoryException") }),
		@ServiceMethod(name = "HEAD", description = "Proxied HEAD request", response = {
				@ServiceResponse(code = 403, description = "Proxying templates may only be stored in /var/proxy"),
				@ServiceResponse(code = 500, description = "ProxyClientException or RepositoryException") }),
		@ServiceMethod(name = "OPTIONS", description = "Proxied OPTIONS request", response = {
				@ServiceResponse(code = 403, description = "Proxying templates may only be stored in /var/proxy"),
				@ServiceResponse(code = 500, description = "ProxyClientException or RepositoryException") }) })
public class ResourceProxyServlet extends SlingAllMethodsServlet implements OptingServlet {

  public static final String PROXY_PATH_PREFIX = "/var/proxy/";

  /**
   *
   */
  private static final String SAKAI_REQUEST_STREAM_BODY = "sakai:request-stream-body";

  /**
   *
   */
  private static final String AUTHORIZATION = "Authorization";

  /**
   *
   */
  private static final String BASIC = "Basic ";

  /**
   *
   */
  private static final String BASIC_PASSWORD = ":basic-password";

  /**
   *
   */
  private static final String BASIC_USER = ":basic-user";

  /**
   *
   */
  private static final long serialVersionUID = -3190208378955330531L;

  private static final Logger LOGGER = LoggerFactory
      .getLogger(ResourceProxyServlet.class);

  @Reference
  transient ProxyClientService proxyClientService;

  private transient ProxyPostProcessor defaultPostProcessor = new DefaultProxyPostProcessorImpl();

  @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = ProxyPreProcessor.class, bind = "bindPreProcessor", unbind = "unbindPreProcessor")
  Map<String, ProxyPreProcessor> preProcessors = new ConcurrentHashMap<String, ProxyPreProcessor>();

  @Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, referenceInterface = ProxyPostProcessor.class, bind = "bindPostProcessor", unbind = "unbindPostProcessor")
  Map<String, ProxyPostProcessor> postProcessors = new ConcurrentHashMap<String, ProxyPostProcessor>();

  private Set<String> headerBacklist = new HashSet<String>();

  /**
   *
   */
  public ResourceProxyServlet() {
   headerBacklist.add("Host");
   headerBacklist.add("Content-Length");
   headerBacklist.add("Content-Type");
   headerBacklist.add("Authorization");
  }
  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doDelete(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doDelete(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    dispatch(request, response, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    dispatch(request, response, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doHead(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doHead(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    dispatch(request, response, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doOptions(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doOptions(SlingHttpServletRequest request,
      SlingHttpServletResponse response) throws ServletException, IOException {
    dispatch(request, response, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    String proxyRequest = request.getHeader("Sakai-Proxy-Request-Body");
    boolean proxyStream = false;
    if ("1".equals(proxyRequest)) {
      proxyStream = true;
    }
    dispatch(request, response, proxyStream);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPut(org.apache.sling.api.SlingHttpServletRequest,
   *      org.apache.sling.api.SlingHttpServletResponse)
   */
  @Override
  protected void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response)
      throws ServletException, IOException {
    String proxyRequest = request.getHeader("Sakai-Proxy-Request-Body");
    boolean proxyStream = true;
    if ("0".equals(proxyRequest)) {
      proxyStream = false;
    }
    dispatch(request, response, proxyStream);
  }

  protected void dispatch(SlingHttpServletRequest request,
      SlingHttpServletResponse response, boolean userInputStream)
      throws ServletException, IOException {
    try {

      Resource resource = request.getResource();
      if ( !resource.getPath().startsWith(PROXY_PATH_PREFIX) ) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Proxying templates may only be stored in " + PROXY_PATH_PREFIX);
        return;
      }
      Node node = resource.adaptTo(Node.class);
      if ( !userInputStream ) {
        Value[] v = JcrUtils.getValues(node, SAKAI_REQUEST_STREAM_BODY);
        if ( v != null && v.length > 0 ) {
          userInputStream = Boolean.parseBoolean(v[0].getString());
        }
      }
      Map<String, String> headers = new ConcurrentHashMap<String, String>();
      for (Enumeration<?> enames = request.getHeaderNames(); enames.hasMoreElements();) {

        String name = (String) enames.nextElement();
        if ( !headerBacklist.contains(name) ) {
          headers.put(name, request.getHeader(name));
        }
      }
      // search for special headers.
      if (headers.containsKey(BASIC_USER)) {
        String user = headers.get(BASIC_USER);
        String password = headers.get(BASIC_PASSWORD);
        Base64 base64 = new Base64();
        String passwordDigest = new String(base64.encode((user + ":" + password).getBytes("UTF-8")));
        String digest = BASIC+passwordDigest.trim();
        headers.put(AUTHORIZATION, digest);
      }

      for (Entry<String, String> e : headers.entrySet()) {
        if (e.getKey().startsWith(":")) {
          headers.remove(e.getKey());
        }
      }

      // collect the parameters and store into a mutable map.
      RequestParameterMap parameterMap = request.getRequestParameterMap();
      Map<String, Object> templateParams = new ConcurrentHashMap<String, Object>(parameterMap);

      // search for special parameters.
      if (parameterMap.containsKey(BASIC_USER)) {
        String user = parameterMap.getValue(BASIC_USER).getString();
        String password = parameterMap.getValue(BASIC_PASSWORD).getString();
        Base64 base64 = new Base64();
        String passwordDigest = new String(base64.encode((user + ":" + password).getBytes("UTF-8")));
        String digest = BASIC+passwordDigest.trim();
        headers.put(AUTHORIZATION, digest);
      }

      // we might want to pre-process the headers
      if (node.hasProperty(ProxyPreProcessor.SAKAI_PREPROCESSOR)) {
        String preprocessorName = node
            .getProperty(ProxyPreProcessor.SAKAI_PREPROCESSOR).getString();
        ProxyPreProcessor preprocessor = preProcessors.get(preprocessorName);
        if (preprocessor != null) {
          preprocessor.preProcessRequest(request, headers, templateParams);
        } else {
          LOGGER.warn("Unable to find pre processor of name {} for node {} ",
              preprocessorName, node.getPath());
        }
      }
      ProxyPostProcessor postProcessor = defaultPostProcessor;
      // we might want to post-process the headers
      if (node.hasProperty(ProxyPostProcessor.SAKAI_POSTPROCESSOR)) {
        String postProcessorName = node.getProperty(
            ProxyPostProcessor.SAKAI_POSTPROCESSOR).getString();
        if (postProcessors.containsKey(postProcessorName))
          postProcessor = postProcessors.get(postProcessorName);
        if (postProcessor == null) {
          LOGGER.warn("Unable to find post processor of name {} for node {} ",
              postProcessorName, node.getPath());
          postProcessor = defaultPostProcessor;
        }
      }

      ProxyResponse proxyResponse = proxyClientService.executeCall(node, headers,
          templateParams, null, -1, null);
      try {
        postProcessor.process(templateParams, response, proxyResponse);
      } finally {
        proxyResponse.close();
      }
    } catch (IOException e) {
      throw e;
    } catch (ProxyClientException e) {
      response.sendError(500, e.getMessage());
    } catch (RepositoryException e) {
      response.sendError(500, e.getMessage());
    }
  }

  protected void bindPreProcessor(ProxyPreProcessor proxyPreProcessor) {
    preProcessors.put(proxyPreProcessor.getName(), proxyPreProcessor);
  }

  protected void unbindPreProcessor(ProxyPreProcessor proxyPreProcessor) {
    preProcessors.remove(proxyPreProcessor.getName());
  }

  protected void bindPostProcessor(ProxyPostProcessor proxyPostProcessor) {
    postProcessors.put(proxyPostProcessor.getName(), proxyPostProcessor);
  }

  protected void unbindPostProcessor(ProxyPostProcessor proxyPostProcessor) {
    postProcessors.remove(proxyPostProcessor.getName());
  }

  // ---------- OptingServlet interface ----------
  /**
   * {@inheritDoc}
   *
   * @see org.apache.sling.api.servlets.OptingServlet#accepts(org.apache.sling.api.SlingHttpServletRequest)
   */
  public boolean accepts(SlingHttpServletRequest request) {
    if ("delete".equals(request.getParameter(":operation"))) {
      log("Opting out of service due to existence of parameter [:operation=delete]");
      return false;
    } else {
      return true;
    }
  }

}
