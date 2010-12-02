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
package org.sakaiproject.nakamura.site;

import static org.junit.Assert.assertEquals;

import static org.sakaiproject.nakamura.api.site.SiteConstants.JOINABLE;

import static org.junit.Assert.assertTrue;
import static org.sakaiproject.nakamura.api.site.SiteConstants.SAKAI_IS_SITE_TEMPLATE;

import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.site.SiteService.Joinable;

/**
 *
 */
public class SiteServiceImplTest {

  private SiteServiceImpl siteService;

  @Before
  public void setUp() {
    siteService = new SiteServiceImpl();
  }

  @Test
  public void testIsSiteTemplate() throws Exception {
    MockNode node = new MockNode("/path/to/template");
    node.setProperty(SAKAI_IS_SITE_TEMPLATE, true);
    boolean result = siteService.isSiteTemplate(node);
    assertTrue(result);
  }

  public void testGetJoinable() throws Exception {
    MockNode node = new MockNode("/path/to/site");
    node.setProperty(JOINABLE, "withauth");
    Joinable result = siteService.getJoinable(node);
    assertEquals(Joinable.withauth, result);
    
    node.setProperty(JOINABLE, "foobar");
    assertEquals(Joinable.no, result);
  }

}
