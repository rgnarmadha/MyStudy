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

import org.apache.sling.api.SlingHttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public interface ProxyPostProcessor {

  /**
   *
   */
  public static final String SAKAI_POSTPROCESSOR = "sakai:postprocessor";

  /**
   * @param templateParams
   *          The parameters used to fill out the template. These are supplied as request
   *          parameters.
   * @param response
   *          The response that will be sent to the user.
   * @param proxyResponse
   *          The response as it came back from the remote resource.
   * @throws IOException
   */
  void process(Map<String, Object> templateParams, SlingHttpServletResponse response,
      ProxyResponse proxyResponse) throws IOException;

  /**
   * @return The name of this ProxyPostProcessor. Nodes that want this processor to be run
   *         should set the sakai:postprocessor property to this value.
   */
  String getName();

}
