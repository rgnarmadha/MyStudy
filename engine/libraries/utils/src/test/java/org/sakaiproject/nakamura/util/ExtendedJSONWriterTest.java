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
package org.sakaiproject.nakamura.util;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 *
 */
public class ExtendedJSONWriterTest {

  private List<Object> mocks;

  @Before
  public void setUp() {
    mocks = new ArrayList<Object>();
  }

  @Test
  public void testValueMap() {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("l", 1.5);
    map.put("foo", "bar");

    ValueMapDecorator valueMap = new ValueMapDecorator(map);

    StringWriter writer = new StringWriter();
    ExtendedJSONWriter ext = new ExtendedJSONWriter(writer);
    try {
      ext.valueMap(valueMap);

      String s = writer.toString();
      assertEquals("Returned JSON was not as excepted",
          "{\"foo\":\"bar\",\"l\":1.5}", s);

    } catch (JSONException e) {
      fail("A good ValueMap should not throw a JSONException.");
    }
  }

  @Test
  public void testNode() {
    try {
      Node node = createMock(Node.class);
      expect(node.getPath()).andReturn("/path/to/node");
      expect(node.getName()).andReturn("node");
      PropertyIterator propertyIterator = createMock(PropertyIterator.class);
      PropertyDefinition propDefSingle = createMock(PropertyDefinition.class);
      PropertyDefinition propDefMultiple = createMock(PropertyDefinition.class);
      expect(propDefSingle.isMultiple()).andReturn(false).anyTimes();
      expect(propDefMultiple.isMultiple()).andReturn(true).anyTimes();

      // Properties
      // Double
      Property doubleProp = createMock(Property.class);
      expect(doubleProp.getType()).andReturn(PropertyType.DOUBLE);
      Value doubleValue = createMock(Value.class);

      expect(doubleValue.getType()).andReturn(PropertyType.DOUBLE);
      expect(doubleValue.getDouble()).andReturn(Double.parseDouble("1.5"));
      expect(doubleProp.getName()).andReturn("doub").once();
      expect(doubleProp.getDefinition()).andReturn(propDefSingle);
      expect(doubleProp.getValue()).andReturn(doubleValue).once();

      // Multi string prop
      Property multiStringProp = createMock(Property.class);
      expect(multiStringProp.getType()).andReturn(PropertyType.STRING).once();
      expect(multiStringProp.getDefinition()).andReturn(propDefMultiple).once();
      expect(multiStringProp.getName()).andReturn("multiString").once();
      Value[] multiStringValues = new Value[2];
      multiStringValues[0] = createMock(Value.class);
      multiStringValues[1] = createMock(Value.class);
      expect(multiStringValues[0].getType()).andReturn(PropertyType.STRING);
      expect(multiStringValues[1].getType()).andReturn(PropertyType.STRING);
      expect(multiStringValues[0].getString()).andReturn("foo");
      expect(multiStringValues[1].getString()).andReturn("bar");
      expect(multiStringProp.getValues()).andReturn(multiStringValues);

      // Iterator
      expect(propertyIterator.hasNext()).andReturn(true);
      expect(propertyIterator.nextProperty()).andReturn(doubleProp);
      expect(propertyIterator.hasNext()).andReturn(true);
      expect(propertyIterator.nextProperty()).andReturn(multiStringProp);
      expect(propertyIterator.hasNext()).andReturn(false);

      expect(node.getProperties()).andReturn(propertyIterator).anyTimes();

      replay();

      StringWriter writer = new StringWriter();
      ExtendedJSONWriter ext = new ExtendedJSONWriter(writer);
      try {
        ext.node(node);
        writer.flush();
        String s = writer.toString();
        JSONObject o = new JSONObject(s);
        assertEquals(1.5, o.getDouble("doub"), 0);
        assertEquals(2, o.getJSONArray("multiString").length());
        assertEquals("/path/to/node", o.get("jcr:path"));
        assertEquals("node", o.get("jcr:name"));

      } catch (JSONException e) {
        fail("Should not throw a JSONException.");
      }
    } catch (RepositoryException e) {
      fail("Should not throw a RepositoryException.");
    }
  }


