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
package org.sakaiproject.nakamura.presence;

import static org.junit.Assert.assertEquals;

import static org.easymock.EasyMock.expect;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.presence.PresenceService;
import org.sakaiproject.nakamura.api.presence.PresenceUtils;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.io.StringWriter;

/**
 *
 */
public class PresenceUtilsTest extends AbstractEasyMockTest {

  private static final String LOCATION = "myLoc";
  private static final String STATUS = "offline";

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testMakePresenceJSON() throws JSONException {
    StringWriter writer = new StringWriter();
    String userId = "jack";
    PresenceService presenceService = preparePresenceService(userId);

    replay();

    PresenceUtils.makePresenceJSON(writer, userId, presenceService);

    JSONObject o = new JSONObject(writer.toString());
    assertEquals(userId, o.get("user"));
    assertEquals(LOCATION, o.get(PresenceService.PRESENCE_LOCATION_PROP));
    assertEquals(STATUS, o.get(PresenceService.PRESENCE_STATUS_PROP));
    
  }

  private PresenceService preparePresenceService(String uuid) {
    PresenceService presenceService = createMock(PresenceService.class);
    expect(presenceService.getLocation(uuid)).andReturn(LOCATION);
    expect(presenceService.getStatus(uuid)).andReturn(STATUS);

    return presenceService;
  }

}
