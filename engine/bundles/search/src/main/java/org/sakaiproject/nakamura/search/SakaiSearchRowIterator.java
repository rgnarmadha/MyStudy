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

import static org.sakaiproject.nakamura.api.search.SearchConstants.SAKAI_EXCLUDE_TREE;

import org.sakaiproject.nakamura.api.search.ValidatingRowIterator;

import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * This class wraps a {@link RowIterator}. When you request the nextRow it will check if
 * it can return the next {@link Row}. It checks if a {@link Row} that represents a
 * {@link Node} is in a blacklisted tree. If the {@link Node} is in a blacklisted tree, it
 * will skip it and fetch the next one. This iterator always lazy loads the next row.
 */
public final class SakaiSearchRowIterator extends ValidatingRowIterator {

  private String[] blacklistedPaths;

  /**
   * @param iterator
   *          The iterator that should be wrapped.
   */
  SakaiSearchRowIterator(RowIterator iterator) {
    super(iterator);
    loadNextRow();
  }

  /**
   *
   * @param iterator
   *          The iterator that should be wrapped.
   * @param blacklistedPaths
   *          An array of paths that should be ignored.
   */
  SakaiSearchRowIterator(RowIterator iterator, String[] blacklistedPaths) {
    super(iterator);
    if (blacklistedPaths != null) {
      Arrays.sort(blacklistedPaths);
      this.blacklistedPaths = blacklistedPaths;
    }
    loadNextRow();
  }

  /**
   * Checks if this node should be included in the search results.
   *
   * @param node
   * @return Wether or not the node should be included in the search results.
   */
  @Override
  protected boolean isValid(Node node) {
    try {
      String path = node.getPath();
      if ("/".equals(path)) {
        return true;
      } else if (node.hasProperty(SAKAI_EXCLUDE_TREE)) {
        return !node.getProperty(SAKAI_EXCLUDE_TREE).getBoolean();
      } else {
        if (blacklistedPaths != null) {
          for (String blackPath : blacklistedPaths) {
            if (path.startsWith(blackPath)) {
              return false;
            }
          }
        }
        return isValid(node.getParent());
      }
    } catch (RepositoryException e) {
      return false;
    }
  }

}
