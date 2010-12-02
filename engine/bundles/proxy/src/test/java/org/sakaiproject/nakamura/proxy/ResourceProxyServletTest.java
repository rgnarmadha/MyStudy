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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sakaiproject.nakamura.api.proxy.ProxyClientService;
import org.sakaiproject.nakamura.api.proxy.ProxyPostProcessor;
import org.sakaiproject.nakamura.api.proxy.ProxyPreProcessor;
import org.sakaiproject.nakamura.api.proxy.ProxyResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceProxyServletTest {

  private ResourceProxyServlet servlet;

  @Mock
  private SlingHttpServletRequest request;

  @Mock
  private SlingHttpServletResponse response;

  @Mock
  private Resource resource;

  @Mock
  private Node node;

  private Vector<String> headerNames;

  private Vector<String> parameterNames;

  @Mock
  private RequestParameterMap parameterMap;

  @Mock
  private ProxyClientService proxyClientService;

  @Mock
  private ProxyResponse proxyResponse;

  @Mock
  private ServletOutputStream responseOutputStream;

  @Mock
  private Property jcrProperty;

  private Map<String, ProxyPreProcessor> proxyPreProcessors;

  @Mock
  private ProxyPreProcessor proxyPreProcessor;

  private Map<String, ProxyPostProcessor> proxyPostProcessors;

  @Mock
  private ProxyPostProcessor proxyPostProcessor;

  @Before
  public void setup() {
    servlet = new ResourceProxyServlet();
    headerNames = new Vector<String>();
    parameterNames = new Vector<String>();
    proxyPreProcessors = new HashMap<String, ProxyPreProcessor>();
    proxyPreProcessors.put("rss", proxyPreProcessor);
    proxyPostProcessors = new HashMap<String, ProxyPostProcessor>();
    proxyPostProcessors.put("rss", proxyPostProcessor);
  }

  @Test
  public void rejectsRequestsNotOnTheRightPath() throws Exception {
    // given
    requestReturnsAResource();
    resourceWithFooPath();

    // when
    servlet.doGet(request, response);

    // then
    verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
  }

  @Test
  public void returnsAProxiedGet() throws Exception {
    // given
    requestReturnsAResource();
    resourceWithLegitimatePath();
    resourceReturnsANode();
    requestReturnsHeaderNames();
    requestReturnsParameterNames();
    requestReturnsParameterMap();
    proxyClientServiceReturnsAProxyResponse();
    proxyResponseHasHelloWorldInputStream();
    slingResponseHasOutputStream();
    servlet.proxyClientService = proxyClientService;

    // when
    servlet.doGet(request, response);
  }

  @Test
  public void canDoHeaderBasicAuth() throws Exception {
    // given
    requestReturnsAResource();
    resourceWithLegitimatePath();
    resourceReturnsANode();
    requestReturnsHeaderNames();
    requestReturnsParameterNames();
    requestReturnsParameterMap();
    requestHasBasicAuthHeaders();
    proxyClientServiceReturnsAProxyResponse();
    proxyResponseHasHelloWorldInputStream();
    slingResponseHasOutputStream();
    servlet.proxyClientService = proxyClientService;

    // when
    servlet.doGet(request, response);
  }

  @Test
  public void canDoParameterBasicAuth() throws Exception {
    // given
    requestReturnsAResource();
    resourceWithLegitimatePath();
    resourceReturnsANode();
    requestReturnsHeaderNames();
    requestReturnsParameterNames();
    requestReturnsParameterMap();
    requestHasBasicAuthParameters();
    proxyClientServiceReturnsAProxyResponse();
    proxyResponseHasHelloWorldInputStream();
    slingResponseHasOutputStream();
    servlet.proxyClientService = proxyClientService;

    // when
    servlet.doGet(request, response);
  }

  private void requestHasBasicAuthParameters() {
    parameterNames.add(":basic-user");
    parameterNames.add(":basic-password");
    when(request.getParameterValues(":basic-user")).thenReturn(new String[]{"zach"});
    when(request.getParameterValues(":basic-password")).thenReturn(new String[]{"secret"});
  }

  @Test
  public void canPostWithAContentBody() throws Exception {
    // given
    requestReturnsAResource();
    resourceWithLegitimatePath();
    resourceReturnsANode();
    requestHasSakaiProxyRequestBodyOneHeader();
    requestReturnsHeaderNames();
    requestReturnsParameterMap();
    proxyClientServiceReturnsAProxyResponse();
    proxyResponseHasHelloWorldInputStream();
    slingResponseHasOutputStream();
    servlet.proxyClientService = proxyClientService;

    // when
    servlet.doPost(request, response);
  }

  @Test
  public void canPutWithAContentBody() throws Exception {
    // given
    requestReturnsAResource();
    resourceWithLegitimatePath();
    resourceReturnsANode();
    requestHasSakaiProxyRequestBodyZeroHeader();
    requestReturnsHeaderNames();
    requestReturnsParameterNames();
    requestReturnsParameterMap();
    proxyClientServiceReturnsAProxyResponse();
    proxyResponseHasHelloWorldInputStream();
    slingResponseHasOutputStream();
    servlet.proxyClientService = proxyClientService;

    // when
    servlet.doPut(request, response);
  }

  @Test
  public void conveysParamsToTheProxy() throws Exception {
    // given
    requestReturnsAResource();
    resourceWithLegitimatePath();
    resourceReturnsANode();
    requestReturnsHeaderNames();
    requestReturnsParameterNames();
    requestReturnsParameterMap();
    requestHasACoupleOfQueryParameters();
    proxyClientServiceReturnsAProxyResponse();
    proxyResponseHasHelloWorldInputStream();
    slingResponseHasOutputStream();
    servlet.proxyClientService = proxyClientService;

    // when
    servlet.doGet(request, response);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void canInvokePreProcessor() throws Exception {
    // given
    requestReturnsAResource();
    resourceWithLegitimatePath();
    resourceReturnsANode();
    requestReturnsHeaderNames();
    requestReturnsParameterNames();
    requestReturnsParameterMap();
    nodeHasSakaiPreprocessorProperty();
    proxyClientServiceReturnsAProxyResponse();
    proxyResponseHasHelloWorldInputStream();
    slingResponseHasOutputStream();
    servlet.preProcessors = proxyPreProcessors;
    servlet.proxyClientService = proxyClientService;

    // when
    servlet.doGet(request, response);

    verify(proxyPreProcessor).preProcessRequest(eq(request), (Map<String,String>)any(), (Map<String,Object>)any());
  }

  @Test
  public void canInvokePostProcessor() throws Exception {
    // given
    requestReturnsAResource();
    resourceWithLegitimatePath();
    resourceReturnsANode();
    requestReturnsHeaderNames();
    requestReturnsParameterNames();
    requestReturnsParameterMap();
    nodeHasSakaiPostprocessorProperty();
    proxyClientServiceReturnsAProxyResponse();
    proxyResponseHasHelloWorldInputStream();
    slingResponseHasOutputStream();
    servlet.postProcessors = proxyPostProcessors;
    servlet.proxyClientService = proxyClientService;

    // when
    servlet.doGet(request, response);

    HashMap<String, Object> map = new HashMap<String, Object>();
    verify(proxyPostProcessor).process(map, response, proxyResponse);
  }

  private void nodeHasSakaiPostprocessorProperty() throws Exception {
    when(node.hasProperty(ProxyPostProcessor.SAKAI_POSTPROCESSOR)).thenReturn(Boolean.TRUE);
    when(node.getProperty(ProxyPostProcessor.SAKAI_POSTPROCESSOR)).thenReturn(jcrProperty);
    when(jcrProperty.getString()).thenReturn("rss");
  }

  private void nodeHasSakaiPreprocessorProperty() throws Exception {
    when(node.hasProperty(ProxyPreProcessor.SAKAI_PREPROCESSOR)).thenReturn(Boolean.TRUE);
    when(node.getProperty(ProxyPreProcessor.SAKAI_PREPROCESSOR)).thenReturn(jcrProperty);
    when(jcrProperty.getString()).thenReturn("rss");
  }

  private void requestHasACoupleOfQueryParameters() {
    parameterNames.add("q");
    parameterNames.add("max");
    when(request.getParameter("q")).thenReturn("puppies");
    when(request.getParameter("max")).thenReturn("25");
  }

  private void requestHasSakaiProxyRequestBodyOneHeader() {
    when(request.getHeader("Sakai-Proxy-Request-Body")).thenReturn("1");
  }

  private void requestHasSakaiProxyRequestBodyZeroHeader() {
    when(request.getHeader("Sakai-Proxy-Request-Body")).thenReturn("0");
  }

  private void requestHasBasicAuthHeaders() {
    headerNames.add(":basic-user");
    headerNames.add(":basic-password");
    when(request.getHeader(":basic-user")).thenReturn("zach");
    when(request.getHeader(":basic-password")).thenReturn("secret");
  }

  private void slingResponseHasOutputStream() throws Exception {
    when(response.getOutputStream()).thenReturn(responseOutputStream);
  }

  private void proxyResponseHasHelloWorldInputStream() throws Exception {
    when(proxyResponse.getResponseBodyAsInputStream()).thenReturn(
        new ByteArrayInputStream("Hello, world.".getBytes("UTF-8")));
  }

  @SuppressWarnings("unchecked")
  private void proxyClientServiceReturnsAProxyResponse() throws Exception {
    when(
        proxyClientService.executeCall((Node) any(), (Map<String, String>) any(),
            (Map<String, Object>) any(), (InputStream) any(), anyLong(), anyString()))
        .thenReturn(proxyResponse);
  }

  private void requestReturnsParameterNames() {
    when(request.getParameterNames()).thenReturn(parameterNames.elements());
  }

  private void requestReturnsHeaderNames() {
    when(request.getHeaderNames()).thenReturn(headerNames.elements());
  }

  private void resourceReturnsANode() {
    when(resource.adaptTo(Node.class)).thenReturn(node);
  }
  private void requestReturnsParameterMap() {
    when(request.getRequestParameterMap()).thenReturn(parameterMap);
  }
  private void requestReturnsAResource() {
    when(request.getResource()).thenReturn(resource);
  }

  private void resourceWithFooPath() {
    when(resource.getPath()).thenReturn("/foo/bar/_dostuff");
  }

  private void resourceWithLegitimatePath() {
    when(resource.getPath()).thenReturn(
        ResourceProxyServlet.PROXY_PATH_PREFIX + "twitter");
  }

}
