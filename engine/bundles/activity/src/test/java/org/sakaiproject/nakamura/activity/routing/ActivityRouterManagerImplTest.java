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
package org.sakaiproject.nakamura.activity.routing;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.activity.ActivityRoute;
import org.sakaiproject.nakamura.api.activity.ActivityRouter;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.List;

import javax.jcr.Node;

/**
 *
 */
public class ActivityRouterManagerImplTest extends AbstractEasyMockTest {

  protected boolean called2;
  protected boolean called1;

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testSorting() {
    ActivityRouter router1 = new ActivityRouter() {

      public void route(Node activity, List<ActivityRoute> routes) {
        called1  = true;
      }

      public int getPriority() {
        return 50;
      }
    };
    ActivityRouter router2 = new ActivityRouter() {

      public void route(Node activity, List<ActivityRoute> routes) {
        called2  = true;
      }

      public int getPriority() {
        return 100;
      }
    };

    ActivityRouterManagerImpl manager = new ActivityRouterManagerImpl();
    manager.addActivityRouter(router1);
    manager.addActivityRouter(router2);

    List<ActivityRouter> sortedRouters = manager.getSortedList();
    Assert.assertEquals(100, sortedRouters.get(0).getPriority());
    Assert.assertEquals(50, sortedRouters.get(1).getPriority());

    Node activity = createNiceMock(Node.class);

    replay();
    manager.getActivityRoutes(activity);
    Assert.assertTrue(called1);
    Assert.assertTrue(called2);
    manager.removeActivityRouter(router1);
    called1 = false;
    called2 = false;
    manager.getActivityRoutes(activity);
    Assert.assertFalse(called1);
    Assert.assertTrue(called2);
    verify();
  }


}
