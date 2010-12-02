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
package org.sakaiproject.nakamura.search;

import static org.junit.Assert.fail;

import static org.junit.Assert.assertEquals;
import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_EXCLUDE_TREE;

import org.apache.sling.commons.testing.jcr.MockNode;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.search.SakaiSearchRowIterator;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;
import org.sakaiproject.nakamura.testutils.easymock.MockRowIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 *
 */
public class SakaiSearchRowIteratorTest extends AbstractEasyMockTest {

  private SakaiSearchRowIterator sakaiIterator;

  @Before
  public void setUp() {
  }

  @Test
  public void testHasNext() throws Exception {
    Node nodeA = createNode("/path/to/nodeA", false);
    Node nodeB = createNode("/path/to/nodeB", true);

    List<Node> nodes = new ArrayList<Node>();
    nodes.add(nodeA);
    nodes.add(nodeB);

    MockRowIterator rowIterator = new MockRowIterator(nodes);
    sakaiIterator = new SakaiSearchRowIterator(rowIterator);
    int i = 0;
    while (sakaiIterator.hasNext()) {
      i++;
      sakaiIterator.next();
    }
    assertEquals(1, i);
  }

  @Test
  public void testBlacklistedPaths() throws Exception {
    Node nodeA = createNode("/path/to/nodeA", false);
    Node nodeB = createNode("/path/to/nodeB", true);
    Node nodeC = new MockNode("/path/to/black/listed/to/nodeC");

    List<Node> nodes = new ArrayList<Node>();
    nodes.add(nodeA);
    nodes.add(nodeB);
    nodes.add(nodeC);

    String[] blacklistedPaths = new String[] { "/path/to/black/listed" };

    MockRowIterator rowIterator = new MockRowIterator(nodes);
    sakaiIterator = new SakaiSearchRowIterator(rowIterator, blacklistedPaths);
    int i = 0;
    while (sakaiIterator.hasNext()) {
      i++;
      sakaiIterator.next();
    }
    assertEquals(1, i);
  }

  @Test
  public void testSkip() throws Exception {
    Node nodeA = createNode("/path/to/nodeA", false);
    Node nodeB = createNode("/path/to/nodeB", false);
    Node nodeC = createNode("/path/to/nodeC", false);
    Node nodeD = createNode("/path/to/nodeD", false);
    Node rootNode = createNode("/", false);

    List<Node> nodes = new ArrayList<Node>();
    nodes.add(nodeA);
    nodes.add(nodeB);
    nodes.add(nodeC);
    nodes.add(nodeD);
    nodes.add(rootNode);
    MockRowIterator rowIterator = new MockRowIterator(nodes);
    sakaiIterator = new SakaiSearchRowIterator(rowIterator);
    sakaiIterator.skip(3);
    int i = 0;
    while (sakaiIterator.hasNext()) {
      i++;
      sakaiIterator.next();
    }
    assertEquals(2, i);

    try {
      // This should throw a NoSuchElementException
      sakaiIterator.next();
      fail("next() should have thrown a NoSuchElementException when there are no more elements.");
    } catch (NoSuchElementException e) {

    }
  }

  protected Node createNode(String path, boolean excludeTree) throws RepositoryException {
    MockNode node = new MockNode(path);
    node.setProperty(SAKAI_EXCLUDE_TREE, excludeTree);
    return node;
  }
}
