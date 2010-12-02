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
package org.sakaiproject.nakamura.site.search;

import static org.junit.Assert.fail;

import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import static org.easymock.EasyMock.expect;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ContentSearchPropertyProviderTest extends AbstractEasyMockTest {

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testProperties() {
    ContentSearchPropertyProvider provider = new ContentSearchPropertyProvider();
    Map<String, String> map = new HashMap<String, String>();

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    addStringRequestParameter(request, "site", "physics");
    replay();
    provider.loadUserProperties(request, map);

    Assert.assertEquals(" AND @id = 'physics'", map.get("_site"));
  }

  @Test
  public void testBadParameter() {
    ContentSearchPropertyProvider provider = new ContentSearchPropertyProvider();
    Map<String, String> map = new HashMap<String, String>();

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    addStringRequestParameter(request, "site", "phys' and @foo='bar");
    replay();
    provider.loadUserProperties(request, map);
    System.err.println(map.get("_site"));
    if (" AND @id = 'phys' and @foo='bar'".equals(map.get("_site"))) {
      fail("The property provider should take care of property injection.");
    }
  }

  @Test
  public void testNoParameter() {
    ContentSearchPropertyProvider provider = new ContentSearchPropertyProvider();
    Map<String, String> map = new HashMap<String, String>();

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getRequestParameter("site")).andReturn(null);
    replay();
    provider.loadUserProperties(request, map);

    Assert.assertEquals(null, map.get("_site"));
  }
}
