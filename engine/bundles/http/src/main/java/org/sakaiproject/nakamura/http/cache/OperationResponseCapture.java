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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletOutputStream;

public class OperationResponseCapture {

  public static final int MARKER = 0xff;
  public static final int END_OF_MARKER = 0xfe;
  public static final int ADD_INT_HEADER = 0x01;
  public static final int ADD_HEADER = 0x02;
  public static final int SET_STATUS = 0x03;
  public static final int SET_STATUS_WITH_MESSAGE = 0x04;
  public static final int SET_INT_HEADER = 0x05;
  public static final int SET_HEADER = 0x06;
  public static final int SET_LOCALE = 0x07;
  public static final int SET_CONTENT_TYPE = 0x08;
  public static final int SET_CONTENT_LENGTH = 0x09;
  public static final int SET_CHARACTER_ENCODING = 0x0A;
  public static final int ADD_DATE_HEADER = 0x0B;
  public static final int SET_DATE_HEADER = 0x0C;
  /**
   * a : delimited list of headernames in lower case that can't be cached.
   */
  private static final String DONT_CACHE = ":set-cookies:set-cookie:age:connection:www-authenticate:";
  private PrintWriter writer;
  private SplitOutputStream outputStream;
  private boolean cacheable;
  private SplitWriter splitWriter;
  private List<Operation> operations = new ArrayList<Operation>();

  public OperationResponseCapture() {
    cacheable = true;
    resetRedoLog();
  }

  public PrintWriter getWriter(PrintWriter baseWriter) {
    if (outputStream != null) {
      throw new IllegalStateException();
    }
    if (writer == null) {
      splitWriter = new SplitWriter(baseWriter);
      writer = new PrintWriter(splitWriter);
    }
    return writer;
  }

  public ServletOutputStream getOutputStream(ServletOutputStream baseStream) {
    if (writer != null) {
      throw new IllegalStateException();
    }
    if (outputStream == null) {
      outputStream = new SplitOutputStream(baseStream);
    }
    return outputStream;
  }

  public void sendRedirect(String location) {
    dropCache();
  }

  public void sendError(int sc, String msg) {
    dropCache();
  }

  public void setDateHeader(String name, long date) {
    
    if (cacheable && cacheHeader(name)) {
      operations.add(new Operation(SET_DATE_HEADER, name, date));
    }
  }

  public void addDateHeader(String name, long date) {
    if (cacheable && cacheHeader(name)) {
      operations.add(new Operation(ADD_DATE_HEADER, name, date));
    }
  }

  public void setCharacterEncoding(String charset) {
    if (cacheable) {
      operations.add(new Operation(SET_CHARACTER_ENCODING, charset));
    }
  }

  public void setContentLength(int len) {
    if (cacheable) {
      operations.add(new Operation(SET_CONTENT_LENGTH, len));
    }
  }

  public void setContentType(String type) {
    if (cacheable) {
      operations.add(new Operation(SET_CONTENT_TYPE, type));
    }
  }

  public void setLocale(Locale loc) {
    if (cacheable) {
      operations.add(new Operation(SET_LOCALE, loc.getLanguage(), loc.getCountry()));
    }
  }

  public void setHeader(String name, String value) {
    if (cacheable && cacheHeader(name)) {
      operations.add(new Operation(SET_HEADER, name, value));
    }
  }

  public void setIntHeader(String name, int value) {
    if (cacheable && cacheHeader(name)) {
      operations.add(new Operation(SET_INT_HEADER, name, value));
    }
  }

  public void setStatus(int sc) {
    if (sc != 200) {
      dropCache();
    }
    if (cacheable) {
      operations.add(new Operation(SET_STATUS, sc));
    }
  }

  public void setStatus(int sc, String sm) {
    if (sc != 200) {
      dropCache();
    }
    if (cacheable) {
      operations.add(new Operation(SET_STATUS_WITH_MESSAGE, sc, sm));
    }
  }

  public void addHeader(String name, String value) {
    if (cacheable && cacheHeader(name)) {
      operations.add(new Operation(ADD_HEADER, name, value));
    }
  }

  public void addIntHeader(String name, int value) {
    if (cacheable && cacheHeader(name)) {
      operations.add(new Operation(ADD_INT_HEADER, name, value));
    }
  }

  private boolean cacheHeader(String name) {
    String lc = name.toLowerCase();
    return (DONT_CACHE.indexOf(lc) == -1);
  }

  public void reset() {
    resetRedoLog();
  }

  public void resetBuffer() {
    dropCache();
  }

  public void sendError(int sc) {
    dropCache();
  }

  private void dropCache() {
    cacheable = false;
    resetRedoLog();
  }

  private void resetRedoLog() {
    operations.clear();
  }

  public Operation[] getRedoLog() throws IOException {
    return operations.toArray(new Operation[operations.size()]);
  }

  public byte[] getByteContent() throws IOException {
    if (outputStream != null) {
      outputStream.flush();
      return outputStream.toByteArray();
    }
    return null;
  }

  public String getStringContent() {
    if (writer != null) {
      writer.flush();
      return splitWriter.getStringContent();
    }
    return null;
  }

  public boolean canCache() {
    return cacheable;
  }

}
