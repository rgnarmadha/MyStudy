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
package org.sakaiproject.nakamura.util;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.wrappers.SlingHttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletOutputStream;

public class ResponseWrapper extends SlingHttpServletResponseWrapper {

  ByteArrayOutputStream boas = new ByteArrayOutputStream();
  ServletOutputStream servletOutputStream = new ServletOutputStream() {
    @Override
    public void write(int b) throws IOException {
      boas.write(b);
    }
  };
  PrintWriter pw = new PrintWriter(servletOutputStream);
  private String type;
  private String charset;
  private int status = 200; // Default is 200, this is also the statuscode if none get's
  // set on the response.
  private Dictionary<String, String> headers;

  public ResponseWrapper(SlingHttpServletResponse wrappedResponse) {
    super(wrappedResponse);
    headers = new Hashtable<String, String>();
  }

  @Override
  public String getCharacterEncoding() {
    return charset;
  }

  @Override
  public String getContentType() {
    return type;
  }

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
   * @see javax.servlet.ServletResponseWrapper#isCommitted()
   */
  @Override
  public boolean isCommitted() {
    // We always return false, so we can keep on outputting.
   return false;
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

  @Override
  public void setCharacterEncoding(String charset) {
    this.charset = charset;
  }

  @Override
  public void setContentType(String type) {
    this.type = type;
    headers.put("Content-Type", type);
  }

  @Override
  public void setContentLength(int len) {
    headers.put("Content-Length", Integer.toString(len));
  }

  @Override
  public void reset() {
  }

  @Override
  public void resetBuffer() {
  }

  //
  // Status
  // 

  @Override
  public void setStatus(int sc) {
    this.status = sc;
  }

  @Override
  public void setStatus(int sc, String sm) {
    this.status = sc;
  }

  @Override
  public void sendError(int sc) throws IOException {
    this.status = sc;
  }

  @Override
  public void sendError(int sc, String msg) throws IOException {
    this.status = sc;
  }

  //
  // Headers
  //

  @Override
  public void setHeader(String name, String value) {
    headers.put(name, value);
  }

  @Override
  public void addHeader(String name, String value) {
    headers.put(name, value);
  }

  @Override
  public void addIntHeader(String name, int value) {
    headers.put(name, "" + value);
  }

  @Override
  public void addDateHeader(String name, long date) {
    headers.put(name, "" + date);
  }

  @Override
  public void setDateHeader(String name, long date) {
    headers.put(name, "" + date);
  }

  /**
   * @return The headers returned by the underlying response.
   */
  public Dictionary<String, String> getResponseHeaders() {
    return headers;
  }

  /**
   * @return The data written to the underlying response stream. This stream is encoded as
   *         UTF-8.
   * @throws UnsupportedEncodingException
   *           Failed to encode.
   */
  public String getDataAsString() throws UnsupportedEncodingException {
    pw.close();
    return boas.toString("utf-8");
  }

  /**
   * @return The data written to the underlying response stream.
   */
  public OutputStream getDataAsOutputSream() {
    pw.flush();
    return boas;
  }

  /**
   * @return The status code returned by the underlying response
   */
  public int getResponseStatus() {
    return this.status;
  }

}
