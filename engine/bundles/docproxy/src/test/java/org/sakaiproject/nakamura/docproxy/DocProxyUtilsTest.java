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
package org.sakaiproject.nakamura.docproxy;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.RT_EXTERNAL_REPOSITORY;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.RT_EXTERNAL_REPOSITORY_DOCUMENT;

import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.Test;
import org.sakaiproject.nakamura.api.docproxy.DocProxyUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

/**
 *
 */
public class DocProxyUtilsTest {

  @Test
  public void testRepositoryConfig() throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException {
    Node node = new MockNode("/docproxy/disk");
    node.setProperty(SLING_RESOURCE_TYPE_PROPERTY, RT_EXTERNAL_REPOSITORY);
    boolean result = DocProxyUtils.isExternalRepositoryConfig(node);
    assertEquals(true, result);

    node.setProperty(SLING_RESOURCE_TYPE_PROPERTY, "foo");
    result = DocProxyUtils.isExternalRepositoryConfig(node);
    assertEquals(false, result);

    Node mockedNode = createMock(Node.class);
    expect(mockedNode.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)).andReturn(true);
    expect(mockedNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY)).andThrow(
        new RepositoryException());
    replay(mockedNode);

    result = DocProxyUtils.isExternalRepositoryConfig(mockedNode);
    assertEquals(false, result);

  }

  @Test
  public void testRepositoryDocument() throws ValueFormatException, VersionException,
      LockException, ConstraintViolationException, RepositoryException {
    Node node = new MockNode("/docproxy/disk/foo/document.doc");
    node.setProperty(SLING_RESOURCE_TYPE_PROPERTY, RT_EXTERNAL_REPOSITORY_DOCUMENT);
    boolean result = DocProxyUtils.isExternalRepositoryDocument(node);
    assertEquals(true, result);

    node.setProperty(SLING_RESOURCE_TYPE_PROPERTY, "foo");
    result = DocProxyUtils.isExternalRepositoryDocument(node);
    assertEquals(false, result);

    Node mockedNode = createMock(Node.class);
    expect(mockedNode.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)).andReturn(true);
    expect(mockedNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY)).andThrow(
        new RepositoryException());
    replay(mockedNode);

    result = DocProxyUtils.isExternalRepositoryDocument(mockedNode);
    assertEquals(false, result);
  }

}
