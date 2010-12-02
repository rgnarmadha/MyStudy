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

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import static org.easymock.EasyMock.expect;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 *
 */
public class AggregateCountTest extends AbstractEasyMockTest {

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testCount() throws RepositoryException {
    String[] checkFields = new String[] { "sakai:tag" };
    AggregateCount aggregate = new AggregateCount(checkFields, true);

    Map<String, String> properties = new HashMap<String, String>();
    properties.put("sling:resourceType", "foo/bar");
    properties.put("sakai:tag", "foo");

    for (int i = 0; i < 50; i++) {
      Node node = createNode(properties, "/foo/bar/" + i, false);
      aggregate.add(node);
    }
    // Create 2 extra nodes we should have already processed.
    Node sameNodeA = createNode(properties, "/foo/bar/0", false);
    aggregate.add(sameNodeA);
    Node sameNodeB = createNode(properties, "/foo/bar/1", false);
    aggregate.add(sameNodeB);
    Node nodeWithChilds = createNode(properties, "/foo/bar/bla", true);
    aggregate.add(nodeWithChilds);

    Map<String, Map<String, Integer>> map = aggregate.getAggregate();

    Assert.assertEquals(52, Integer.parseInt(map.get("sakai:tag").get("foo")
        .toString()));

  }

  /**
   * Create a node with a couple of string properties.
   */
  protected Node createNode(Map<String, String> properties, String path,
      boolean children) throws RepositoryException {
    Node node = EasyMock.createMock(Node.class);
    expect(node.getPath()).andReturn(path).anyTimes();
    for (Entry<String, String> e : properties.entrySet()) {
      String key = e.getKey();
      String pvalue = e.getValue();
      Value val = EasyMock.createMock(Value.class);
      expect(val.getString()).andReturn(pvalue).anyTimes();
      expect(val.getType()).andReturn(PropertyType.STRING).anyTimes();
      EasyMock.replay(val);

      PropertyDefinition propDef = createMock(PropertyDefinition.class);
      expect(propDef.isMultiple()).andReturn(false).anyTimes();
      EasyMock.replay(propDef);

      Property prop = createMock(Property.class);
      expect(prop.getType()).andReturn(PropertyType.STRING).anyTimes();
      expect(prop.getValue()).andReturn(val).anyTimes();
      expect(prop.getString()).andReturn(pvalue).anyTimes();
      expect(prop.getDefinition()).andReturn(propDef).anyTimes();
      EasyMock.replay(prop);

      expect(node.hasProperty(key)).andReturn(true).anyTimes();
      expect(node.getProperty(key)).andReturn(prop).anyTimes();

    }
    if (children) {
      expect(node.hasNodes()).andReturn(true).anyTimes();
      NodeIterator nodeIterator = createMock(NodeIterator.class);
      expect(nodeIterator.hasNext()).andReturn(true);
      Node childNode = createNode(properties, path + "/child", false);
      expect(nodeIterator.nextNode()).andReturn(childNode);
      expect(nodeIterator.hasNext()).andReturn(false);
      EasyMock.replay(nodeIterator);
      expect(node.getNodes()).andReturn(nodeIterator).anyTimes();
    } else {
      expect(node.hasNodes()).andReturn(false).anyTimes();
      NodeIterator nodeIterator = createMock(NodeIterator.class);
      expect(nodeIterator.hasNext()).andReturn(false);
      expect(node.getNodes()).andReturn(nodeIterator).anyTimes();
      EasyMock.replay(nodeIterator);
    }
    EasyMock.replay(node);

    return node;

  }
}
