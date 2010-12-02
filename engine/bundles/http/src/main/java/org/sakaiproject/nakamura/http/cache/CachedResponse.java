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
import java.io.Serializable;

import javax.servlet.http.HttpServletResponse;

/**
  A pojo to contain the response redo log and content.
 */
public class CachedResponse implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -533080549451000116L;
  private long expires;
  private Operation[] operations;
  private byte[] byteContent;
  private String stringContent;

  public CachedResponse(OperationResponseCapture responseOperation, int cacheAge) throws IOException {
    expires = System.currentTimeMillis() + cacheAge*1000L;
    responseOperation.setDateHeader("X-Nakamura-Cache", System.currentTimeMillis());
    operations = responseOperation.getRedoLog();
    byteContent = responseOperation.getByteContent();
    stringContent = responseOperation.getStringContent();
  }

  public boolean isValid() {
    return expires > System.currentTimeMillis();
  }

  public void replay(HttpServletResponse response) throws IOException {
    OperationResponseReplay responseOperation = new OperationResponseReplay(operations, byteContent, stringContent);
    responseOperation.replay(response);
  }
  
  @Override
  public String toString() {
    return "redo "+operations.length+" operations "+String.valueOf(stringContent==null?byteContent.length:stringContent.length());
  }

}
