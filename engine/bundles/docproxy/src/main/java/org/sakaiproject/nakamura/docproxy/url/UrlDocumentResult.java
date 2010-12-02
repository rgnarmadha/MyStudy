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
package org.sakaiproject.nakamura.docproxy.url;

import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class UrlDocumentResult implements ExternalDocumentResult {
  private static final Logger LOG = LoggerFactory.getLogger(UrlDocumentResult.class);

  private String uri;
  private String contentType;
  private long contentLength;
  private Map<String, Object> properties = new HashMap<String, Object>();

  public UrlDocumentResult() {
  }

  public UrlDocumentResult(String uri, String contentType, long contentLength,
      Map<String, Object> properties) {
    this.uri = uri;
    this.contentType = contentType;
    this.contentLength = contentLength;
    if (properties != null) {
      this.properties = properties;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata#getType()
   */
  public String getType() {
    return UrlRepositoryProcessor.TYPE;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata#getUri()
   */
  public String getUri() {
    return uri;
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
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata#getContentLength()
   */
  public long getContentLength() {
    return contentLength;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResultMetadata#getProperties()
   */
  public Map<String, Object> getProperties() {
    return properties;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.docproxy.ExternalDocumentResult#getDocumentInputStream(long)
   */
  public InputStream getDocumentInputStream(long startingAt) throws DocProxyException {
    try {
      URL url = new URL(uri);
      InputStream is = url.openStream();
      long actual = is.skip(startingAt);
      if (actual != startingAt) {
        LOG.info("Requested skip: {}, actual: {}", startingAt, actual);
      }
      return is;
    } catch (IOException e) {
      throw new DocProxyException(500, "Error in getting document input stream: "
          + e.getMessage());
    }
  }

  public void addProperty(String key, Object value) {
    if (properties == null) {
      properties = new HashMap<String, Object>();
    }
    properties.put(key, value);
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public void setContentLength(long contentLength) {
    this.contentLength = contentLength;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = properties;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (contentLength ^ (contentLength >>> 32));
    result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
    result = prime * result + ((properties == null) ? 0 : properties.hashCode());
    result = prime * result + ((uri == null) ? 0 : uri.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    UrlDocumentResult other = (UrlDocumentResult) obj;
    if (contentLength != other.contentLength)
      return false;
    if (contentType == null) {
      if (other.contentType != null)
        return false;
    } else if (!contentType.equals(other.contentType))
      return false;
    if (properties == null) {
      if (other.properties != null)
        return false;
    } else if (!properties.equals(other.properties))
      return false;
    if (uri == null) {
      if (other.uri != null)
        return false;
    } else if (!uri.equals(other.uri))
      return false;
    return true;
  }

  public InputStream getDocumentInputStream(long startingAt, String userId)
      throws DocProxyException {
    // TODO Auto-generated method stub
    return null;
  }
}
