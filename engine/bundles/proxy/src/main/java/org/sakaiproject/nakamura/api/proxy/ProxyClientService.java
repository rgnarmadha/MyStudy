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
package org.sakaiproject.nakamura.api.proxy;

import org.apache.commons.httpclient.HttpConnectionManager;

import java.io.InputStream;
import java.util.Map;

import javax.jcr.Node;

/**
 * A Proxy Client service provides processing for Proxy request, specified by a resource
 * in the JCR. On invoking the executeCall method the supplied resource is inspected and
 * used to generate a request which is then dispatched to the requested endpoint.
 */
public interface ProxyClientService {

  /**
   * Specification property: The URL template for the end point.
   */
  public static final String SAKAI_REQUEST_PROXY_ENDPOINT = "sakai:request-proxy-endpoint";

  /**
   * Specification property: The method to use at this end point
   */
  public static final String SAKAI_REQUEST_PROXY_METHOD = "sakai:request-proxy-method";

  /**
   * Specification property: The content type of the request body.
   */
  public static final String SAKAI_REQUEST_CONTENT_TYPE = "sakai:request-content-type";
  
  /**
   * Specification property: The tempalte for the request body, if required.
   */
  public static final String SAKAI_PROXY_REQUEST_TEMPLATE = "sakai:proxy-request-template";

  /**
   * A multi value property containing a list of headers to add to the request.
   */
  public static final String SAKAI_PROXY_HEADER = "sakai:proxy-header";

  /**
   * The maximum number of bytes that this request will accept.
   */
  public static final String SAKAI_LIMIT_GET_SIZE = "sakai:proxy-limit-length";


  /**
   * Executes a HTTP call using a path in the JCR to point to a template and a map of
   * properties to populate that template with. An example might be a SOAP call.
   * 
   * <pre>
   * {http://www.w3.org/2001/12/soap-envelope}Envelope:{
   *  {http://www.w3.org/2001/12/soap-envelope}Body:{
   *   {http://www.example.org/stock}GetStockPriceResponse:{
   *    &gt;body:[       ]
   *    {http://www.example.org/stock}Price:{
   *     &gt;body:[34.5]
   *    }
   *   }
   *   &gt;body:[  ]
   *  }
   *  &gt;body:[   ]
   *  {http://www.w3.org/2001/12/soap-envelope}encodingStyle:[http://www.w3.org/2001/12/soap-encoding]
   * }
   * 
   * </pre>
   * 
   * @param node
   *          the node containing the proxy end point specification.
   * @param headers
   *          a map of headers to set int the request.
   * @param input
   *          a map of parameters for all templates (both url and body)
   * @param requestInputStream
   *          containing the request body (can be null if the call requires no body or the
   *          template will be used to generate the body)
   * @param requestContentLength
   *          if the requestImputStream is specified, the length specifies the lenght of
   *          the body.
   * @param requerstContentType
   *          the content type of the request, if null the node property
   *          sakai:proxy-request-content-type will be used.
   * @throws ProxyClientException
   */
  public ProxyResponse executeCall(Node node, Map<String, String> headers,
      Map<String, Object> input, InputStream requestInputStream,
      long requestContentLength, String requestContentType) throws ProxyClientException;

  /**
   * Exports the HTTP Connection Manager for use by bundles making HTTP requests
   */
  public HttpConnectionManager getHttpConnectionManager();
    
}
