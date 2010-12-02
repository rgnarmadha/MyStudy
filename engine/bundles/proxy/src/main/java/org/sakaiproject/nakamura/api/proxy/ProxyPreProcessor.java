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

import org.apache.sling.api.SlingHttpServletRequest;

import java.util.Map;

/**
 * A pre processor interface for Proxy Requests, preprocesses each request performing
 * further processing on the headers and templateParameters.
 */
public interface ProxyPreProcessor {

  /**
   * 
   */
  public static final String SAKAI_PREPROCESSOR = "sakai:preprocessor";

  /**
   * @param request
   *          The request that hits the proxy node.
   * @param headers
   *          A map of headers already collected. The map can be modified as wished. All
   *          of the entries in this map will end up als headers on the outgoing request
   *          to the remote resource.
   * @param templateParams
   *          A map of template parameters that will be passed to velocity to render the
   *          url.
   */
  void preProcessRequest(SlingHttpServletRequest request, Map<String, String> headers,
      Map<String, Object> templateParams);

  /**
   * @return The name of this ProxyPreProcessor. Nodes that want this processor to be run
   *         should set the sakai:preprocessor property to this value.
   */
  String getName();

}
