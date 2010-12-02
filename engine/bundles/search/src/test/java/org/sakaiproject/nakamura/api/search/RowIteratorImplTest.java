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

import junit.framework.Assert;

import org.junit.Test;
import org.sakaiproject.nakamura.search.RowIteratorImpl;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.query.Row;


/**
 *
 */
public class RowIteratorImplTest extends AbstractEasyMockTest {

  @Test
  public void test() {
    Row row1 = createNiceMock(Row.class);
    Row row2 = createNiceMock(Row.class);
    
    
    replay();
    List<Row> rows = new ArrayList<Row>();
    rows.add(row1);
    rows.add(row2);
       
    RowIteratorImpl rowIteratorImpl = new RowIteratorImpl(rows);
    Assert.assertEquals(2, rowIteratorImpl.getSize());
    Assert.assertEquals(0, rowIteratorImpl.getPosition());
    Assert.assertTrue(rowIteratorImpl.hasNext());
    Assert.assertEquals(row1, rowIteratorImpl.next());
    Assert.assertTrue(rowIteratorImpl.hasNext());
    Assert.assertEquals(row2, rowIteratorImpl.nextRow());

    try {
      rowIteratorImpl.skip(10L);
      Assert.fail();
    } catch ( NoSuchElementException e) {
      // Swallow exception.
    }
    verify();
  }
}
