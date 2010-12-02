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
import static org.easymock.EasyMock.expect;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_LOCATION;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.REPOSITORY_PROCESSOR;
import static org.sakaiproject.nakamura.api.docproxy.DocProxyConstants.RT_EXTERNAL_REPOSITORY;

import org.apache.sling.commons.testing.jcr.MockNode;
import org.easymock.EasyMock;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.docproxy.DocProxyException;
import org.sakaiproject.nakamura.api.docproxy.ExternalRepositoryProcessor;
import org.sakaiproject.nakamura.docproxy.disk.DiskProcessor;
import org.sakaiproject.nakamura.docproxy.disk.DiskProcessorTest;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
public class AbstractDocProxyServlet extends AbstractEasyMockTest {
  protected MockNode proxyNode;
  protected String currPath;
  protected ExternalRepositoryProcessorTracker tracker;
  protected BundleContext bundleContext;
  protected ComponentContext componentContext;
  protected DiskProcessor diskProcessor;
  protected Session session;

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest#setUp()
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();

    // Setup a proxy node.
    String readmePath = getClass().getClassLoader().getResource("README").getPath();
    currPath = readmePath.substring(0, readmePath.lastIndexOf("/"));

    proxyNode = new MockNode("/docproxy/disk");
    proxyNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, RT_EXTERNAL_REPOSITORY);
    proxyNode.setProperty(REPOSITORY_PROCESSOR, "disk");
    proxyNode.setProperty(REPOSITORY_LOCATION, currPath);

    session = createProxySession();
    proxyNode.setSession(session);

    // Mock up the tracker
    diskProcessor = new DiskProcessor();
    bundleContext = expectServiceTrackerCalls(ExternalRepositoryProcessor.class.getName());

    componentContext = EasyMock.createMock(ComponentContext.class);
    expect(componentContext.getBundleContext()).andReturn(bundleContext);
    EasyMock.replay(componentContext);

    tracker = new ExternalRepositoryProcessorTracker(bundleContext,
        ExternalRepositoryProcessor.class.getName(), null);

    tracker.putProcessor(diskProcessor, "disk");

  }

  /**
   * @return
   */
  private Session createProxySession() throws RepositoryException {
    Session session = createMock(Session.class);
    expect(session.itemExists("/docproxy/disk")).andReturn(true).anyTimes();
    expect(session.itemExists("/docproxy")).andReturn(true).anyTimes();
    expect(session.itemExists("/")).andReturn(true).anyTimes();
    expect(session.getItem("/docproxy/disk")).andReturn(proxyNode).anyTimes();
    return session;
  }

  public Node createFile(DiskProcessor processor, Node proxyNode, String path,
      String content) throws PathNotFoundException, RepositoryException,
      UnsupportedEncodingException, DocProxyException {
    ByteArrayInputStream stream = new ByteArrayInputStream(content.getBytes("UTF-8"));
    Node node = DiskProcessorTest.createFile(processor, proxyNode, path, stream);
    session = createProxySession();
    ((MockNode) proxyNode).setSession(session);
    return node;
  }

}
