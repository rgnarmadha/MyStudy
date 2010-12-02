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
package org.sakaiproject.nakamura.remotefiles.docproxy;


import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;
import org.sakaiproject.nakamura.remotefiles.RemoteFilesRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 *
 */
public class RemoteFilesDocumentResult implements ExternalDocumentResult {

  private long contentLength;
  private String contentType;
  private Map<String, Object> properties;
  private String uri;
  private RemoteFilesRepository remoteFilesRepository;

  @SuppressWarnings("unchecked")
  public RemoteFilesDocumentResult(Map<String, Object> document, RemoteFilesRepository remoteFilesRepository) {
    if (document != null) {
      if (document.containsKey("contentLength")) {
        this.contentLength = (Long) document.get("contentLength");
      }
      if (document.containsKey("contentType")) {
        this.contentType = (String) document.get("contentType");
      }
      if (document.containsKey("properties")) {
        this.properties = (Map<String, Object>) document.get("properties");
        if (this.properties.containsKey("thumbnailUri")) {
          this.properties.put("thumbnailUri",
              "/remotefiles" + (String) this.properties.get("thumbnailUri"));
        }
      }
      this.uri = (String) document.get("uri");
    } else {
      throw new IllegalArgumentException("Document must not be null.");
    }
    this.remoteFilesRepository = remoteFilesRepository;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult#getDocumentInputStream(long)
   */
  public InputStream getDocumentInputStream(long startingAt, String userId)
      throws DocProxyException {
    try {
      InputStream rv = new ByteArrayInputStream(remoteFilesRepository.getFileContent(uri, userId));
      {
        rv.skip(startingAt);
        return rv;
      }
    } catch (IOException e) {
      throw new DocProxyException(500,
          "Could not start reading external repository document at the requested byte position: "
              + startingAt);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata#getContentLength()
   */
  public long getContentLength() {
    return contentLength;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata#getContentType()
   */
  public String getContentType() {
    return contentType;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata#getProperties()
   */
  public Map<String, Object> getProperties() throws DocProxyException {
    return properties;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata#getType()
   */
  public String getType() {
    return RemoteFilesRepositoryProcessor.TYPE;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata#getUri()
   */
  public String getUri() {
    return "/remotefiles" + uri;
  }

}
