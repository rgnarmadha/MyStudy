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
package org.sakaiproject.nakamura.site.servlet;

import org.apache.sling.api.request.RequestParameter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class DummyRequestParameter implements RequestParameter {

  private String string;

  public DummyRequestParameter(String string) {
    this.string = string;
  }

  public byte[] get() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getContentType() {
    // TODO Auto-generated method stub
    return null;
  }

  public String getFileName() {
    // TODO Auto-generated method stub
    return null;
  }

  public InputStream getInputStream() throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  public long getSize() {
    // TODO Auto-generated method stub
    return 0;
  }

  public String getString() {
    return string;
  }

  public String getString(String encoding) throws UnsupportedEncodingException {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isFormField() {
    // TODO Auto-generated method stub
    return false;
  }

}
