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
package org.apache.sling.jcr.jackrabbit.server.index;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.jackrabbit.core.query.lucene.FieldNames;
import org.apache.sling.jcr.jackrabbit.server.impl.index.TermCloudVectorMapper;
import org.junit.Test;

import java.util.Iterator;

/**
 *
 */
public class TermCloudVectorMapperTest {

    @Test
    public void testAdd() {
        TermCloudVectorMapper tvcm = new TermCloudVectorMapper(10);
        assertTrue(tvcm.isIgnoringOffsets());
        assertTrue(tvcm.isIgnoringPositions());
        tvcm.setDocumentNumber(1);
        tvcm.setExpectations(FieldNames.FULLTEXT, 10, false, false);
        tvcm.setExpectations(FieldNames.FULLTEXT, 100, false, false); //this will produce a log message
        tvcm.map("a", 1, null, null);
        tvcm.map("a", 1, null, null);
        tvcm.map("a", 1, null, null);
        tvcm.map("a", 1, null, null);
        Iterator<CloudTerm> i = tvcm.iterator(5);
        assertFalse(i.hasNext());
        tvcm.map("a", 1, null, null);
        i = tvcm.iterator(5);
        assertTrue(i.hasNext());
        CloudTerm t = i.next();
        assertTrue("a".equals(t.getName()));
        assertEquals(5, t.getCount());
        assertFalse(i.hasNext());
        i = tvcm.iterator(0);
        assertTrue(i.hasNext());
        t = i.next();
        assertTrue("a".equals(t.getName()));
        assertEquals(5, t.getCount());
        assertFalse(i.hasNext());
        tvcm.map("b", 1, null, null);
        i = tvcm.iterator(0);
        assertTrue(i.hasNext());
        t = i.next();
        assertTrue("a".equals(t.getName()));
        assertEquals(5, t.getCount());
        assertTrue(i.hasNext());
        t = i.next();
        assertTrue("b".equals(t.getName()));
        assertEquals(1, t.getCount());
        assertFalse(i.hasNext());
    }

    @Test
    public void testAddOverflow() {
        TermCloudVectorMapper tvcm = new TermCloudVectorMapper(10);
        for (int i = 0; i < 10; i++) {
            tvcm.map(String.valueOf(i), i,null,null);
        }
        Iterator<CloudTerm> itc = tvcm.iterator(-1);
        for (int i = 9; i >= 0; i--) {
            assertTrue(itc.hasNext());
            assertEquals(i, itc.next().getCount());
        }
        assertFalse(itc.hasNext());

        tvcm.map(String.valueOf(10), 10, null, null);
        itc = tvcm.iterator(-1);
        for (int i = 9; i >= 0; i--) {
            assertTrue(itc.hasNext());
            assertEquals(i + 1, itc.next().getCount());
        }
        assertFalse(itc.hasNext());
    }

    

}
