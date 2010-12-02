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
package org.sakaiproject.nakamura.testutils.easymock;

import org.apache.sling.commons.testing.jcr.MockValue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 * Mocks a {@link RowIterator}. Pass in a {@link List} of {@link Node}.
 */
public class MockRowIterator implements RowIterator {

  private Iterator<Node> nodeIterator;
  private Map<String, Row> convertedRows = new HashMap<String, Row>();
  private long pos;
  private List<Node> nodes;

  public MockRowIterator(List<Node> nodes) {
    this.nodeIterator = nodes.iterator();
    this.nodes = nodes;
  }

  private Row nodeToRow(Node node) {
    try {
      if (convertedRows.get(node.getPath()) != null) {
        return convertedRows.get(node.getPath());
      }

      final Node returnNode = node;
      Row row = new Row() {

        public Value[] getValues() throws RepositoryException {
          return null;
        }

        public Value getValue(String propertyName) throws ItemNotFoundException,
            RepositoryException {
          return new MockValue(returnNode.getPath());
        }

        public Node getNode() throws RepositoryException {
          return returnNode;
        }

        public Node getNode(String arg0) throws RepositoryException {
          return returnNode;
        }

        public String getPath() throws RepositoryException {
          return returnNode.getPath();
        }

        public String getPath(String arg0) throws RepositoryException {
          return returnNode.getPath();
        }

        public double getScore() throws RepositoryException {
          // TODO Auto-generated method stub
          return 0;
        }

        public double getScore(String arg0) throws RepositoryException {
          // TODO Auto-generated method stub
          return 0;
        }
      };

      convertedRows.put(node.getPath(), row);
      return row;

    } catch (RepositoryException e) {
      return null;
    }

  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.jcr.query.RowIterator#nextRow()
   */
  public Row nextRow() {
    pos++;
    return nodeToRow(nodeIterator.next());
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.jcr.RangeIterator#getPosition()
   */
  public long getPosition() {
    return pos;
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.jcr.RangeIterator#getSize()
   */
  public long getSize() {
    return nodes.size();
  }

  /**
   * {@inheritDoc}
   * 
   * @see javax.jcr.RangeIterator#skip(long)
   */
  public void skip(long skipNum) {
    while (skipNum > 0) {
      nextRow();
      skipNum++;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return nodeIterator.hasNext();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Iterator#next()
   */
  public Object next() {
    return nextRow();
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    nodeIterator.remove();
  }

}