  @Test
  public void writeNodeTreeToWriterNoSubNodes() throws Exception {
    Node node = createMock(Node.class);
    expect(node.getPath()).andReturn("/path/to/node");
    expect(node.getName()).andReturn("node");

    NodeIterator nodes = createMock(NodeIterator.class);
    expect(nodes.hasNext()).andReturn(false);
    expect(node.getNodes()).andReturn(nodes);

    PropertyIterator propertyIterator = createMock(PropertyIterator.class);
    PropertyDefinition propDefSingle = createMock(PropertyDefinition.class);
    expect(propDefSingle.isMultiple()).andReturn(false).anyTimes();

    Property doubleProp = createMock(Property.class);
    expect(doubleProp.getType()).andReturn(PropertyType.DOUBLE);
    Value doubleValue = createMock(Value.class);

    expect(doubleValue.getType()).andReturn(PropertyType.DOUBLE);
    expect(doubleValue.getDouble()).andReturn(Double.valueOf(1.5));
    expect(doubleProp.getName()).andReturn("doub").once();
    expect(doubleProp.getDefinition()).andReturn(propDefSingle);
    expect(doubleProp.getValue()).andReturn(doubleValue).once();

    // Iterator
    expect(propertyIterator.hasNext()).andReturn(true);
    expect(propertyIterator.nextProperty()).andReturn(doubleProp);
    expect(propertyIterator.hasNext()).andReturn(false);

    expect(node.getProperties()).andReturn(propertyIterator).anyTimes();

    replay();

    StringWriter writer = new StringWriter();
    ExtendedJSONWriter ext = new ExtendedJSONWriter(writer);
    ExtendedJSONWriter.writeNodeTreeToWriter(ext, node);
    writer.flush();
    String s = writer.toString();
    JSONObject o = new JSONObject(s);
    assertEquals(1.5, o.getDouble("doub"), 0);
    assertEquals("/path/to/node", o.get("jcr:path"));
    assertEquals("node", o.get("jcr:name"));
  }

