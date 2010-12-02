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
package org.sakaiproject.nakamura.message.resource;

import org.sakaiproject.nakamura.api.resource.SubPathProducer;
import org.sakaiproject.nakamura.util.PathUtils;

/**
 * A Path producer for messages.
 */
public class MessageSubPathProducer implements SubPathProducer {

  private String id;

  public MessageSubPathProducer(String id) {
    this.id = id;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.resource.SubPathProducer#getSubPath()
   */
  public String getSubPath() {
    // Our id is something like 7eb256fd000d8fb33668138998251f605696b112
    // This will return: /09/93/4d/50/7eb256fd000d8fb33668138998251f605696b112
    return PathUtils.normalizePath(PathUtils.toSimpleShardPath("", id, ""));
  }

}
