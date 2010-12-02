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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

public class SplitOutputStream extends ServletOutputStream {

  
  private ServletOutputStream baseStream;
  private ByteArrayOutputStream store;

  public SplitOutputStream(ServletOutputStream baseStream) {
    store = new ByteArrayOutputStream();
    this.baseStream = baseStream;
  }

  @Override
  public void write(int b) throws IOException {
    baseStream.write(b);
    store.write(b);
  }
  
  @Override
  public void flush() throws IOException {
    super.flush();
    baseStream.flush();
    store.flush();
  }
  
  @Override
  public void close() throws IOException {
    super.close();
    baseStream.flush();
  }

  public byte[] toByteArray() {
    return store.toByteArray();
  }

  
  

}