  @Test
  public void writeNodeTreeToWriterSubNodes() throws Exception {
    Node node = createMock(Node.class);
    expect(node.getPath()).andReturn("/path/to/node");
    expect(node.getName()).andReturn("node");

    Node subNode1 = createMock(Node.class);
    expect(subNode1.getPath()).andReturn("/path/to/node/sub1").anyTimes();
    expect(subNode1.getName()).andReturn("sub1").anyTimes();

    Node subNode2 = createMock(Node.class);
    expect(subNode2.getPath()).andReturn("/path/to/node/sub1/sub2").anyTimes();
    expect(subNode2.getName()).andReturn("sub2").anyTimes();

    NodeIterator nodes = createMock(NodeIterator.class);
    expect(node.getNodes()).andReturn(nodes);
    expect(nodes.hasNext()).andReturn(true);
    expect(nodes.nextNode()).andReturn(subNode1);
    expect(nodes.hasNext()).andReturn(false);

    NodeIterator subNodes1 = createMock(NodeIterator.class);
    expect(subNode1.getNodes()).andReturn(subNodes1);
    expect(subNodes1.hasNext()).andReturn(true);
    expect(subNodes1.nextNode()).andReturn(subNode2);
    expect(subNodes1.hasNext()).andReturn(false);

    NodeIterator subNodes2 = createMock(NodeIterator.class);
    expect(subNode2.getNodes()).andReturn(subNodes2);
    expect(subNodes2.hasNext()).andReturn(false).anyTimes();

    PropertyDefinition propDefSingle = createMock(PropertyDefinition.class);
    expect(propDefSingle.isMultiple()).andReturn(false).anyTimes();

    Property doubleProp = createMock(Property.class);
    expect(doubleProp.getType()).andReturn(PropertyType.DOUBLE).anyTimes();
    Value doubleValue = createMock(Value.class);

    expect(doubleValue.getType()).andReturn(PropertyType.DOUBLE).anyTimes();
    expect(doubleValue.getDouble()).andReturn(Double.valueOf(1.5)).anyTimes();
    expect(doubleProp.getName()).andReturn("doub").anyTimes();
    expect(doubleProp.getDefinition()).andReturn(propDefSingle).anyTimes();
    expect(doubleProp.getValue()).andReturn(doubleValue).anyTimes();

    // Iterator
    PropertyIterator propIter = createMock(PropertyIterator.class);
    expect(propIter.hasNext()).andReturn(true);
    expect(propIter.nextProperty()).andReturn(doubleProp);
    expect(propIter.hasNext()).andReturn(false);

    PropertyIterator subPropIter1 = createMock(PropertyIterator.class);
    expect(subPropIter1.hasNext()).andReturn(true);
    expect(subPropIter1.nextProperty()).andReturn(doubleProp);
    expect(subPropIter1.hasNext()).andReturn(false);

    PropertyIterator subPropIter2 = createMock(PropertyIterator.class);
    expect(subPropIter2.hasNext()).andReturn(true);
    expect(subPropIter2.nextProperty()).andReturn(doubleProp);
    expect(subPropIter2.hasNext()).andReturn(false);

    expect(node.getProperties()).andReturn(propIter).anyTimes();
    expect(subNode1.getProperties()).andReturn(subPropIter1).anyTimes();
    expect(subNode2.getProperties()).andReturn(subPropIter2).anyTimes();

    replay();

    StringWriter writer = new StringWriter();
    ExtendedJSONWriter ext = new ExtendedJSONWriter(writer);

    ExtendedJSONWriter.writeNodeTreeToWriter(ext, node);

    writer.flush();
    String s = writer.toString();
    JSONObject o = new JSONObject(s);
    assertEquals(1.5, o.getDouble("doub"), 0);
    assertEquals("/path/to/node", o.get("jcr:path"));
    assertEquals("node", o.get("jcr:name"));

    JSONObject subObj1 = o.getJSONObject("sub1");
    assertNotNull(subObj1);
    assertEquals("/path/to/node/sub1", subObj1.get("jcr:path"));

    JSONObject subObj2 = subObj1.getJSONObject("sub2");
    assertNotNull(subObj2);
    assertEquals("/path/to/node/sub1/sub2", subObj2.get("jcr:path"));
  }

