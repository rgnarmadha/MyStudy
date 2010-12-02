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
package org.sakaiproject.nakamura.http.i18n;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Response wrapper to filter i18n keys into language messages.
 */
public class CapturingHttpServletResponse extends HttpServletResponseWrapper {
  private final CharArrayWriter caw;
  private final ByteArrayOutputStream baos;

  public CapturingHttpServletResponse(HttpServletResponse response) {
    super(response);
    caw = new CharArrayWriter();
    baos = new ByteArrayOutputStream();
  }

  @Override
  public ServletOutputStream getOutputStream() throws IOException {
    // calling super will throw an exception if that's the right thing to do
    super.getOutputStream();

    // we've passed the super call, return a stream
    return new ServletOutputStream() {

      @Override
      public void write(int b) throws IOException {
        baos.write(b);
      }
    };
  }

  @Override
  public PrintWriter getWriter() throws IOException{
    // calling super will throw an exception if that's the right thing to do
    super.getWriter();

    // we've passed the super call, return a print writer
    return new PrintWriter(caw);
  }

  @Override
  public String toString() {
    String retval = null;
    if (baos.size() > 0) {
      try {
        retval = baos.toString("UTF-8");
      } catch (UnsupportedEncodingException e) {
        // let retval be null
      }
    } else if (caw.size() > 0) {
      retval = caw.toString();
    }
    return retval;
  }
}