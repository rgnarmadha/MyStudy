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

import java.util.NoSuchElementException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 *
 */
public abstract class ValidatingRowIterator implements RowIterator {
  private RowIterator iterator;
  private long position;
  protected Row nextRow;

  /**
   * @param iterator
   *          The iterator that should be wrapped.
   */
  public ValidatingRowIterator(RowIterator iterator) {
    this.iterator = iterator;
    this.position = -1;
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.jcr.query.RowIterator#nextRow()
   */
  public Row nextRow() {
    if (nextRow == null) {
      throw new NoSuchElementException();
    }
    Row r = nextRow;
    loadNextRow();
    return r;
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.jcr.RangeIterator#getPosition()
   */
  public long getPosition() {
    return position;
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.jcr.RangeIterator#getSize()
   */
  public long getSize() {
    // Without running over the entire result set and filtering out the Rows that should
    // not be included we don't know the exact size.
    return -1;
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.jcr.RangeIterator#skip(long)
   */
  public void skip(long skipNum) {
    while (skipNum > 0) {
      nextRow();
      skipNum--;
      position++;
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    return (nextRow != null);
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
    throw new UnsupportedOperationException("remove");
  }

  /**
   * @param row
   * @return
   */
  protected boolean isValid(Row row) {
    try {
      Node node = row.getNode();
      return isValid(node);
    } catch (RepositoryException e) {
      return false;
    }
  }

  /**
   * Checks if this node should be included in the search results.
   *
   * @param node
   * @return Wether or not the node should be included in the search results.
   */
  protected abstract boolean isValid(Node node);

  /**
   * Loads the next available row.
   */
  protected void loadNextRow() {
    nextRow = null;
    while (iterator.hasNext()) {
      Row row = iterator.nextRow();
      if (isValid(row)) {
        position++;
        nextRow = row;
        break;
      }
    }
  }
}