  @Test
  public void writeNodeTreeToWriterLimitedSubNodes0() throws Exception {
    Node node = createMock(Node.class);
    expect(node.getPath()).andReturn("/path/to/node").anyTimes();
    expect(node.getName()).andReturn("node").anyTimes();

    Node subNode1 = createMock(Node.class);
    expect(subNode1.getPath()).andReturn("/path/to/node/sub1").anyTimes();
    expect(subNode1.getName()).andReturn("sub1").anyTimes();

    Node subNode2 = createMock(Node.class);
    expect(subNode2.getPath()).andReturn("/path/to/node/sub1/sub2").anyTimes();
    expect(subNode2.getName()).andReturn("sub2").anyTimes();

    NodeIterator nodes = createMock(NodeIterator.class);
    expect(node.getNodes()).andReturn(nodes).anyTimes();
    expect(nodes.hasNext()).andReturn(true);
    expect(nodes.nextNode()).andReturn(subNode1);
    expect(nodes.hasNext()).andReturn(false);

    NodeIterator subNodes1 = createMock(NodeIterator.class);
    expect(subNode1.getNodes()).andReturn(subNodes1).anyTimes();
    expect(subNodes1.hasNext()).andReturn(true);
    expect(subNodes1.nextNode()).andReturn(subNode2);
    expect(subNodes1.hasNext()).andReturn(false);

    NodeIterator subNodes2 = createMock(NodeIterator.class);
    expect(subNode2.getNodes()).andReturn(subNodes2).anyTimes();
    expect(subNodes2.hasNext()).andReturn(false).anyTimes();

    PropertyDefinition propDefSingle = createMock(PropertyDefinition.class);
    expect(propDefSingle.isMultiple()).andReturn(false).anyTimes();

    Property doubleProp = createMock(Property.class);
    expect(doubleProp.getType()).andReturn(PropertyType.DOUBLE).anyTimes();
    Value doubleValue = createMock(Value.class);

    expect(doubleValue.getType()).andReturn(PropertyType.DOUBLE).anyTimes();
    expect(doubleValue.getDouble()).andReturn(Double.valueOf(1.5)).anyTimes();
    expect(doubleProp.getName()).andReturn("doub").anyTimes();
    expect(doubleProp.getDefinition()).andReturn(propDefSingle).anyTimes();
    expect(doubleProp.getValue()).andReturn(doubleValue).anyTimes();

    // Iterator
    PropertyIterator propIter = createMock(PropertyIterator.class);
    expect(propIter.hasNext()).andReturn(true);
    expect(propIter.nextProperty()).andReturn(doubleProp);
    expect(propIter.hasNext()).andReturn(false);

    PropertyIterator subPropIter1 = createMock(PropertyIterator.class);
    expect(subPropIter1.hasNext()).andReturn(true);
    expect(subPropIter1.nextProperty()).andReturn(doubleProp);
    expect(subPropIter1.hasNext()).andReturn(false);

    PropertyIterator subPropIter2 = createMock(PropertyIterator.class);
    expect(subPropIter2.hasNext()).andReturn(true);
    expect(subPropIter2.nextProperty()).andReturn(doubleProp);
    expect(subPropIter2.hasNext()).andReturn(false);

    expect(node.getProperties()).andReturn(propIter).anyTimes();
    expect(subNode1.getProperties()).andReturn(subPropIter1).anyTimes();
    expect(subNode2.getProperties()).andReturn(subPropIter2).anyTimes();

    replay();

    StringWriter writer = new StringWriter();
    ExtendedJSONWriter ext = new ExtendedJSONWriter(writer);

    ExtendedJSONWriter.writeNodeTreeToWriter(ext, node, false, 0);

    writer.flush();
    String s = writer.toString();
    JSONObject o = new JSONObject(s);
    assertEquals(1.5, o.getDouble("doub"), 0);
    assertEquals("/path/to/node", o.get("jcr:path"));
    assertEquals("node", o.get("jcr:name"));

    assertFalse(o.has("sub1"));
  }

