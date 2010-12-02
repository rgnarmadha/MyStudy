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
package org.sakaiproject.nakamura.api.message;

import org.sakaiproject.nakamura.util.StringUtils;

/**
 *
 */
public abstract class AbstractMessageRoute implements MessageRoute {

  /**
   * 
   */
  private static final String INTERNAL = "internal";
  private String transport;
  private String rcpt;

  /**
   * @param r
   */
  public AbstractMessageRoute(String r) {
    String[] routing = StringUtils.split(r, ':', 2);
    if (routing == null || routing.length == 0) {
      transport = null;
      rcpt = null;
    } else if (routing.length == 1) {
      transport = INTERNAL;
      rcpt = routing[0];
    } else {
      transport = routing[0];
      rcpt = routing[1];
    }
  }

  /**
   * @return the transport
   */
  public String getTransport() {
    return transport;
  }

  /**
   * @return the rcpt
   */
  public String getRcpt() {
    return rcpt;
  }
}
