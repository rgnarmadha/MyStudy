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

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.jcr.JsonItemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;

import javax.jcr.Binary;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;

/**
 * Utilities to make simple JCR operations easier and avoid duplication.
 */
public class JcrUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(JcrUtils.class);

  /**
   * Deep creates a path.
   *
   * @param session
   * @param path
   * @param nodeType
   * @return
   * @throws RepositoryException
   */
  public static Node deepGetOrCreateNode(Session session, String path, String nodeType)
      throws RepositoryException {
    if (path == null || !path.startsWith("/")) {
      throw new IllegalArgumentException("path must be an absolute path.");
    }
    if (!"/".equals(path) && path.endsWith("/")) { // strip trailing slash
      path = path.substring(0, path.lastIndexOf("/"));
    }
    // get the starting node
    String startingNodePath = path;
    Node startingNode = null;
    while (startingNode == null) {
      if (startingNodePath.equals("/")) {
        startingNode = session.getRootNode();
      } else if (session.itemExists(startingNodePath)) {
        startingNode = (Node) session.getItem(startingNodePath);
      } else {
        int pos = startingNodePath.lastIndexOf('/');
        if (pos > 0) {
          startingNodePath = startingNodePath.substring(0, pos);
        } else {
          startingNodePath = "/";
        }
      }
    }
    // is the searched node already existing?
    if (startingNodePath.length() == path.length()) {
      return startingNode;
    }
    // create nodes
    int from = (startingNodePath.length() == 1 ? 1 : startingNodePath.length() + 1);
    Node node = startingNode;
    while (from > 0) {
      final int to = path.indexOf('/', from);
      final String name = to < 0 ? path.substring(from) : path.substring(from, to);
      boolean leafNode = to < 0;
      // although the node should not exist (according to the first test
      // above)
      // we do a sanety check.
      if (node.hasNode(name)) {
        node = node.getNode(name);
      } else {
        if (leafNode && nodeType != null) {
          node = node.addNode(name, nodeType);
        } else {
          node = node.addNode(name);
        }
      }
      from = to + 1;
    }
    return node;
  }

  /**
   * Deep creates a path.
   *
   * @param session
   * @param path
   * @return
   * @throws RepositoryException
   */
  public static Node deepGetOrCreateNode(Session session, String path)
      throws RepositoryException {
    return deepGetOrCreateNode(session, path, null);
  }

  /**
   * @throws RepositoryException
   *
   */
  public static Node getFirstExistingNode(Session session, String absRealPath)
      throws RepositoryException {
    Item item = null;
    try {
      item = session.getItem(absRealPath);
    } catch (PathNotFoundException ex) {
    } catch (RepositoryException re) {
      LOGGER.warn(re.getMessage());
    }
    String parentPath = absRealPath;
    while (item == null && !"/".equals(parentPath)) {
      parentPath = PathUtils.getParentReference(parentPath);
      try {
        item = session.getItem(parentPath);
      } catch (PathNotFoundException ex) {
      } catch (RepositoryException re) {
        LOGGER.warn(re.getMessage());
      }
    }
    if (item == null) {
      return null;
    }
    // convert first item to a node.
    if (!item.isNode()) {
      item = item.getParent();
    }

    return (Node) item;
  }

  /**
   * @param logger
   * @param node
   * @throws JSONException
   * @throws RepositoryException
   */
  public static void logItem(Logger logger, Node node) throws RepositoryException,
      JSONException {
    StringWriter sw = new StringWriter();
    JsonItemWriter dumpWriter = new JsonItemWriter(new HashSet<String>());
    dumpWriter.dump(node, sw, 5, true);
    logger.info("Node is {} ", sw.toString());
  }

  /**
   * @param node
   * @param propertyName
   * @return
   * @throws RepositoryException
   */
  public static Value[] getValues(Node node, String propertyName)
      throws RepositoryException {
    if (node.hasProperty(propertyName)) {
      Property property = node.getProperty(propertyName);
      PropertyDefinition pd = property.getDefinition();
      if (pd.isMultiple()) {
        return property.getValues();
      } else {
        return new Value[] { property.getValue() };
      }
    }
    return new Value[] {};
  }

  public static String getMultiValueString(Property property) throws RepositoryException {
    PropertyDefinition pd = property.getDefinition();
    if (pd.isMultiple()) {
      StringBuilder sb = new StringBuilder();
      for (Value v : property.getValues()) {
        sb.append(v.getString());
      }
      return sb.toString();
    } else {
      return property.getValue().getString();
    }
  }

  public static NodeInputStream getInputStreamForNode(Node node) {
    try {
      Node content = node;
      if (node.isNodeType("nt:file")
          || (node.hasProperty("jcr:frozenPrimaryType") && "nt:file".equals(node
              .getProperty("jcr:frozenPrimaryType").getString()))) {
        content = node.getNode("jcr:content");
      }
      Property data;

      if (content.hasProperty("jcr:data")) {
        data = content.getProperty("jcr:data");
      } else {
        try {
          Item item = content.getPrimaryItem();
          while (item.isNode()) {
            item = ((Node) item).getPrimaryItem();
          }
          data = ((Property) item);
        } catch (ItemNotFoundException infe) {
          data = null;
        }
      }

      if (data != null) {
        long length = data.getLength();
        InputStream stream = data.getBinary().getStream();
        return new NodeInputStream(node, stream, length);
      }
    } catch (RepositoryException re) {
      LOGGER.error("getInputStream: Cannot get InputStream for " + node, re);
    }
    return null;
  }

  /**
   * Add a value to a property. This property will always be multi-valued.
   *
   * @param session
   *          The session to use.
   * @param node
   *          The node to perform the operation on.
   * @param propertyName
   *          The name of the property to add a value to.
   * @param value
   *          Add a value to the property
   * @param type
   *          The type defined in {@link PropertyType}.
   * @throws RepositoryException
   */
  public static void addValue(Session session, Node node, String propertyName,
      String value, int type) throws RepositoryException {
    Value[] oldVals = getValues(node, propertyName);
    Value[] newVals = new Value[oldVals.length + 1];
    for (int i = 0; i < oldVals.length; i++) {
      Value v = oldVals[i];
      newVals[i] = v;
    }

    // Create a value that points to the tag node.
    // We create a soft reference (aka set uuid of tag node on file)
    newVals[oldVals.length] = session.getValueFactory().createValue(value, type);
    node.setProperty(propertyName, newVals);
  }

  /**
   * Add a value on a multi-valued property. Only when it's not already in there.
   *
   * @param session
   *          The session to create the {@link Value value}.
   * @param node
   *          The {@link Node node} to set the property on.
   * @param propertyName
   *          The name of the property.
   * @param value
   *          The actual value that will be stored on the property.
   * @param type
   *          The {@link PropertyType type} that this value has.
   * @throws RepositoryException
   *           Something went wrong.
   */
  public static void addUniqueValue(Session session, Node node, String propertyName,
      String value, int type) throws RepositoryException {
    boolean containsValue = false;
    Value[] oldVals = getValues(node, propertyName);
    Value[] newVals = new Value[oldVals.length + 1];
    for (int i = 0; i < oldVals.length; i++) {
      Value v = oldVals[i];
      containsValue = v.getString().equals(value);
      if (containsValue) {
        // This value is already on the property, no need to loop over the rest.
        return;
      }
      newVals[i] = v;
    }

    // Create a value.
    newVals[oldVals.length] = session.getValueFactory().createValue(value, type);
    // store it on property.
    node.setProperty(propertyName, newVals);
  }

  /**
   * Checks if a node has a specified mixin.
   *
   * @param node
   *          The node in question
   * @param mixin
   *          The name of the mixin to look for.
   */
  public static boolean hasMixin(Node node, String mixin) throws RepositoryException {
    NodeType[] nodetypes = node.getMixinNodeTypes();
    boolean hasRequiredMixin = false;
    for (NodeType type : nodetypes) {
      if (type.getName().equals(mixin)) {
        hasRequiredMixin = true;
        break;
      }
    }
    return hasRequiredMixin;
  }

  /**
   * The following Objects will be returned into a {@link Value value} or an array of
   * Values. The passed in object can be an array.
   * <ul>
   * <li>{@link BigDecimal BigDecimal}</li>
   * <li>{@link Binary Binary}</li>
   * <li>{@link Boolean Boolean}</li>
   * <li>{@link Calendar Calendar}</li>
   * <li>{@link Double Double}</li>
   * <li>{@link Long Long}</li>
   * <li>{@link String String}</li>
   * <li>{@link JSONArray JSONArray}</li>
   * </ul>
   * If the object could not be changed to a Value object, this method will return null.
   *
   * @param o
   *          The value (or array of) that needs to be created. {@link Node Nodes} are not
   *          supported.
   * @param session
   *          Needed to create the value.
   * @return A {@link Value value} or null.
   * @throws RepositoryException
   */
  public static Object createValue(Object o, Session session) throws RepositoryException {
    ValueFactory vf = session.getValueFactory();
    Value val = null;

    // Handle the arrays first.
    if (o.getClass().isArray()) {
      Object[] arr = (Object[]) o;
      int l = arr.length;
      Value[] vals = new Value[l];
      for (int i = 0; i < l; i++) {
        vals[i] = (Value) createValue(arr[i], session);
      }
      return vals;
    }
    if (o instanceof JSONArray) {
      JSONArray arr = (JSONArray) o;
      int l = arr.length();
      Value[] vals = new Value[l];
      for (int i = 0; i < l; i++) {
        Object arrValue = arr.opt(i);
        if (arrValue instanceof JSONArray) {
          throw new IllegalArgumentException(
              "The JSONArray cannot contain another array.");
        }
        vals[i] = (Value) createValue(arrValue, session);
      }
      return vals;

    }

    // Single values.
    else if (o instanceof BigDecimal) {
      val = vf.createValue((BigDecimal) o);
    } else if (o instanceof Binary) {
      val = vf.createValue((Binary) o);
    } else if (o instanceof Boolean) {
      val = vf.createValue((Boolean) o);
    } else if (o instanceof Calendar) {
      val = vf.createValue((Calendar) o);
    } else if (o instanceof Double) {
      val = vf.createValue((Double) o);
    } else if (o instanceof Long) {
      val = vf.createValue((Long) o);
    } else if (o instanceof String) {
      val = vf.createValue(o.toString());
    }

    return val;

  }

  /**
   * Delete a value on a multi-valued property.
   *
   * @param session
   *          The session to create the {@link Value value}.
   * @param node
   *          The {@link Node node} to set the property on.
   * @param propertyName
   *          The name of the property.
   * @param propertyValue
   *          The actual value that will be removed from the property.
   * @throws RepositoryException
   *           Something went wrong.
   */
  public static void deleteValue(Session session, Node node, String propertyName,
      String propertyValue) throws RepositoryException {
    if (session == null || node == null || propertyName == null || propertyValue == null) {
      return;
    }

    Value[] values = getValues(node, propertyName);
    ArrayList<Value> valueList = new ArrayList<Value>();
    for (Value value : values) {
      if (!propertyValue.equals(value.getString())) {
        valueList.add(value);
      }
    }

    // only set the values if we collected fewer than are already set
    if (valueList.size() < values.length) {
      node.setProperty(propertyName, valueList.toArray(new Value[0]));
    }
  }
}
