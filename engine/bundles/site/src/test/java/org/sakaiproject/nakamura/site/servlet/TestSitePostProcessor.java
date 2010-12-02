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
package org.sakaiproject.nakamura.site.servlet;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class TestSitePostProcessor {

  private ArrayList<Modification> changes;
  private Item item;
  private Session session;
  private ResourceResolver resolver;
  private SlingHttpServletRequest request;
  private SitePostProcessor processor;
  private String itemPath = "/some/string";

  @Before
  public void setUp()
  {
    processor = new SitePostProcessor();
    request = createNiceMock(SlingHttpServletRequest.class);
    resolver = createNiceMock(ResourceResolver.class);
    session = createNiceMock(Session.class);
    item = createNiceMock(Item.class);

    expect(request.getResourceResolver()).andReturn(resolver);
    expect(resolver.adaptTo(eq(Session.class))).andReturn(session);

    Modification modification = new Modification(ModificationType.MODIFY, itemPath, null);
    changes = new ArrayList<Modification>();
    changes.add(modification);
  }

  @Test
  public void testModifySite() throws Exception
  {
    expect(session.itemExists(eq(itemPath))).andReturn(true);
    expect(session.getItem(eq(itemPath))).andReturn(item);
    expect(item.isNode()).andReturn(true);
    checkSatisfied();
  }

  @Test
  public void testModifySiteProperty() throws Exception
  {
    Node node = createNiceMock(Node.class);
    expect(session.itemExists(eq(itemPath))).andReturn(true);
    expect(session.getItem(eq(itemPath))).andReturn(item);
    expect(item.isNode()).andReturn(false);
    expect(item.getParent()).andReturn(node);
    checkSatisfied();
  }

  @Test
  public void testModifySiteException() throws Exception
  {
    expect(session.itemExists(eq(itemPath))).andThrow(new RepositoryException("Exceptional"));
    checkSatisfied();
  }

  private void checkSatisfied() throws Exception {
    replay(request, resolver, session, item);
    processor.process(request, changes);
    verify(request, resolver, session, item);
  }

}
