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

package org.sakaiproject.nakamura.email.outgoing;

import org.apache.jackrabbit.JcrConstants;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class JcrEmailDataSource implements DataSource {
  private static final Logger LOGGER = LoggerFactory.getLogger(JcrEmailDataSource.class);

  private Node node;

  public JcrEmailDataSource(Node node) {
    this.node = node;
  }

  public String getContentType() {
    String ct = "application/octet-stream";
    try {
      if (node.getPrimaryNodeType().getName().equals(JcrConstants.NT_FILE)) {
        Node contentNode = node.getNode(JcrConstants.JCR_CONTENT);
        ct = contentNode.getProperty(JcrConstants.JCR_MIMETYPE).getString();
      } else {
        if (node.hasProperty(MessageConstants.PROP_SAKAI_CONTENT_TYPE)) {
          ct = node.getProperty(MessageConstants.PROP_SAKAI_CONTENT_TYPE).getString();
        }
      }
    } catch (RepositoryException re) {
      LOGGER.error(re.getMessage(), re);
    }
    return ct;
  }

  public InputStream getInputStream() throws IOException {
    InputStream is = null;
    try {
      if (node.getPrimaryNodeType().getName().equals(JcrConstants.NT_FILE)) {
        Node contentNode = node.getNode(JcrConstants.JCR_CONTENT);
        is = contentNode.getProperty(JcrConstants.JCR_DATA).getBinary().getStream();
      } else {
        is = node.getProperty(MessageConstants.PROP_SAKAI_ATTACHMENT_CONTENT).getBinary()
            .getStream();
      }
    } catch (RepositoryException e) {
      LOGGER.error(e.getMessage(), e);
      // in Java 1.6 this would be "throw new IOException(e)" the following is a 1.5 hack
      // to get the same result
      throw (IOException) new IOException(e.getMessage()).initCause(e);
    }
    return is;
  }

  public String getName() {
    String name = null;
    try {
      name = node.getName();
    } catch (RepositoryException re) {
      LOGGER.error(re.getMessage(), re);
    }
    return name;
  }

  public OutputStream getOutputStream() throws IOException {
    throw new IOException("This data source is designed for read only.");
  }

}
