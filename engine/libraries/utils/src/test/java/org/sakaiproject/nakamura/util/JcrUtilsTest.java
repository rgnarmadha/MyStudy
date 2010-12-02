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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.sling.commons.json.JSONException;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * 
 */
public class JcrUtilsTest {

  @Test
  public void testDeepCreate() throws RepositoryException {
    Session session = createMock(Session.class);
    Node nodeB = createMock(Node.class);
    Node nodeC = createMock(Node.class);
    Node nodeD = createMock(Node.class);
    expect(session.itemExists("/x/a/b/c/d")).andReturn(false);
    expect(session.itemExists("/x/a/b/c")).andReturn(false);
    expect(session.itemExists("/x/a/b")).andReturn(true);
    expect(session.getItem("/x/a/b")).andReturn(nodeB);
    expect(nodeB.hasNode("c")).andReturn(false);
    expect(nodeB.addNode("c")).andReturn(nodeC);
    expect(nodeC.hasNode("d")).andReturn(false);
    expect(nodeC.addNode("d")).andReturn(nodeD);

    replay(session, nodeB, nodeC, nodeD);
    JcrUtils.deepGetOrCreateNode(session, "/x/a/b/c/d");
    verify(session, nodeB, nodeC, nodeD);
  }

  @Test
  public void testGetFirstExistingNode() throws RepositoryException {
    Session session = createMock(Session.class);
    Node node = createMock(Node.class);
    expect(session.getItem("/a/b/exists/doesnta/doesntb")).andThrow(
        new PathNotFoundException());
    expect(session.getItem("/a/b/exists/doesnta")).andThrow(new PathNotFoundException());
    expect(session.getItem("/a/b/exists")).andReturn(node);
    expect(node.isNode()).andReturn(true);
    replay(session, node);
    assertEquals(node, JcrUtils.getFirstExistingNode(session,
        "/a/b/exists/doesnta/doesntb"));
    verify(session, node);
  }

  @Test
  public void testGetFirstExistingNodeProperty() throws RepositoryException {
    Session session = createMock(Session.class);
    Node node = createMock(Node.class);
    Property property = createMock(Property.class);
    expect(session.getItem("/a/b/sakai:exists/doesnta/doesntb")).andThrow(
        new PathNotFoundException());
    expect(session.getItem("/a/b/sakai:exists/doesnta")).andThrow(
        new PathNotFoundException());
    expect(session.getItem("/a/b/sakai:exists")).andReturn(property);
    expect(property.isNode()).andReturn(false);
    expect(property.getParent()).andReturn(node);
    replay(session, node, property);
    assertEquals(node, JcrUtils.getFirstExistingNode(session,
        "/a/b/sakai:exists/doesnta/doesntb"));
    verify(session, node, property);
  }

  @Test
  public void testGetFirstExistingNodeNull() throws RepositoryException {
    Session session = createMock(Session.class);
    Node node = createMock(Node.class);
    expect(session.getItem("/doesnta/doesntb")).andThrow(new PathNotFoundException());
    expect(session.getItem("/doesnta")).andThrow(new PathNotFoundException());
    expect(session.getItem("/")).andThrow(new PathNotFoundException());
    replay(session, node);
    assertNull(JcrUtils.getFirstExistingNode(session, "/doesnta/doesntb"));
    verify(session, node);
  }

  @Test
  public void testGetInputStreamForNode() throws RepositoryException {
    Node node = createMock(Node.class);
    Node jcrContentNode = createMock(Node.class);
    Property property = createMock(Property.class);
    expect(node.isNodeType("nt:file")).andReturn(true);
    expect(node.getNode("jcr:content")).andReturn(jcrContentNode);
    expect(jcrContentNode.hasProperty("jcr:data")).andReturn(true);
    expect(jcrContentNode.getProperty("jcr:data")).andReturn(property);
    expect(property.getLength()).andReturn(100L);
    byte[] buf = new byte[100];
    ByteArrayInputStream bais = new ByteArrayInputStream(buf);
    Binary binary = createMock(Binary.class);
    expect(property.getBinary()).andReturn(binary);
    expect(binary.getStream()).andReturn(bais);

    replay(node, jcrContentNode, property, binary);
    assertNotNull(JcrUtils.getInputStreamForNode(node));
    verify(node, jcrContentNode, property, binary);
  }

  @Test
  public void testGetInputStreamForNodeNonStandard() throws RepositoryException {
    Node node = createMock(Node.class);
    Node jcrContentNode = createMock(Node.class);
    Property property = createMock(Property.class);
    expect(node.isNodeType("nt:file")).andReturn(false);
    expect(node.hasProperty("jcr:data")).andReturn(false);
    expect(node.hasProperty("jcr:frozenPrimaryType")).andReturn(false);
    expect(node.getPrimaryItem()).andReturn(jcrContentNode);
    expect(jcrContentNode.isNode()).andReturn(true);
    expect(jcrContentNode.getPrimaryItem()).andReturn(property);
    expect(property.isNode()).andReturn(false);
    expect(property.getLength()).andReturn(100L);
    byte[] buf = new byte[100];
    ByteArrayInputStream bais = new ByteArrayInputStream(buf);
    Binary binary = createMock(Binary.class);
    expect(property.getBinary()).andReturn(binary);
    expect(binary.getStream()).andReturn(bais);

    replay(node, jcrContentNode, property, binary);
    assertNotNull(JcrUtils.getInputStreamForNode(node));
    verify(node, jcrContentNode, property, binary);
  }

