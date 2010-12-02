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

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.sakaiproject.nakamura.api.proxy.ProxyResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements the ProxyResponse holder by wrapping the HttpMethod request.
 */
public class ProxyResponseImpl implements ProxyResponse {

  private int result;
  private HttpMethod method;
  private Map<String, String[]> headers = new HashMap<String, String[]>();
  private String cause;

  /**
   * @param result
   * @param method
   */
  public ProxyResponseImpl(int result, HttpMethod method) {
    this.result = result;
    this.method = method;

    for (Header header : method.getResponseHeaders()) {
      String name = header.getName();
      String[] values = headers.get(name);
      if ( values == null ) {
        values = new String[] {header.getValue()};
      } else {
        String[] newValues = new String[values.length+1];
        System.arraycopy(values, 0, newValues, 0, values.length);
        newValues[values.length] = header.getValue();
        values = newValues;
      }

      boolean add = true;
      // We ignore JSESSIONID cookies coming back.
      if (name.toLowerCase().equals("set-cookie")) {
        for (String v : values) {
          if (v.contains("JSESSIONID")) {
            add = false;
            break;
          }
        }
      }
      if (add) {
        headers.put(name, values);
      }
    }
  }

  /**
   * @param scPreconditionFailed
   * @param string
   * @param method2
   */
  public ProxyResponseImpl(int result, String cause, HttpMethod method) {
    this(result,method);
    this.cause = cause;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.proxy.ProxyResponse#getResultCode()
   */
  public int getResultCode() {
    return result;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.proxy.ProxyResponse#getResponseHeaders()
   */
  public Map<String, String[]> getResponseHeaders() {
    return headers;
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.proxy.ProxyResponse#getResponseBody()
   */
  public byte[] getResponseBody() throws IOException {
    return method.getResponseBody();
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.proxy.ProxyResponse#getResponseBodyAsInputStream()
   */
  public InputStream getResponseBodyAsInputStream() throws IOException {
    return method.getResponseBodyAsStream();
  }

  /**
   * {@inheritDoc}
   * @see org.sakaiproject.nakamura.api.proxy.ProxyResponse#getResponseBodyAsString()
   */
  public String getResponseBodyAsString() throws IOException {
    return method.getResponseBodyAsString();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.proxy.ProxyResponse#close()
   */
  public void close() {
    method.releaseConnection();
    method = null;
  }

  /**
   * @return the cause
   */
  public String getCause() {
    return cause;
  }

}
