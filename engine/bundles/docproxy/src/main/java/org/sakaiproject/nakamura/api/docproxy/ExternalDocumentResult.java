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

import java.io.InputStream;

/**
 * Get the ExternalDocumentResult. This extends {@link ExternalDocumentResultMetadata} by
 * adding a input stream from which the body of the document can be read.
 */
public interface ExternalDocumentResult extends ExternalDocumentResultMetadata {

  /**
   * @param startingAt
   *          the starting position of the input stream. The implementation should
   *          position the stream at this location. There is no requirement on the
   *          implementation to create a stream where values before this location can be
   *          retrieved.
   *
   * @param userId
   *          the id of the user requesting the external document, for authorization purposes
   *
   * @return an input stream that contains the body of the document. The caller is
   *         responsible for closing the input stream once it has been retrieved. The
   *         implementation is responsible for managing the stream if it is not retrieved.
   *         Ideally the implementation will not create the stream if its not requested.
   */
  public InputStream getDocumentInputStream(long startingAt, String userId) throws DocProxyException;
}