  @Test
  public void testGetMultiValueStringMulti() throws RepositoryException {
    Property property = createMock(Property.class);
    PropertyDefinition propertyDefinition = createMock(PropertyDefinition.class);
    Value value = createMock(Value.class);
    Value[] values = new Value[] { value, value };

    expect(property.getDefinition()).andReturn(propertyDefinition);
    expect(propertyDefinition.isMultiple()).andReturn(true);
    expect(property.getValues()).andReturn(values);
    expect(value.getString()).andReturn("Value").times(2);
    replay(property, propertyDefinition, value);

    assertEquals("ValueValue", JcrUtils.getMultiValueString(property));
    verify(property, propertyDefinition, value);
  }

  @Test
  public void testGetMultiValueStringSingle() throws RepositoryException {
    Property property = createMock(Property.class);
    PropertyDefinition propertyDefinition = createMock(PropertyDefinition.class);
    Value value = createMock(Value.class);

    expect(property.getDefinition()).andReturn(propertyDefinition);
    expect(propertyDefinition.isMultiple()).andReturn(false);
    expect(property.getValue()).andReturn(value);
    expect(value.getString()).andReturn("Value");
    replay(property, propertyDefinition, value);

    assertEquals("Value", JcrUtils.getMultiValueString(property));
    verify(property, propertyDefinition, value);
  }

  @Test
  public void testGetValuesMulti() throws RepositoryException {
    Node node = createMock(Node.class);
    Property property = createMock(Property.class);
    PropertyDefinition propertyDefinition = createMock(PropertyDefinition.class);
    Value value = createMock(Value.class);
    Value[] values = new Value[] { value, value };

    expect(node.hasProperty("testProperty")).andReturn(true);
    expect(node.getProperty("testProperty")).andReturn(property);
    expect(property.getDefinition()).andReturn(propertyDefinition);
    expect(propertyDefinition.isMultiple()).andReturn(true);
    expect(property.getValues()).andReturn(values);

    replay(node, property, propertyDefinition, value);
    assertArrayEquals(values, JcrUtils.getValues(node, "testProperty"));
    verify(node, property, propertyDefinition, value);
  }

  @Test
  public void testGetValuesSingle() throws RepositoryException {
    Node node = createMock(Node.class);
    Property property = createMock(Property.class);
    PropertyDefinition propertyDefinition = createMock(PropertyDefinition.class);
    Value value = createMock(Value.class);

    expect(node.hasProperty("testProperty")).andReturn(true);
    expect(node.getProperty("testProperty")).andReturn(property);
    expect(property.getDefinition()).andReturn(propertyDefinition);
    expect(propertyDefinition.isMultiple()).andReturn(false);
    expect(property.getValue()).andReturn(value);

    replay(node, property, propertyDefinition, value);
    Value[] values = JcrUtils.getValues(node, "testProperty");
    assertEquals(1, values.length);
    assertEquals(value, values[0]);
    verify(node, property, propertyDefinition, value);
  }

  @Test
  public void testGetValuesNone() throws RepositoryException {
    Node node = createMock(Node.class);
    Property property = createMock(Property.class);

    expect(node.hasProperty("testProperty")).andReturn(false);

    replay(node, property);
    Value[] values = JcrUtils.getValues(node, "testProperty");
    assertEquals(0, values.length);
    verify(node, property);
  }

  @Test
  public void testLogItem() throws RepositoryException, JSONException {
    Node node = createMock(Node.class);
    PropertyIterator propertyIterator = createMock(PropertyIterator.class);
    NodeIterator nodeIterator = createMock(NodeIterator.class);
    Property property = createMock(Property.class);
    PropertyDefinition propertyDefinition = createMock(PropertyDefinition.class);
    Value value = createMock(Value.class);

    // first loop over properties
    expect(node.getProperties()).andReturn(propertyIterator);
    expect(propertyIterator.hasNext()).andReturn(true);
    expect(propertyIterator.nextProperty()).andReturn(property);
    expect(property.getName()).andReturn("prop:name").times(2);
    expect(property.getType()).andReturn(PropertyType.STRING);
    expect(property.getDefinition()).andReturn(propertyDefinition);
    expect(propertyDefinition.isMultiple()).andReturn(false);
    expect(property.getValue()).andReturn(value);
    expect(value.getType()).andReturn(PropertyType.STRING);
    expect(value.getString()).andReturn("propvalue");
    expect(propertyIterator.hasNext()).andReturn(false);

    expect(node.getNodes()).andReturn(nodeIterator);
    expect(nodeIterator.hasNext()).andReturn(true);
    // reusing node
    expect(nodeIterator.nextNode()).andReturn(node);
    expect(node.getName()).andReturn("ChildNodeName");
    expect(node.getProperties()).andReturn(propertyIterator);
    expect(propertyIterator.hasNext()).andReturn(false);
    expect(node.getNodes()).andReturn(nodeIterator);
    expect(nodeIterator.hasNext()).andReturn(false);

    expect(nodeIterator.hasNext()).andReturn(false);

    replay(node, propertyIterator, nodeIterator, property, propertyDefinition, value);
    JcrUtils.logItem(LoggerFactory.getLogger(JcrUtilsTest.class), node);
    verify(node, propertyIterator, nodeIterator, property, propertyDefinition, value);
  }

}
