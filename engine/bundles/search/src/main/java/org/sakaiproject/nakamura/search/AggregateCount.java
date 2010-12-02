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

import org.sakaiproject.nakamura.api.search.Aggregator;
import org.sakaiproject.nakamura.util.JcrUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * Generates an aggregate count of a set of properties from a set of nodes, presented via
 * add. Optionally it will accumulate for child nodes of the node in question.
 * 
 * The result is stored in a Map of Maps containing counts. The key to the map of counts
 * is the property name, and the key to the count is the value of the property.
 */
public class AggregateCount implements Aggregator {

  /**
   * The storage map for the aggregate.
   */
  private Map<String, Map<String, Integer>> agregateMap = new HashMap<String, Map<String, Integer>>();
  /**
   * The list of property names to aggregate.
   */
  private String[] fields;
  /**
   * If true, child nodes will be aggregated.
   */
  private boolean children = false;
  /**
   * A set of node paths that have already been inspected.
   */
  private Set<String> checked;

  /**
   * Create an aggregate count over a set of fields, optionally collecting all child properties.
   * @param fields the set of fields to collect.
   * @param children if true child nodes will be considered.
   */
  public AggregateCount(String[] fields, boolean children) {
    for (String f : fields) {
      agregateMap.put(f, new HashMap<String, Integer>());
    }
    this.checked = new HashSet<String>();
    
    this.fields = new String[fields.length];
    System.arraycopy(fields, 0, this.fields, 0, fields.length);
    this.children = children;
  }

  /**
   * Add a node to the aggregated set. 
   * @param node the node to add.
   * @throws RepositoryException
   */
  public void add(Node node) throws RepositoryException {
    if (checked.contains(node.getPath())) {
      return;
    }
    for (String f : fields) {
      Map<String, Integer> tagResult = agregateMap.get(f);
      if (node.hasProperty(f)) {
        Value[] tags = JcrUtils.getValues(node, f);
        for (Value t : tags) {
          String tag = t.getString();
          if (tagResult.containsKey(tag)) {
            tagResult.put(tag, tagResult.get(tag) + 1);
          } else {
            tagResult.put(tag, 1);
          }
        }
      }
    }
    checked.add(node.getPath());
    if (children) {
      for (NodeIterator ni = node.getNodes(); ni.hasNext();) {
        add(ni.nextNode());
      }
    }
  }

  /**
   * @return the aggregated set.
   */
  public Map<String, Map<String, Integer>> getAggregate() {
    return agregateMap;
  }

}
