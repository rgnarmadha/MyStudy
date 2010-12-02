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

import static org.junit.Assert.fail;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.search.MergedRowIterator;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 *
 */
public class MergedRowIteratorTest extends AbstractEasyMockTest {

  private static final String CUSTOM_PROP = "iterator";

  @Before
  public void setUp() throws Exception {
    super.setUp();
  }

  @Test
  public void testMerging() throws RepositoryException {
    MergedRowIterator iterator = createMergedRowIterator();
    assertEquals("B", getStringValueFromRow(iterator.nextRow(), CUSTOM_PROP));
    assertEquals("A", getStringValueFromRow(iterator.nextRow(), CUSTOM_PROP));
    assertEquals("B", getStringValueFromRow(iterator.nextRow(), CUSTOM_PROP));
    assertEquals("A", getStringValueFromRow(iterator.nextRow(), CUSTOM_PROP));
    assertEquals("B", getStringValueFromRow(iterator.nextRow(), CUSTOM_PROP));
    assertEquals("A", getStringValueFromRow((Row) iterator.next(), CUSTOM_PROP));
    assertEquals(6, iterator.getPosition());
    assertEquals(false, iterator.hasNext());
    try {
      iterator.next();
      fail("This should have thrown an IllegalStateException.");
    } catch (IllegalStateException e) {
      // Swallow exception
    }
  }

  @Test
  public void testSkipping() throws RepositoryException {
    MergedRowIterator iterator = createMergedRowIterator();
    iterator.skip(3);
    assertEquals(3, iterator.getPosition());
    iterator.skip(3);
    assertEquals(false, iterator.hasNext());
  }

  @Test
  public void testSize() throws RepositoryException {
    MergedRowIterator iterator = createMergedRowIterator();
    assertEquals(-1, iterator.getSize());
  }

  private String getStringValueFromRow(Row row, String prop)
      throws RepositoryException {
    return row.getValue(prop).getString();
  }

  private MergedRowIterator createMergedRowIterator()
      throws RepositoryException {
    MergedRowIterator mergedIterator = null;

    RowIterator iteratorA = EasyMock.createMock(RowIterator.class);
    addRowToIterator(iteratorA, 1000, CUSTOM_PROP, "A");
    addRowToIterator(iteratorA, 900, CUSTOM_PROP, "A");
    addRowToIterator(iteratorA, 800, CUSTOM_PROP, "A");
    expect(iteratorA.hasNext()).andReturn(false).anyTimes();
    expect(iteratorA.getSize()).andReturn(3L).anyTimes();
    EasyMock.replay(iteratorA);

    RowIterator iteratorB = EasyMock.createMock(RowIterator.class);
    addRowToIterator(iteratorB, 1100, CUSTOM_PROP, "B");
    addRowToIterator(iteratorB, 950, CUSTOM_PROP, "B");
    addRowToIterator(iteratorB, 850, CUSTOM_PROP, "B");
    expect(iteratorB.hasNext()).andReturn(false).anyTimes();
    expect(iteratorB.getSize()).andReturn(3L).anyTimes();
    EasyMock.replay(iteratorB);

    mergedIterator = new MergedRowIterator(iteratorA, iteratorB);

    return mergedIterator;
  }

  /**
   * @param iterator
   * @param i
   * @throws RepositoryException
   */
  private void addRowToIterator(RowIterator iterator, int score, String prop,
      String val) throws RepositoryException {
    Row row = createRow(score, prop, val);
    expect(iterator.hasNext()).andReturn(true);
    expect(iterator.nextRow()).andReturn(row);
  }

  /**
   * @param i
   * @return
   * @throws RepositoryException
   * @throws IllegalStateException
   * @throws ValueFormatException
   */
  private Row createRow(long score, String property, String value)
      throws RepositoryException {
    Row row = EasyMock.createMock(Row.class);

    Value valScore = EasyMock.createMock(Value.class);
    expect(valScore.getLong()).andReturn(score).anyTimes();
    EasyMock.replay(valScore);

    Value valProp = EasyMock.createMock(Value.class);
    expect(valProp.getString()).andReturn(value).anyTimes();
    EasyMock.replay(valProp);

    expect(row.getValue("jcr:score")).andReturn(valScore).anyTimes();
    expect(row.getValue(property)).andReturn(valProp).anyTimes();
    EasyMock.replay(row);
    return row;
  }
}
