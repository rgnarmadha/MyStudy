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

package org.sakaiproject.nakamura.jpaprovider.test;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.sakaiproject.nakamura.jpaprovider.UrlEnumeration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

public class DummyBundle implements Bundle {

  @SuppressWarnings("rawtypes")
  public Enumeration findEntries(String arg0, String arg1, boolean arg2) {
    return null;
  }

  public BundleContext getBundleContext() {
    return null;
  }

  public long getBundleId() {
    return 0;
  }

  public URL getEntry(String arg0) {
    return null;
  }

  @SuppressWarnings("rawtypes")
  public Enumeration getEntryPaths(String arg0) {
    return null;
  }

  @SuppressWarnings("rawtypes")
  public Dictionary getHeaders() {
    return null;
  }

  @SuppressWarnings("rawtypes")
  public Dictionary getHeaders(String arg0) {
    return getHeaders();
  }

  public long getLastModified() {
    return 0;
  }

  public String getLocation() {
    return null;
  }

  public ServiceReference[] getRegisteredServices() {
    return null;
  }

  public URL getResource(String resourceName) {
    return getClass().getClassLoader().getResource(resourceName);
  }

  @SuppressWarnings("rawtypes")
  public Enumeration getResources(String arg0) throws IOException {
    URL url = getResource(arg0);
    if (url != null) {
      return new UrlEnumeration(url);
    }
    return null;
  }

  public ServiceReference[] getServicesInUse() {
    return null;
  }

  public int getState() {
    return 0;
  }

  public String getSymbolicName() {
    return null;
  }

  public boolean hasPermission(Object arg0) {
    return false;
  }

  @SuppressWarnings("rawtypes")
  public Class loadClass(String arg0) throws ClassNotFoundException {
    return null;
  }

  public void start() throws BundleException {
  }

  public void start(int arg0) throws BundleException {
  }

  public void stop() throws BundleException {
  }

  public void stop(int arg0) throws BundleException {
  }

  public void uninstall() throws BundleException {
  }

  public void update() throws BundleException {
  }

  public void update(InputStream arg0) throws BundleException {
  }

}
