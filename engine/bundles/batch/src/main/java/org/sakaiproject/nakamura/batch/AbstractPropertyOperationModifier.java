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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.HtmlResponse;
import org.apache.sling.servlets.post.AbstractSlingPostOperation;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

public abstract class AbstractPropertyOperationModifier extends
    AbstractSlingPostOperation {

  public static final Logger log = LoggerFactory
      .getLogger(AbstractPropertyOperationModifier.class);

  @Override
  protected abstract void doRun(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException;

  /**
   * 
   * @param request
   * @param response
   * @param changes
   * @param uriExpander
   * @throws RepositoryException
   */
  @SuppressWarnings("unchecked")
  public void doModify(SlingHttpServletRequest request, HtmlResponse response,
      List<Modification> changes) throws RepositoryException {
    // Get all the resources
    Session session = request.getResourceResolver().adaptTo(Session.class);
    String[] res = getExplodedApplyToPaths(request, session);
    String operation = request.getRequestParameter(":operation").getString();
    Map<String, String[]> params = request.getParameterMap();

    if (res == null) {
      log.info("Changing one item.");
      Resource resource = request.getResource();
      Item item = resource.adaptTo(Item.class);
      modifyProperties(operation, item, params);
      changes.add(Modification.onModified(resource.getPath()));
    } else {
      log.info("Changing multiple operation.");
      for (String path : res) {
        Item item = session.getItem(path);
        if (item != null) {
          modifyProperties(operation, item, params);
          changes.add(Modification.onModified(path));
        }
      }
    }
    if (session.hasPendingChanges()) {
      session.save();
    }
  }

  /**
   * Returns an array of string which holds the JCR path's for the :applyTo request
   * parameter.
   * 
   * @param request
   * @param session
   * @param uriExpander
   * @return
   * @throws RepositoryException
   */
  private String[] getExplodedApplyToPaths(SlingHttpServletRequest request,
      Session session) throws RepositoryException {
    String[] applyTo = request.getParameterValues(SlingPostConstants.RP_APPLY_TO);
    if (applyTo == null) {
      return null;
    }
    return applyTo;
  }

  /**
   * Loops over the properties of a node and modifies all those that are provided in the
   * parameters map.
   * 
   * @param operation
   * @param item
   * @param params
   * @throws RepositoryException
   */
  protected void modifyProperties(String operation, Item item,
      Map<String, String[]> params) throws RepositoryException {
    if (item.isNode()) {
      Node node = (Node) item;
      for (Entry<String, String[]> e : params.entrySet()) {
        String prop = e.getKey();
        // We skip the :operation property.
        if (!prop.equals(":operation") && !prop.equals(":applyTo")) {

          // Get existing values out of JCR.
          List<String> oldValues = new ArrayList<String>();
          if (node.hasProperty(prop)) {
            try {
              Value[] vals = node.getProperty(prop).getValues();
              for (Value v : vals) {
                oldValues.add(v.getString());
              }
            } catch (ValueFormatException vfe) {
              oldValues.add(node.getProperty(prop).getValue().getString());
            }
          }

          // Add or delete the vales.
          for (String s : e.getValue()) {
            if (operation.equals("addProperty") && !oldValues.contains(s)) {
              oldValues.add(s);
            } else if (operation.equals("removeProperty") && oldValues.contains(s)) {
              oldValues.remove(s);
            }
          }

          // Write the properties to the node and save them.
          String[] newValues = new String[oldValues.size()];
          for (int i = 0; i < oldValues.size(); i++) {
            newValues[i] = oldValues.get(i);
          }
          node.setProperty(prop, newValues);
        }
      }
    }
  }
}
