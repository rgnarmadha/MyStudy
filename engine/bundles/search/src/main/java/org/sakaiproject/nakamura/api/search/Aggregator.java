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
package org.sakaiproject.nakamura.api.search;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Aggregates results from nodes in to a frequency map of terms.
 */
public interface Aggregator {

  /**
   * Add a node and count the configured properties adding them to the aggregated map.
   * 
   * @param node
   *          the node to inspect.
   * @throws RepositoryException
   */
  void add(Node node) throws RepositoryException;

  /**
   * @return the aggregate map containing a frequency count for each term. The outer map
   *         is keyed by the property name, and the inner map is keyed by the value of
   *         terms. The value being the frequency.
   */
  Map<String, Map<String, Integer>> getAggregate();

}
