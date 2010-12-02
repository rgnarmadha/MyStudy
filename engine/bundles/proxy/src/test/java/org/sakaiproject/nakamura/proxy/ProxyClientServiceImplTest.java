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

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.jackrabbit.JcrConstants;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sakaiproject.nakamura.api.proxy.ProxyClientException;
import org.sakaiproject.nakamura.api.proxy.ProxyClientService;
import org.sakaiproject.nakamura.api.proxy.ProxyPostProcessor;
import org.sakaiproject.nakamura.api.proxy.ProxyResponse;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;
import org.sakaiproject.nakamura.testutils.http.CapturedRequest;
import org.sakaiproject.nakamura.testutils.http.DummyServer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 *
 */
public class ProxyClientServiceImplTest extends AbstractEasyMockTest {

  /**
   * 
   */
  private static final String APPLICATION_SOAP_XML_CHARSET_UTF_8 = "application/soap+xml; charset=utf-8";
  private static final String REQUEST_TEMPLATE = "<?xml version=\"1.0\"?>\n"
      + "<soap:Envelope xmlns:soap=\"http://www.w3.org/2001/12/soap-envelope\" "
      + "soap:encodingStyle=\"http://www.w3.org/2001/12/soap-encoding\">"
      + "<soap:Body xmlns:m=\"http://www.example.org/stock\">" + "  <m:GetStockPrice>"
      + "    <m:StockName>$stockName</m:StockName>" + "  </m:GetStockPrice>"
      + "</soap:Body>" + "</soap:Envelope>";

  private static final String STOCK_NAME = "IBM";
  private static final String CHECK_REQUEST = "<m:StockName>" + STOCK_NAME
      + "</m:StockName>";
  private static final String RESPONSE_BODY = "<?xml version=\"1.0\"?>\n"
      + "<soap:Envelope xmlns:soap=\"http://www.w3.org/2001/12/soap-envelope\" "
      + "soap:encodingStyle=\"http://www.w3.org/2001/12/soap-encoding\"> "
      + " <soap:Body xmlns:m=\"http://www.example.org/stock\">"
      + "  <m:GetStockPriceResponse> " + "    <m:Price>34.5</m:Price>"
      + "  </m:GetStockPriceResponse>" + "</soap:Body>" + " </soap:Envelope>";
  private static DummyServer dummyServer;
  private ProxyClientServiceImpl proxyClientServiceImpl;

  @BeforeClass
  public static void beforeClass() {
    dummyServer = new DummyServer();
  }

  @AfterClass
  public static void afterClass() {
    dummyServer.close();
  }

  @Before
  public void before() throws Exception {

    proxyClientServiceImpl = new ProxyClientServiceImpl();
    proxyClientServiceImpl.activate(null);
  }

  @After
  public void after() throws Exception {
    proxyClientServiceImpl.deactivate(null);
  }

  @Test
  public void testInvokeServiceMissingNode() throws ProxyClientException,
      RepositoryException {

    replay();
    Map<String, Object> input = new HashMap<String, Object>();
    Map<String, String> headers = new HashMap<String, String>();
    try {
      ProxyResponse response = proxyClientServiceImpl.executeCall(null, headers, input,
          null, 0, null);
      try {
        response.close();
      } catch (Throwable t) {

      }
      fail();
    } catch (ProxyClientException ex) {

    }
    verify();
  }

  @Test
  public void testInvokeServiceNodeNoEndPoint() throws ProxyClientException,
      RepositoryException {
    Node node = createMock(Node.class);

    expect(node.getPath()).andReturn("/testing").anyTimes();
    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        false);

