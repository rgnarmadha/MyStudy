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

import static org.easymock.EasyMock.expect;

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.sakaiproject.nakamura.api.activity.ActivityConstants;
import org.sakaiproject.nakamura.api.activity.ActivityRoute;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Value;

/**
 *
 */
public abstract class AbstractActivityRouterTest extends AbstractEasyMockTest {

  protected Node activity;
  protected String user = "jack";
  protected String path = "/sites/foo/_pages/welcome/activity";
  protected List<ActivityRoute> routes = new ArrayList<ActivityRoute>();

  protected JackrabbitSession session;
  protected Authorizable auJack;
  protected UserManager um;

  public void setUp() throws Exception {
    super.setUp();
    activity = createMock(Node.class);

    session = createMock(JackrabbitSession.class);
    auJack = createAuthorizable(user, false, true);
    um = createUserManager(null, true, auJack);
    expect(activity.hasProperty(ActivityConstants.ACTIVITY_ITEM_RESOURCE_TYPE))
        .andReturn(true).anyTimes();
    expect(session.getUserManager()).andReturn(um).anyTimes();
    expect(activity.getSession()).andReturn(session).anyTimes();

    Property actorProperty = createMock(Property.class);
    expect(actorProperty.getString()).andReturn(user).anyTimes();

    Value valActor = createMock(Value.class);
    expect(valActor.getString()).andReturn(user).anyTimes();
    expect(actorProperty.getValue()).andReturn(valActor).anyTimes();

    expect(activity.getProperty(ActivityConstants.PARAM_ACTOR_ID)).andReturn(
        actorProperty).anyTimes();

    expect(activity.getPath()).andReturn(path).anyTimes();

  }

}
