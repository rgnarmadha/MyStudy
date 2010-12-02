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

import java.util.NoSuchElementException;

import javax.jcr.RepositoryException;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

public class MergedRowIterator implements RowIterator {

  private RowIterator iteratorA;
  private RowIterator iteratorB;

  private Row rowA = null;
  private Row rowB = null;

  private int pos;

  public MergedRowIterator(RowIterator iteratorA, RowIterator iteratorB) {
    this.iteratorA = iteratorA;
    this.iteratorB = iteratorB;
  }

  public Row nextRow() {
    Row rowA = getRowA();
    Row rowB = getRowB();

    Row r = null;

    if (compare(rowA, rowB)) {
      r = rowA;
      this.rowA = null;
    } else {
      r = rowB;
      this.rowB = null;
    }
    pos++;
    if (r == null) {
      throw new IllegalStateException();
    }
    return r;
  }

  public long getPosition() {
    return pos;
  }

  public long getSize() {
    // We could do something like: iteratorA.getSize() + iteratorB.getSize()
    // But this is terribly slow and should not be attempted.
    return -1;
  }

  public void skip(long skipNum) {
    // TODO Find a better performing mechanism for this.
    while (skipNum > 0) {
      try {
        nextRow();
      } catch (IllegalStateException e) {
        throw new NoSuchElementException();
      }
      skipNum--;
    }
  }

  public boolean hasNext() {
    return iteratorA.hasNext() || iteratorB.hasNext();
  }

  public Object next() {
    return nextRow();
  }

  public void remove() {
    throw new UnsupportedOperationException();
  }

  private long getScore(Row row) {
    try {
      if (row == null) {
        return Long.MIN_VALUE;
      }
      return row.getValue("jcr:score").getLong();
    } catch (RepositoryException e) {
      return 0;
    }
  }

  private boolean compare(Row rowA, Row rowB) {
    long l1 = getScore(rowA);
    long l2 = getScore(rowB);
    return (l1 >= l2);
  }

  private Row getRowA() {
    if (rowA == null && iteratorA.hasNext()) {
      this.rowA = iteratorA.nextRow();
    }

    return rowA;
  }

  private Row getRowB() {
    if (rowB == null && iteratorB.hasNext()) {
      this.rowB = iteratorB.nextRow();
    }

    return rowB;
  }

}