    replay();
    Map<String, Object> input = new HashMap<String, Object>();
    Map<String, String> headers = new HashMap<String, String>();
    try {
      ProxyResponse response = proxyClientServiceImpl.executeCall(node, headers, input,
          null, 0, null);
      try {
        response.close();
      } catch (Throwable t) {

      }
      fail();
    } catch (ProxyClientException ex) {
    }
    verify();
  }

  @Test
  public void testInvokeServiceNodeBadEndPoint() throws Exception {
    checkBadUrl("http://${url}",
        "Invalid Endpoint template, relies on request to resolve valid URL http://${url}");
    checkBadUrl("h${url}", "Invalid Endpoint template, relies on request to resolve valid URL");
    checkBadUrl("${url}",  "Invalid Endpoint template, relies on request to resolve valid URL");
  }

  private void checkBadUrl(String badUrl, String message) throws Exception {
    super.setUp();
    Node node = createMock(Node.class);
    Property endpointProperty = createMock(Property.class);
    PropertyDefinition propertyDefinition = createMock(PropertyDefinition.class);
    Value value = createMock(Value.class);

    expect(node.getPath()).andReturn("/testing").anyTimes();
    expect(node.hasProperty(ProxyPostProcessor.SAKAI_POSTPROCESSOR)).andReturn(
        false);
    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        true);
    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        endpointProperty);

    expect(endpointProperty.getDefinition()).andReturn(propertyDefinition);
    expect(propertyDefinition.isMultiple()).andReturn(false).atLeastOnce();
    expect(endpointProperty.getValue()).andReturn(value);
    expect(value.getString()).andReturn(badUrl);

    replay();
    Map<String, Object> input = new HashMap<String, Object>();
    Map<String, String> headers = new HashMap<String, String>();
    try {
      ProxyResponse response = proxyClientServiceImpl.executeCall(node, headers, input,
          null, 0, null);
      try {
        response.close();
      } catch (Throwable t) {

      }
      fail();
    } catch (ProxyClientException ex) {
      assertEquals(message, ex.getMessage());
    }
    verify();
  }

  @Test
  public void testInvokeServiceNodeEndPoint() throws ProxyClientException,
      RepositoryException, IOException {
    Node node = createMock(Node.class);

    expect(node.getPath()).andReturn("/testing").anyTimes();

    Property endpointProperty = createMock(Property.class);
    Property requestMethodProperty = createMock(Property.class);
    Property requestContentType = createMock(Property.class);
    Property templateProperty = createMock(Property.class);
    Property lastModifiedProperty = createMock(Property.class);
    PropertyDefinition propertyDefinition = createMock(PropertyDefinition.class);
    Value value = createMock(Value.class);
    Binary binary = createMock(Binary.class);

    expect(node.hasProperty(ProxyPostProcessor.SAKAI_POSTPROCESSOR)).andReturn(
        false);
    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        true);

    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        endpointProperty);

    expect(endpointProperty.getDefinition()).andReturn(propertyDefinition);
    expect(propertyDefinition.isMultiple()).andReturn(false).atLeastOnce();
    expect(endpointProperty.getValue()).andReturn(value);
    expect(value.getString()).andReturn(dummyServer.getUrl());

    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_METHOD)).andReturn(
        true);

    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_PROXY_METHOD)).andReturn(
        requestMethodProperty);
    expect(requestMethodProperty.getString()).andReturn("POST");
    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_CONTENT_TYPE)).andReturn(
        true);

    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_CONTENT_TYPE)).andReturn(
        requestContentType);
    expect(requestContentType.getString()).andReturn(APPLICATION_SOAP_XML_CHARSET_UTF_8);
    expect(node.hasProperty(ProxyClientService.SAKAI_PROXY_REQUEST_TEMPLATE)).andReturn(
        true).atLeastOnce();
    expect(node.getProperty(ProxyClientService.SAKAI_PROXY_REQUEST_TEMPLATE)).andReturn(
        templateProperty).atLeastOnce();
    expect(node.hasProperty(ProxyClientService.SAKAI_PROXY_HEADER)).andReturn(false)
        .atLeastOnce();

    expect(templateProperty.getValue()).andReturn(value);
    expect(templateProperty.getDefinition()).andReturn(propertyDefinition);
    expect(value.getBinary()).andReturn(binary);
    expect(binary.getStream()).andReturn(
        new ByteArrayInputStream(REQUEST_TEMPLATE.getBytes()));

    expect(node.hasProperty(JcrConstants.JCR_LASTMODIFIED)).andReturn(true).atLeastOnce();
    expect(node.getProperty(JcrConstants.JCR_LASTMODIFIED)).andReturn(
        lastModifiedProperty).atLeastOnce();
    GregorianCalendar now = new GregorianCalendar();
    now.setTimeInMillis(System.currentTimeMillis() - 1000);
    expect(lastModifiedProperty.getDate()).andReturn(now).atLeastOnce();

    dummyServer.setContentType(APPLICATION_SOAP_XML_CHARSET_UTF_8);
    dummyServer.setResponseBody(RESPONSE_BODY);

    replay();
    Map<String, Object> input = new HashMap<String, Object>();
    input.put("stockName", STOCK_NAME);

    Map<String, String> headers = new HashMap<String, String>();
    headers.put("SOAPAction", "");
    ProxyResponse response = proxyClientServiceImpl.executeCall(node, headers, input,
        null, 0, null);

    CapturedRequest request = dummyServer.getRequest();
    assertEquals("Method not correct ", "POST", request.getMethod());
    assertEquals("No Soap Action in request", "", request.getHeader("SOAPAction"));
    assertEquals("Incorrect Content Type in request", APPLICATION_SOAP_XML_CHARSET_UTF_8,
        request.getContentType());

    assertTrue("Template Not merged correctly ",
        request.getRequestBody().indexOf(CHECK_REQUEST) > 0);
    response.close();

    verify();
  }

  @Test
  public void testInvokeServiceNodeEndPointPut() throws ProxyClientException,
      RepositoryException, IOException {
    Node node = createMock(Node.class);

    expect(node.getPath()).andReturn("/testing").anyTimes();

    Property endpointProperty = createMock(Property.class);
    Property requestMethodProperty = createMock(Property.class);
    PropertyDefinition propertyDefinition = createMock(PropertyDefinition.class);
    Value value = createMock(Value.class);

    expect(node.hasProperty(ProxyPostProcessor.SAKAI_POSTPROCESSOR)).andReturn(
        false);
    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        true);

    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        endpointProperty);

    expect(endpointProperty.getDefinition()).andReturn(propertyDefinition);
    expect(propertyDefinition.isMultiple()).andReturn(false).atLeastOnce();
    expect(endpointProperty.getValue()).andReturn(value);
    expect(value.getString()).andReturn(dummyServer.getUrl());

    expect(node.hasProperty(ProxyClientService.SAKAI_PROXY_HEADER)).andReturn(false)
        .atLeastOnce();

    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_METHOD)).andReturn(
        true);

    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_PROXY_METHOD)).andReturn(
        requestMethodProperty);
    expect(requestMethodProperty.getString()).andReturn("PUT");

    dummyServer.setContentType(APPLICATION_SOAP_XML_CHARSET_UTF_8);
    dummyServer.setResponseBody(RESPONSE_BODY);

    replay();
    Map<String, Object> input = new HashMap<String, Object>();
    input.put("stockName", STOCK_NAME);

    Map<String, String> headers = new HashMap<String, String>();
    byte[] bas = new byte[1024];
    for (int i = 0; i < bas.length; i++) {
      bas[i] = (byte) (i & 0xff);
    }
    ByteArrayInputStream bais = new ByteArrayInputStream(bas);
    ProxyResponse response = proxyClientServiceImpl.executeCall(node, headers, input,
        bais, bas.length, "binary/x-data");

    CapturedRequest request = dummyServer.getRequest();
    assertEquals("Method not correct ", "PUT", request.getMethod());
    assertEquals("Incorrect Content Type in request", "binary/x-data",
        request.getContentType());

    assertArrayEquals("Request Not equal ", bas, request.getRequestBodyAsByteArray());
    response.close();

    verify();
  }

  @Test
  public void testInvokeServiceNodeEndPointGet() throws ProxyClientException,
      RepositoryException, IOException {
    testRequest("GET", "GET", RESPONSE_BODY, -1);
  }

  @Test
  public void testInvokeServiceNodeEndPointGetLimit() throws ProxyClientException,
      RepositoryException, IOException {
    testRequest("GET", "GET", RESPONSE_BODY, 1020000);
  }

  @Test
  public void testInvokeServiceNodeEndPointGetLimitLow() throws ProxyClientException,
      RepositoryException, IOException {
    testRequest("GET", "HEAD", null, 1);
  }

  @Test
  public void testInvokeServiceNodeEndPointOptions() throws ProxyClientException,
      RepositoryException, IOException {
    testRequest("OPTIONS", "OPTIONS", RESPONSE_BODY, -1);
  }

  @Test
  public void testInvokeServiceNodeEndPointHead() throws ProxyClientException,
      RepositoryException, IOException {
    testRequest("HEAD", "HEAD", null, -1);
  }

  @Test
  public void testInvokeServiceNodeEndPointOther() throws ProxyClientException,
      RepositoryException, IOException {
    testRequest(null, "GET", RESPONSE_BODY, -1);
  }

  private void testRequest(String type, String expectedMethod, String body, long limit)
      throws ProxyClientException, RepositoryException, IOException {
    Node node = createMock(Node.class);

    expect(node.getPath()).andReturn("/testing").anyTimes();

    Property endpointProperty = createMock(Property.class);
    Property requestMethodProperty = createMock(Property.class);
    PropertyDefinition propertyDefinition = createMock(PropertyDefinition.class);
    Value value = createMock(Value.class);

    expect(node.hasProperty(ProxyPostProcessor.SAKAI_POSTPROCESSOR)).andReturn(
        false);

    expect(node.hasProperty(ProxyClientService.SAKAI_PROXY_REQUEST_TEMPLATE)).andReturn(
        false).anyTimes();
    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        true);

    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_PROXY_ENDPOINT)).andReturn(
        endpointProperty);
    expect(node.hasProperty(ProxyClientService.SAKAI_PROXY_HEADER)).andReturn(false)
        .atLeastOnce();

    if (limit == -1) {
      expect(node.hasProperty(ProxyClientService.SAKAI_LIMIT_GET_SIZE)).andReturn(false)
          .anyTimes();
    } else {
      expect(node.hasProperty(ProxyClientService.SAKAI_LIMIT_GET_SIZE)).andReturn(true)
          .anyTimes();
      Property sizeProperty = createNiceMock(Property.class);
      expect(node.getProperty(ProxyClientService.SAKAI_LIMIT_GET_SIZE)).andReturn(
          sizeProperty).anyTimes();
      expect(sizeProperty.getLong()).andReturn(limit).anyTimes();
    }

    expect(endpointProperty.getDefinition()).andReturn(propertyDefinition);
    expect(propertyDefinition.isMultiple()).andReturn(false).atLeastOnce();
    expect(endpointProperty.getValue()).andReturn(value);
    expect(value.getString()).andReturn(dummyServer.getUrl());

    expect(node.hasProperty(ProxyClientService.SAKAI_REQUEST_PROXY_METHOD)).andReturn(
        true);

    expect(node.getProperty(ProxyClientService.SAKAI_REQUEST_PROXY_METHOD)).andReturn(
        requestMethodProperty);
    expect(requestMethodProperty.getString()).andReturn(type);

    dummyServer.setContentType(APPLICATION_SOAP_XML_CHARSET_UTF_8);
    dummyServer.setResponseBody(body);

    replay();
    Map<String, Object> input = new HashMap<String, Object>();
    input.put("stockName", STOCK_NAME);

    Map<String, String> headers = new HashMap<String, String>();
    ProxyResponse response = proxyClientServiceImpl.executeCall(node, headers, input,
        null, 0, null);

    CapturedRequest request = dummyServer.getRequest();
    assertEquals("Method not correct ", expectedMethod, request.getMethod());
    assertEquals("Incorrect Content Type in request", null, request.getContentType());

    assertEquals(type + "s dont have request bodies ", null,
        request.getRequestBodyAsByteArray());

    assertEquals(body, response.getResponseBodyAsString());
    assertEquals(APPLICATION_SOAP_XML_CHARSET_UTF_8,
        response.getResponseHeaders().get("Content-Type")[0]);

    response.close();

    verify();
  }

}
