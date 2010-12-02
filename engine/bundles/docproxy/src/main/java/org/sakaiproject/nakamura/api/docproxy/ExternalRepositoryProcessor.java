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
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;

/**
 * This interface defines a repository processor that will be implemented by various
 * external repository providers to integrate content into the content tree of K2.
 * 
 * Implementations of this interface will be identified by name and referenced by those
 * names.
 */
public interface ExternalRepositoryProcessor {

  /**
   * Creates or updates a new resource in the remote system relative to the path and
   * modifies the node appropriately.
   * 
   * @param node
   *          The node, with properties where the document proxy is created, the contents
   *          of the node may be modified by the operation to store properties and
   *          metadata associated with the JCR node within K2.
   * @param path
   *          optional path identifying the resource, if null it will be ignored, but can
   *          be ignored by the implementation. Most of the time this will be part of the
   *          URL behind the node. ex: /docproxy/disk/foo/bar/readme.txt where disk is the
   *          DocProxy node, the path would be /foo/bar/readme.txt
   * @param properties
   *          A map of properties to associated with the document, either remotely or
   *          locally on the node, implementation specific. If a property is null it will
   *          be removed, if present it will modify the existing property, if an array it
   *          will add to the array.
   * @param documentStream
   *          an input stream containing the body of the document, if null then no body
   *          has been supplied.
   * @param streamLength
   *          the length of the stream in bytes. If set to -1, the length is unknown and
   *          the implementation should read until EOF.
   * @throws DocProxyException
   */
  public Map<String, Object> updateDocument(Node node, String path,
      Map<String, Object> properties, InputStream documentStream, long streamLength)
      throws DocProxyException;

  /**
   * Gets an ExternalDocumentResult from the external repository.
   * 
   * @param node
   *          the node representing the document.
   * @param path
   *          optional path identifying the resource, if null it will be ignored, but can
   *          be ignored by the implementation. Most of the time this will be part of the
   *          URL behind the node. ex: /docproxy/disk/foo/bar/readme.txt where disk is the
   *          DocProxy node, the path would be /foo/bar/readme.txt
   * @throws DocProxyException
   *           When something goes wrong this should contain an appropriate HTTP status
   *           code and message.
   */
  public ExternalDocumentResult getDocument(Node node, String path)
      throws DocProxyException;

  /**
   * Gets just the metadata of the External Document.
   * 
   * @param node
   *          the node representing the external document.
   * @param path
   *          optional path identifying the resource, if null it will be ignored, but can
   *          be ignored by the implementation. Most of the time this will be part of the
   *          URL behind the node. ex: /docproxy/disk/foo/bar/readme.txt where disk is the
   *          DocProxy node, the path would be /foo/bar/readme.txt
   * @throws DocProxyException
   *           When something goes wrong this should contain an appropriate HTTP status
   *           code and message.
   * @return
   */
  public ExternalDocumentResultMetadata getDocumentMetadata(Node node, String path)
      throws DocProxyException;

  /**
   * Searches for results matching the search properties. The implementation should return
   * results in an efficient manner if appropriate only retrieving the
   * EternalDocumentResult on demand and only retrieving the contents of the
   * ExternalDocumentResult on demand. The implementation should not typically try and
   * load all matching documents.
   * 
   * @param node
   *          The node containing the repository information.
   * @param searchProperties
   *          a key value map of search fields and search values. If the field is
   *          tokenized then the search should be a substring search, if the field is a
   *          keyword the search should be a keyword search.
   * @throws DocProxyException
   *           The search failed for some reason, this should contain an appropriate HTTP
   *           status code and message.
   * @return a lazy iterator of ExternalDocumentResults.
   */
  public Iterator<ExternalDocumentResult> search(Node node,
      Map<String, Object> searchProperties) throws DocProxyException;

   /**
    * Removes the specified document from the external repository.
    * 
    * If it is not appropriate for Sakai to be able to initiate the removal of documents,
    * the implementor can throw an exception or implement this as a no op.
    * @param node
    *          the node representing the document.
    * @param path
    *          optional path identifying the resource, if null it will be ignored, but can
    *          be ignored by the implementation. Most of the time this will be part of the
    *          URL behind the node. ex: /docproxy/disk/foo/bar/readme.txt where disk is the
    *          DocProxy node, the path would be /foo/bar/readme.txt
    * @throws DocProxyException
    */
   public void removeDocument(Node node, String path)
       throws DocProxyException;

  /**
   * @return What kind of external repository this processor should handle.
   */
  public String getType();
}
