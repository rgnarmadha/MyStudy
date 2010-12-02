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
package org.sakaiproject.nakamura.discussion;

import static org.apache.sling.jcr.resource.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic.RepositoryBase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.sakaiproject.nakamura.api.discussion.DiscussionManager;
import org.sakaiproject.nakamura.api.message.MessageConstants;

import java.io.IOException;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

public class DiscussionManagerTest {

  private static BundleContext bundleContext;
  private static RepositoryBase repositoryBase;

  private static RepositoryBase getRepositoryBase() throws IOException,
      RepositoryException {
    if (repositoryBase == null) {
      bundleContext = Mockito.mock(BundleContext.class);
      repositoryBase = new RepositoryBase(bundleContext);
      repositoryBase.start();
      Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

        public void run() {
          if (repositoryBase != null) {
            repositoryBase.stop();
            repositoryBase = null;
          }
        }
      }));
    }
    return repositoryBase;
  }

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
  }

  public Repository getRepository() throws IOException, RepositoryException {
    return getRepositoryBase().getRepository();
  }

  /**
   * Login as administrator
   *
   * @return Returns the administrator session.
   * @throws LoginException
   * @throws RepositoryException
   * @throws IOException
   */
  private Session loginAsAdmin() throws LoginException, RepositoryException, IOException {
    return getRepository().login(new SimpleCredentials("admin", "admin".toCharArray()));
  }

  @Test
  public void testFindSettings() throws Exception {
    Session adminSession = loginAsAdmin();

    // Add a couple of nodes
    Node rootNode = adminSession.getRootNode();
    Node settingsNode = rootNode.addNode("settingsNode");
    settingsNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, "sakai/settings");
    settingsNode.setProperty("sakai:marker", "foo");
    settingsNode.setProperty("sakai:type", "discussion");

    Node randomNode = rootNode.addNode("foo");
    randomNode.setProperty("foo", "bar");

    adminSession.save();

    DiscussionManager manager = new DiscussionManagerImpl();
    Node result = manager.findSettings("foo", adminSession, "discussion");

    assertNotNull(result);
    assertEquals("/settingsNode", result.getPath());
  }

  @Test
  public void testFindMessage() throws Exception {
    Session adminSession = loginAsAdmin();

    // Add a couple of nodes
    Node rootNode = adminSession.getRootNode();
    Node messagesNode = rootNode.addNode("messages");
    Node msgNode = messagesNode.addNode("msgNodeCorrect");
    msgNode.setProperty(SLING_RESOURCE_TYPE_PROPERTY, MessageConstants.SAKAI_MESSAGE_RT);
    msgNode.setProperty("sakai:marker", "foo");
    msgNode.setProperty("sakai:type", "discussion");
    msgNode.setProperty("sakai:id", "10");

    Node msgNode2 = messagesNode.addNode("msgNodeCorrect2");
    msgNode2.setProperty(SLING_RESOURCE_TYPE_PROPERTY, MessageConstants.SAKAI_MESSAGE_RT);
    msgNode2.setProperty("sakai:marker", "foo");
    msgNode2.setProperty("sakai:type", "discussion");
    msgNode2.setProperty("sakai:id", "20");

    Node randomNode = messagesNode.addNode("foo");
    randomNode.setProperty("foo", "bar");

    adminSession.save();

    DiscussionManager manager = new DiscussionManagerImpl();
    Node result = manager.findMessage("10", "foo", adminSession, "/messages");

    assertNotNull(result);
    assertEquals("/messages/msgNodeCorrect", result.getPath());
  }
}