  @Test
  public void writeNodeTreeToWriterLimitedSubNodes1() throws Exception {
    Node node = createMock(Node.class);
    expect(node.getPath()).andReturn("/path/to/node").anyTimes();
    expect(node.getName()).andReturn("node").anyTimes();

    Node subNode1 = createMock(Node.class);
    expect(subNode1.getPath()).andReturn("/path/to/node/sub1").anyTimes();
    expect(subNode1.getName()).andReturn("sub1").anyTimes();

    Node subNode2 = createMock(Node.class);
    expect(subNode2.getPath()).andReturn("/path/to/node/sub1/sub2").anyTimes();
    expect(subNode2.getName()).andReturn("sub2").anyTimes();

    NodeIterator nodes = createMock(NodeIterator.class);
    expect(node.getNodes()).andReturn(nodes).anyTimes();
    expect(nodes.hasNext()).andReturn(true);
    expect(nodes.nextNode()).andReturn(subNode1);
    expect(nodes.hasNext()).andReturn(false);

    NodeIterator subNodes1 = createMock(NodeIterator.class);
    expect(subNode1.getNodes()).andReturn(subNodes1).anyTimes();
    expect(subNodes1.hasNext()).andReturn(true);
    expect(subNodes1.nextNode()).andReturn(subNode2);
    expect(subNodes1.hasNext()).andReturn(false);

    NodeIterator subNodes2 = createMock(NodeIterator.class);
    expect(subNode2.getNodes()).andReturn(subNodes2).anyTimes();
    expect(subNodes2.hasNext()).andReturn(false).anyTimes();

    PropertyDefinition propDefSingle = createMock(PropertyDefinition.class);
    expect(propDefSingle.isMultiple()).andReturn(false).anyTimes();

    Property doubleProp = createMock(Property.class);
    expect(doubleProp.getType()).andReturn(PropertyType.DOUBLE).anyTimes();
    Value doubleValue = createMock(Value.class);

    expect(doubleValue.getType()).andReturn(PropertyType.DOUBLE).anyTimes();
    expect(doubleValue.getDouble()).andReturn(Double.valueOf(1.5)).anyTimes();
    expect(doubleProp.getName()).andReturn("doub").anyTimes();
    expect(doubleProp.getDefinition()).andReturn(propDefSingle).anyTimes();
    expect(doubleProp.getValue()).andReturn(doubleValue).anyTimes();

    // Iterator
    PropertyIterator propIter = createMock(PropertyIterator.class);
    expect(propIter.hasNext()).andReturn(true);
    expect(propIter.nextProperty()).andReturn(doubleProp);
    expect(propIter.hasNext()).andReturn(false);

    PropertyIterator subPropIter1 = createMock(PropertyIterator.class);
    expect(subPropIter1.hasNext()).andReturn(true);
    expect(subPropIter1.nextProperty()).andReturn(doubleProp);
    expect(subPropIter1.hasNext()).andReturn(false);

    PropertyIterator subPropIter2 = createMock(PropertyIterator.class);
    expect(subPropIter2.hasNext()).andReturn(true);
    expect(subPropIter2.nextProperty()).andReturn(doubleProp);
    expect(subPropIter2.hasNext()).andReturn(false);

    expect(node.getProperties()).andReturn(propIter).anyTimes();
    expect(subNode1.getProperties()).andReturn(subPropIter1).anyTimes();
    expect(subNode2.getProperties()).andReturn(subPropIter2).anyTimes();

    replay();

    StringWriter writer = new StringWriter();
    ExtendedJSONWriter ext = new ExtendedJSONWriter(writer);

    ExtendedJSONWriter.writeNodeTreeToWriter(ext, node, false, 1);

    writer.flush();
    String s = writer.toString();
    JSONObject o = new JSONObject(s);
    assertEquals(1.5, o.getDouble("doub"), 0);
    assertEquals("/path/to/node", o.get("jcr:path"));
    assertEquals("node", o.get("jcr:name"));

    JSONObject subObj1 = o.getJSONObject("sub1");
    assertNotNull(subObj1);
    assertEquals("/path/to/node/sub1", subObj1.get("jcr:path"));

    assertFalse(subObj1.has("sub2"));
  }

