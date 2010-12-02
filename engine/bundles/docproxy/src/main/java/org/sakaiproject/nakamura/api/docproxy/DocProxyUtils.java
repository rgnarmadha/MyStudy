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

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_REF;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.RT_EXTERNAL_REPOSITORY;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.RT_EXTERNAL_REPOSITORY_DOCUMENT;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * A couple of utility classes for Document Proxying.
 */
public class DocProxyUtils {

  /**
   * Checks whether or not a node is the config for an external repository
   *
   * @param node
   *          The node to check.
   * @return true = the node is a doc proxy node, false it is not or is null.
   */
  public static boolean isExternalRepositoryConfig(Node node) {
    try {
      return (node != null && node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY) && node
          .getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString()
          .equals(RT_EXTERNAL_REPOSITORY));
    } catch (RepositoryException e) {
      return false;
    }
  }

  /**
   * Checks whether or not a node represents a document in an external repository.
   *
   * @param node
   *          The node to check.
   * @return true = the node is a doc proxy node, false it is not or is null.
   */
  public static boolean isExternalRepositoryDocument(Node node) {
    try {
      return (node != null && node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY) && node
          .getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString()
          .equals(RT_EXTERNAL_REPOSITORY_DOCUMENT));
    } catch (RepositoryException e) {
      return false;
    }
  }

  /**
   * Outputs an {@link ExternalDocumentResultMetadata} in JSON.
   *
   * @param write
   *          The {@link JSONWriter} to write to.
   * @param meta
   *          The {@link ExternalDocumentResultMetadata} to output.
   * @throws JSONException
   * @throws DocProxyException
   */
  public static void writeMetaData(ExtendedJSONWriter write,
      ExternalDocumentResultMetadata meta) throws JSONException, DocProxyException {
    write.object();
    write.key("Content-Type");
    write.value(meta.getContentType());
    write.key("Content-Length");
    write.value(meta.getContentLength());
    write.key("uri");
    write.value(meta.getUri());
    write.key("properties");
    ValueMap map = new ValueMapDecorator(meta.getProperties());
    write.valueMap(map);
    write.endObject();
  }

  /**
   * Get a proxy document node which holds information over an external repository,
   * processor ..
   *
   * @param node
   *          This node should hold a property {@link DocProxyConstants.REPOSITORY_REF}
   *          with a value of the UUID of the proxy node.
   * @return The proxy node.
   * @throws DocProxyException
   *           There was no reference found to an external repository.
   */
  public static Node getProxyNode(Node node) throws DocProxyException {
    try {
      String ref = node.getProperty(REPOSITORY_REF).getString();
      Session session = node.getSession();
      Node retval = null;
      if (ref.startsWith("/")) {
        Item item = session.getItem(ref);
        if (item.isNode()) {
          retval = (Node) item;
        }
      } else {
        retval = session.getNodeByIdentifier(ref);
      }
      return retval;
    } catch (RepositoryException e) {
      throw new DocProxyException(500,
          "This node holds no reference to an external repository node.");
    }
  }
}
