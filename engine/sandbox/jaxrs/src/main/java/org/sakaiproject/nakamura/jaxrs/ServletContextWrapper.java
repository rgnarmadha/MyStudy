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
package org.sakaiproject.nakamura.jaxrs;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;


/**
 *
 */
public class ServletContextWrapper implements ServletContext {
  /**
   *
   */
  private ServletContext delegate;
  /**
   * @param delegate
   */
  ServletContextWrapper(ServletContext delegate) {
    this.delegate = delegate;
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getAttribute(java.lang.String)
   */
  public Object getAttribute(String arg0) {
    return delegate.getAttribute(arg0);
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getAttributeNames()
   */
  @SuppressWarnings("unchecked")
  public Enumeration getAttributeNames() {
    return delegate.getAttributeNames();
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getContext(java.lang.String)
   */
  public ServletContext getContext(String arg0) {
    return delegate.getContext(arg0);
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getContextPath()
   */
  public String getContextPath() {
    return delegate.getContextPath();
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getInitParameter(java.lang.String)
   */
  public String getInitParameter(String key) {
    if ("resteasy.servlet.mapping.prefix".equalsIgnoreCase(key)) {
      return ResteasyServlet.SERVLET_PATH;
    } else {
      return delegate.getInitParameter(key);
    }
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getInitParameterNames()
   */
  @SuppressWarnings("unchecked")
  public Enumeration getInitParameterNames() {
    return delegate.getInitParameterNames();
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getMajorVersion()
   */
  public int getMajorVersion() {
    return delegate.getMajorVersion();
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getMimeType(java.lang.String)
   */
  public String getMimeType(String arg0) {
    return delegate.getMimeType(arg0);
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getMinorVersion()
   */
  public int getMinorVersion() {
    return delegate.getMinorVersion();
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getNamedDispatcher(java.lang.String)
   */
  public RequestDispatcher getNamedDispatcher(String arg0) {
    return delegate.getNamedDispatcher(arg0);
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getRealPath(java.lang.String)
   */
  public String getRealPath(String arg0) {
    return delegate.getRealPath(arg0);
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getRequestDispatcher(java.lang.String)
   */
  public RequestDispatcher getRequestDispatcher(String arg0) {
    return delegate.getRequestDispatcher(arg0);
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getResource(java.lang.String)
   */
  public URL getResource(String arg0) throws MalformedURLException {
    return delegate.getResource(arg0);
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getResourceAsStream(java.lang.String)
   */
  public InputStream getResourceAsStream(String arg0) {
    return delegate.getResourceAsStream(arg0);
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getResourcePaths(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  public Set getResourcePaths(String arg0) {
    return delegate.getResourcePaths(arg0);
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getServerInfo()
   */
  public String getServerInfo() {
    return delegate.getServerInfo();
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getServlet(java.lang.String)
   */
  @Deprecated
  public Servlet getServlet(String arg0) throws ServletException {
    return delegate.getServlet(arg0);
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getServletContextName()
   */
  public String getServletContextName() {
    return delegate.getServletContextName();
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getServletNames()
   */
  @Deprecated
  @SuppressWarnings("unchecked")
  public Enumeration getServletNames() {
    return delegate.getServletNames();
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#getServlets()
   */
  @Deprecated
  @SuppressWarnings("unchecked")
  public Enumeration getServlets() {
    return delegate.getServlets();
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#log(java.lang.Exception, java.lang.String)
   */
  @Deprecated
  public void log(Exception arg0, String arg1) {
    delegate.log(arg0, arg1);
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#log(java.lang.String, java.lang.Throwable)
   */
  public void log(String arg0, Throwable arg1) {
    delegate.log(arg0, arg1);
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#log(java.lang.String)
   */
  public void log(String arg0) {
    delegate.log(arg0);
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#removeAttribute(java.lang.String)
   */
  public void removeAttribute(String arg0) {
    delegate.removeAttribute(arg0);
  }
  /**
   * {@inheritDoc}
   * @see javax.servlet.ServletContext#setAttribute(java.lang.String, java.lang.Object)
   */
  public void setAttribute(String arg0, Object arg1) {
    delegate.setAttribute(arg0, arg1);
  }
}
