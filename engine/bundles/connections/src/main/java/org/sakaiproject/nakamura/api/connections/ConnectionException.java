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
package org.sakaiproject.nakamura.api.connections;

/**
 * 
 */
public class ConnectionException extends Exception {

  /**
   *
   */
  private static final long serialVersionUID = -4812533559726881910L;
  private int code = 500;

  /**
   * 
   */
  public ConnectionException(int code) {
    super();
    this.code = code;
  }

  /**
   * @param message
   */
  public ConnectionException(int code, String message) {
    super(message);
    this.code = code;
  }

  /**
   * @param cause
   */
  public ConnectionException(int code, Throwable cause) {
    super(cause);
    this.code = code;
  }

  /**
   * @param message
   * @param cause
   */
  public ConnectionException(int code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }
  
  /**
   * @return the code
   */
  public int getCode() {
    return code;
  }

}