  @Test
  public void writeNodeTreeToWriterLimitedSubNodes2() throws Exception {
    Node node = createMock(Node.class);
    expect(node.getPath()).andReturn("/path/to/node").anyTimes();
    expect(node.getName()).andReturn("node").anyTimes();

    Node subNode1 = createMock(Node.class);
    expect(subNode1.getPath()).andReturn("/path/to/node/sub1").anyTimes();
    expect(subNode1.getName()).andReturn("sub1").anyTimes();

    Node subNode2 = createMock(Node.class);
    expect(subNode2.getPath()).andReturn("/path/to/node/sub1/sub2").anyTimes();
    expect(subNode2.getName()).andReturn("sub2").anyTimes();

    NodeIterator nodes = createMock(NodeIterator.class);
    expect(node.getNodes()).andReturn(nodes).anyTimes();
    expect(nodes.hasNext()).andReturn(true);
    expect(nodes.nextNode()).andReturn(subNode1);
    expect(nodes.hasNext()).andReturn(false);

    NodeIterator subNodes1 = createMock(NodeIterator.class);
    expect(subNode1.getNodes()).andReturn(subNodes1).anyTimes();
    expect(subNodes1.hasNext()).andReturn(true);
    expect(subNodes1.nextNode()).andReturn(subNode2);
    expect(subNodes1.hasNext()).andReturn(false);

    NodeIterator subNodes2 = createMock(NodeIterator.class);
    expect(subNode2.getNodes()).andReturn(subNodes2).anyTimes();
    expect(subNodes2.hasNext()).andReturn(false).anyTimes();

    PropertyDefinition propDefSingle = createMock(PropertyDefinition.class);
    expect(propDefSingle.isMultiple()).andReturn(false).anyTimes();

    Property doubleProp = createMock(Property.class);
    expect(doubleProp.getType()).andReturn(PropertyType.DOUBLE).anyTimes();
    Value doubleValue = createMock(Value.class);

    expect(doubleValue.getType()).andReturn(PropertyType.DOUBLE).anyTimes();
    expect(doubleValue.getDouble()).andReturn(Double.valueOf(1.5)).anyTimes();
    expect(doubleProp.getName()).andReturn("doub").anyTimes();
    expect(doubleProp.getDefinition()).andReturn(propDefSingle).anyTimes();
    expect(doubleProp.getValue()).andReturn(doubleValue).anyTimes();

    // Iterator
    PropertyIterator propIter = createMock(PropertyIterator.class);
    expect(propIter.hasNext()).andReturn(true);
    expect(propIter.nextProperty()).andReturn(doubleProp);
    expect(propIter.hasNext()).andReturn(false);

    PropertyIterator subPropIter1 = createMock(PropertyIterator.class);
    expect(subPropIter1.hasNext()).andReturn(true);
    expect(subPropIter1.nextProperty()).andReturn(doubleProp);
    expect(subPropIter1.hasNext()).andReturn(false);

    PropertyIterator subPropIter2 = createMock(PropertyIterator.class);
    expect(subPropIter2.hasNext()).andReturn(true);
    expect(subPropIter2.nextProperty()).andReturn(doubleProp);
    expect(subPropIter2.hasNext()).andReturn(false);

    expect(node.getProperties()).andReturn(propIter).anyTimes();
    expect(subNode1.getProperties()).andReturn(subPropIter1).anyTimes();
    expect(subNode2.getProperties()).andReturn(subPropIter2).anyTimes();

    replay();

    StringWriter writer = new StringWriter();
    ExtendedJSONWriter ext = new ExtendedJSONWriter(writer);

    ExtendedJSONWriter.writeNodeTreeToWriter(ext, node, false, 2);

    writer.flush();
    String s = writer.toString();
    JSONObject o = new JSONObject(s);
    assertEquals(1.5, o.getDouble("doub"), 0);
    assertEquals("/path/to/node", o.get("jcr:path"));
    assertEquals("node", o.get("jcr:name"));

    JSONObject subObj1 = o.getJSONObject("sub1");
    assertNotNull(subObj1);
    assertEquals("/path/to/node/sub1", subObj1.get("jcr:path"));

    JSONObject subObj2 = subObj1.getJSONObject("sub2");
    assertNotNull(subObj2);
    assertEquals("/path/to/node/sub1/sub2", subObj2.get("jcr:path"));
  }

  /*
   * Helper methods for mocking.
   */

  protected <T> T createMock(Class<T> c) {
    T result = org.easymock.EasyMock.createMock(c);
    mocks.add(result);
    return result;
  }

  protected void replay() {
    org.easymock.EasyMock.replay(mocks.toArray());
  }

}
