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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.activity.ActivityRoute;
import org.sakaiproject.nakamura.api.activity.ActivityUtils;

/**
 *
 */
public class PersonalActivityRouterTest extends AbstractActivityRouterTest {

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }
  
  @Test
  public void testAdding() {
    replay();
    PersonalActivityRouter router = new PersonalActivityRouter();
    router.route(activity, routes);

    ActivityRoute route = routes.get(0);
    String dest = route.getDestination();

    Assert.assertEquals(ActivityUtils.getUserFeed(auJack), dest);
    verify();
  }

}
