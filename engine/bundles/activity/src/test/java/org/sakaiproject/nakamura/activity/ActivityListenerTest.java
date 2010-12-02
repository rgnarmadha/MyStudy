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
package org.sakaiproject.nakamura.activity;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.activity.ActivityConstants;
import org.sakaiproject.nakamura.api.activity.ActivityUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

/**
 *
 */
public class ActivityListenerTest {

  private Session session;
  private Node activity;
  private String id = "2010-01-21-09-ad12ea31e3a1d2311c3";
  private String pathToActivity = "/sites/physics-101/_pages/activity/2010/01/21/09/";
  private String path = pathToActivity + id;

  @Before
  public void setUp() throws RepositoryException {
    session = createMock(Session.class);
    activity = createMock(Node.class);
    expect(activity.getName()).andReturn(id).anyTimes();
    expect(activity.getPath()).andReturn(path);
    replay(activity);
  }

  @Test
  public void testDelivery() throws RepositoryException {
    String activityFeedPath = "/_user/private/admin/activity";

    // Feed does not exist.
    Node activityFeedNode = returnBigStore(activityFeedPath, true);

    // Do the deepGetCreate dance
    Node adminNode = createMock(Node.class);
    expect(session.itemExists(activityFeedPath)).andReturn(false);
    expect(session.itemExists("/_user/private/admin")).andReturn(true)
        .anyTimes();
    expect(session.getItem("/_user/private/admin")).andReturn(adminNode)
        .anyTimes();
    expect(adminNode.hasNode("activity")).andReturn(false).anyTimes();
    expect(adminNode.addNode("activity")).andReturn(activityFeedNode)
        .anyTimes();
    session.save();

    replay(adminNode);

    // Skip the deepGetCreate dance on the saving in feed..
    Node parentNode = createMock(Node.class);
    expect(parentNode.isNew()).andReturn(false);
    
    String dest = ActivityUtils.getPathFromId(id, activityFeedPath);
    expect(session.itemExists("/_user/private/admin/activity/2010/01/21/09"))
        .andReturn(true);
    expect(session.getItem("/_user/private/admin/activity/2010/01/21/09")).andReturn(parentNode);

    prepareCopy(path, dest);
    replay(session);

    ActivityListener listener = new ActivityListener();
    listener.deliverActivityToFeed(session, activity, activityFeedPath);
  }

  @Test
  public void testCopy() throws RepositoryException {
    String src = "/sites/foo/activity/1/2/3/4/foobar";
    String dest = "/_user/private/activity/foobar";
    prepareCopy(src, dest);

    replay(session);

    ActivityListener listener = new ActivityListener();
    listener.copyActivityItem(session, src, dest);
    EasyMock.verify(session);
  }

  public void prepareCopy(String srcAbsPath, String destAbsPath)
      throws RepositoryException {
    Workspace workspace = createMock(Workspace.class);
    workspace.copy(srcAbsPath, destAbsPath);
    expect(session.getWorkspace()).andReturn(workspace);

    Node node = createMock(Node.class);
    expect(node.setProperty(ActivityConstants.PARAM_SOURCE, srcAbsPath))
        .andReturn(null);
    replay(node);
    expect(session.getItem(destAbsPath)).andReturn(node);
    expect(session.hasPendingChanges()).andReturn(true).anyTimes();
    session.save();

    replay(workspace);
  }

  /**
   * Creates a big store node
   * 
   * @param path
   *          The path to create it for
   * @param isNew
   *          Wether or not this node is new.
   * @return
   * @throws RepositoryException
   */
  public Node returnBigStore(String path, boolean isNew)
      throws RepositoryException {
    Node bigStoreNode = createMock(Node.class);
    expect(bigStoreNode.isNew()).andReturn(isNew);
    if (isNew) {
      expect(
          bigStoreNode.setProperty(
              JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY,
              ActivityConstants.ACTIVITY_FEED_RESOURCE_TYPE)).andReturn(null);
      expect(session.itemExists(path)).andReturn(true);
      expect(session.getItem(path)).andReturn(bigStoreNode).anyTimes();
      session.save();
    }
    replay(bigStoreNode);
    return bigStoreNode;
  }

}
