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

package org.sakaiproject.nakamura.http.cache;

import org.apache.sling.api.servlets.HttpConstants;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Filters and captures a response operation.
 */
public class FilterResponseWrapper extends HttpServletResponseWrapper {

  private boolean withCookies;
  private boolean withLastModified;
  private boolean captureResponse;
  private OperationResponseCapture capture = new OperationResponseCapture();

  public FilterResponseWrapper(HttpServletResponse wrappedResponse, boolean withLastModfied, boolean withCookies, boolean captureResponse) {
    super(wrappedResponse);
    this.withCookies = withCookies;
    this.withLastModified = withLastModfied;
    if ( captureResponse ) {
      capture = new OperationResponseCapture();
    }
    this.captureResponse = captureResponse;
  }
  
  @Override
  public void setDateHeader(String name, long date) {
    if ( withLastModified || !HttpConstants.HEADER_LAST_MODIFIED.equals(name)) {
      super.setDateHeader(name, date);
      if ( captureResponse ) {
        capture.setDateHeader(name,date);
      }
    }
  }
  
  @Override
  public void addDateHeader(String name, long date) {
    if ( withLastModified || !HttpConstants.HEADER_LAST_MODIFIED.equals(name)) {
      super.addDateHeader(name, date);
      if ( captureResponse ) {
        capture.addDateHeader(name,date);
      }
    }
  }
  
  @Override
  public void setCharacterEncoding(String charset) {
    super.setCharacterEncoding(charset);
    if ( captureResponse ) {
      capture.setCharacterEncoding(charset);
    }
  }
  
  @Override
  public void setContentLength(int len) {
    super.setContentLength(len);
    if ( captureResponse ) {
      capture.setContentLength(len);
    }
  }

  @Override
  public void setContentType(String type) {
    super.setContentType(type);
    if ( captureResponse ) {
      capture.setContentType(type);
    }
  }
  
  @Override
  public void setLocale(Locale loc) {
    super.setLocale(loc);
    if ( captureResponse ) {
      capture.setLocale(loc);
    }
  }
  
  @Override
  public void setHeader(String name, String value) {
    super.setHeader(name, value);
    if ( captureResponse ) {
      capture.setHeader(name, value);
    }
  }
  
  @Override
  public void setIntHeader(String name, int value) {
    super.setIntHeader(name, value);
    if ( captureResponse ) {
      capture.setIntHeader(name, value);
    }
  }
  @Override
  public void setStatus(int sc) {
    super.setStatus(sc);
    if ( captureResponse ) {
      capture.setStatus(sc);
    }
  }
  
  @Override
  public void setStatus(int sc, String sm) {
    super.setStatus(sc, sm);
    if ( captureResponse ) {
      capture.setStatus(sc,sm);
    }
  }
  
  @Override
  public void addHeader(String name, String value) {
    super.addHeader(name, value);
    if ( captureResponse ) {
      capture.addHeader(name, value);
    }
  }
  
  @Override
  public void addIntHeader(String name, int value) {
    super.addIntHeader(name, value);
    if ( captureResponse ) {
      capture.addIntHeader(name, value);
    }
  }
  
  @Override
  public void reset() {
    super.reset();
    if ( captureResponse ) {
      capture.reset();
    }
  }
  
  @Override
  public void resetBuffer() {
    super.resetBuffer();
    if ( captureResponse ) {
      capture.resetBuffer();
    }
  }
  
  @Override
  public void sendError(int sc) throws IOException {
    super.sendError(sc);
    if ( captureResponse ) {
      capture.sendError(sc);
    }
  }
  
  @Override
  public void sendError(int sc, String msg) throws IOException {
    super.sendError(sc, msg);
    if ( captureResponse ) {
      capture.sendError(sc, msg);
    }
  }
  
  @Override
  public void sendRedirect(String location) throws IOException {
    super.sendRedirect(location);
    if ( captureResponse ) {
      capture.sendRedirect(location);
    }
  }
  
  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    return capture.getOutputStream(super.getOutputStream());
  }
  
  @Override
  public PrintWriter getWriter() throws IOException {
    return capture.getWriter(super.getWriter());
  }
  
  
  @Override
  public void addCookie(Cookie cookie) {
    if ( withCookies ) {
      super.addCookie(cookie);
    }
  }

  public OperationResponseCapture getResponseOperation() {
    return capture;
  }
  

}
