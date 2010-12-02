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
package org.sakaiproject.nakamura.batch;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.JSONString;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.sakaiproject.nakamura.util.JcrUtils;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

@Component(immediate = true)
@Service
@Deprecated
public class CreateTreeOperation extends AbstractSlingPostOperation {

  private static final long serialVersionUID = 9207596135556346980L;
  public static final String TREE_PARAM = "tree";
  public static final String DELETE_PARAM = "delete";

  @Property(value = "createTree")
  static final String SLING_POST_OPERATION = "sling.post.operation";

  @Deprecated
  @Override
  protected void doRun(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {
    // Check parameters
    RequestParameter treeParam = request.getRequestParameter(TREE_PARAM);
    RequestParameter deleteParam = request.getRequestParameter(DELETE_PARAM);
    Session session = request.getResourceResolver().adaptTo(Session.class);
    JSONObject json = null;
    Node node = null;

    // If there is no tree parameter we ignore the entire thing
    if (treeParam == null) {
      throw new RepositoryException("No " + TREE_PARAM + " parameter found.");
    }

    // Get the json object.
    try {
      json = new JSONObject(treeParam.getString());
    } catch (JSONException e) {
      throw new RepositoryException("Invalid JSON tree structure");
    }

    // Get node
    String path = request.getResource().getPath();
    node = JcrUtils.deepGetOrCreateNode(session, path);

    // Check if we want to delete the entire tree before creating a new node.
    if (deleteParam != null && !node.isNew() && "1".equals(deleteParam.getString())) {
      // Remove the node (we could get all the child nodes and remove the tree)
      node.remove();
      // Save the session so the node is actually removed
      session.save();
      // Create a brand new one.
      node = JcrUtils.deepGetOrCreateNode(session, path);
    }

    // Start creating the tree.
    createTree(session, json, node);

    // Save the session.
    if (session.hasPendingChanges()) {
      session.save();
    }
  }

  private void createTree(Session session, JSONObject json, Node node)
      throws ItemExistsException, PathNotFoundException, VersionException,
      ConstraintViolationException, LockException, RepositoryException {

    try {
      Iterator<String> keys = json.keys();
      while (keys.hasNext()) {

        String key = keys.next();
        if (!key.startsWith("jcr:")) {
          Object obj = json.get(key);

          if (obj instanceof JSONObject) {
            // This represents a child node.
            Node childNode = addNode(node, key);
            createTree(session, (JSONObject) obj, childNode);
          } else if (obj instanceof JSONArray) {
            // This represents a multivalued property

            JSONArray arr = (JSONArray) obj;
            Value[] values = new Value[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
              values[i] = getValue(arr.get(i), session);
            }
            node.setProperty(key, values);

          } else {
            node.setProperty(key, getValue(obj, session));
          }
        }
      }
    } catch (JSONException e) {
      // TODO
    }
  }

  /**
   * Get the {@link Value JCR Value} for an object. If none is found, it will default o a
   * string value.
   *
   * @param obj
   * @param session
   * @return
   * @throws RepositoryException
   */
  protected Value getValue(Object obj, Session session)
      throws RepositoryException {
    Value value = null;
    ValueFactory vf = session.getValueFactory();
    if (obj instanceof JSONString) {
      value = vf.createValue(((JSONString) obj).toJSONString());
    } else if (obj instanceof String) {
      value = vf.createValue(obj.toString());
    } else if (obj instanceof BigDecimal) {
      value = vf.createValue((BigDecimal) obj);
    } else if (obj instanceof Boolean) {
      value = vf.createValue(Boolean.valueOf(obj.toString()));
    } else if (obj instanceof Double) {
      value = vf.createValue((Double) obj);
    } else if (obj instanceof Long) {
      value = vf.createValue((Long) obj);
    } else if (obj instanceof Integer) {
      value = vf.createValue((Integer) obj);
    } else if (obj instanceof Calendar) {
      value = vf.createValue((Calendar) obj);
    } else {
      value = vf.createValue(obj.toString());
    }
    return value;
  }

  protected Node addNode(Node node, String key) throws ItemExistsException,
      PathNotFoundException, VersionException, ConstraintViolationException,
      LockException, RepositoryException {
    if (node.hasNode(key)) {
      return node.getNode(key);
    }

    return node.addNode(key);
  }

}
