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
package org.sakaiproject.nakamura.api.docproxy;

import java.util.Map;

/**
 * Gets a light weight object representing the external resource. With this object there
 * is no requirement retrieve the body, on the properties and metadata.
 */
public interface ExternalDocumentResultMetadata {

  /**
   * @return get the type of this object identifying the ExternalRepositoryProvider type
   *         that created the object.
   */
  public String getType();

  /**
   * @return a URI identifying the remote object.
   */
  public String getUri();

  /**
   * @return the content type that should be used to send the object back to the client.
   */
  public String getContentType();

  /**
   * @return the number of bytes in the body of the content.
   */
  public long getContentLength();

  /**
   * @return a map of properties in name value pair for. The map may be hierarchical by
   *         embedding maps within maps.
   * @throws {@link DocProxyException}
   */
  public Map<String, Object> getProperties() throws DocProxyException;

}
