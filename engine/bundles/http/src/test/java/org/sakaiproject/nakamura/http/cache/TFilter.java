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

import java.io.IOException;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class TFilter implements FilterChain {

  private boolean useOutputStream;
  
  public TFilter(boolean userOutputStream) {
    this.useOutputStream = userOutputStream;
  }

  @SuppressWarnings("deprecation")
  public void doFilter(ServletRequest request, ServletResponse response)
      throws IOException, ServletException {
    HttpServletResponse sresponse = (HttpServletResponse) response;
    sresponse.addDateHeader("Date", System.currentTimeMillis());
    sresponse.setDateHeader("Last-Modified", System.currentTimeMillis());
    sresponse.setCharacterEncoding("URF-8");
    sresponse.setContentLength(10);
    sresponse.setContentType("test/plain");
    sresponse.setHeader("Cache-Control", "max-age=3600");
    sresponse.setIntHeader("Age", 1000);
    sresponse.setLocale(new Locale("en","GB"));
    sresponse.setStatus(200);
    sresponse.setStatus(200, "Ok");
    sresponse.addHeader("Cache-Control", " public");
    sresponse.addIntHeader("Age", 101);
    if ( useOutputStream ) {
      sresponse.getOutputStream().write(new byte[1024]);
    } else {
      sresponse.getWriter().write("ABCDEF");
          
    }
  }

}
