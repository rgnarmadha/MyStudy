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

import static org.easymock.EasyMock.expect;

import static org.junit.Assert.assertNull;

import static org.junit.Assert.assertEquals;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.sling.MockResource;
import org.junit.Test;
import org.sakaiproject.nakamura.discussion.searchresults.DiscussionInitialPostPropertyProvider;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class InitialPostPropertyProviderTest extends AbstractEasyMockTest {

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest#setUp()
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testPath() {
    DiscussionInitialPostPropertyProvider provider = new DiscussionInitialPostPropertyProvider();

    Map<String, String> propertiesMap = new HashMap<String, String>();

    Resource resource = new MockResource(null, "/foo", null);

    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getResource()).andReturn(resource);
    addStringRequestParameter(request, "path", "/aa/00/aa");

    replay();
    provider.loadUserProperties(request, propertiesMap);

    // Remove normal path string from map.
    assertNull(propertiesMap.get("path"));
    // Proper escaping.
    assertEquals("/aa/_x0030_0/aa", propertiesMap.get("_path"));
  }

}
