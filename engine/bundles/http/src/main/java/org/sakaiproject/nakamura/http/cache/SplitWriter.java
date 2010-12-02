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
import java.io.StringWriter;
import java.io.Writer;

public class SplitWriter extends Writer {

  
  private PrintWriter baseWriter;
  private StringWriter store;

  public SplitWriter(PrintWriter baseWriter) {
    this.baseWriter = baseWriter;
    this.store = new StringWriter();
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    baseWriter.write(cbuf, off, len);
    store.write(cbuf,off,len);
  }

  @Override
  public void flush() throws IOException {
    baseWriter.flush();
    store.flush();
  }

  @Override
  public void close() throws IOException {
    baseWriter.close();
  }

  public String getStringContent() { 
    return store.toString();
  }

  
  

}
