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
package org.sakaiproject.nakamura.testutils.http;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 *
 */
public class CapturedRequest {

  private Map<String, String> headers = new HashMap<String, String>();
  private String requestBody;
  private String contentType;
  private String method;
  private byte[] byteBody;

  /**
   * @param request
   * @throws IOException
   */
  public CapturedRequest(HttpServletRequest request) throws IOException {
    for (Enumeration<?> names = request.getHeaderNames(); names.hasMoreElements();) {
      String name = (String) names.nextElement();
      headers.put(name, request.getHeader(name));
    }
    contentType = request.getContentType();
    if (request.getContentLength() > 0) {
      byteBody = new byte[request.getContentLength()];
      InputStream in = request.getInputStream();
      in.read(byteBody);
      requestBody = new String(byteBody);
    }
    method = request.getMethod();
  }

  /**
   * @param string
   * @return
   */
  public String getHeader(String name) {
    return headers.get(name);
  }

  /**
   * @return
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * @return the requestBody
   */
  public String getRequestBody() {
    return requestBody;
  }

  /**
   * @return the method
   */
  public String getMethod() {
    return method;
  }

  /**
   * @return
   */
  @SuppressWarnings(justification="In Test code", value={"EI_EXPOSE_REP"})
  public byte[] getRequestBodyAsByteArray() {
    return byteBody;
  }
}
