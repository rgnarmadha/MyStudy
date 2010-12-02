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
import static org.junit.Assert.fail;

import org.apache.sling.jcr.jackrabbit.server.impl.index.CloudTermImpl;
import org.apache.sling.jcr.jackrabbit.server.impl.index.TermCloud;
import org.junit.Test;

import java.util.Iterator;

/**
 *
 */
public class TermCloudTest {

    @Test
    public void testAdd() {
        TermCloud tc = new TermCloud(10);
        tc.add(new CloudTermImpl("a", 1));
        tc.add(new CloudTermImpl("a", 1));
        tc.add(new CloudTermImpl("a", 1));
        tc.add(new CloudTermImpl("a", 1));
        Iterator<CloudTerm> i = tc.iterator(5);
        assertFalse(i.hasNext());
        tc.add(new CloudTermImpl("a", 1));
        i = tc.iterator(5);
        assertTrue(i.hasNext());
        CloudTerm t = i.next();
        assertTrue("a".equals(t.getName()));
        assertEquals(5, t.getCount());
        assertFalse(i.hasNext());
        i = tc.iterator(0);
        assertTrue(i.hasNext());
        t = i.next();
        assertTrue("a".equals(t.getName()));
        assertEquals(5, t.getCount());
        assertFalse(i.hasNext());
        tc.add(new CloudTermImpl("b", 1));
        i = tc.iterator(0);
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
        TermCloud tc = new TermCloud(10);
        for (int i = 0; i < 10; i++) {
            tc.add(new CloudTermImpl(String.valueOf(i), i));
        }
        Iterator<CloudTerm> itc = tc.iterator(-1);
        for (int i = 9; i >= 0; i--) {
            assertTrue(itc.hasNext());
            assertEquals(i, itc.next().getCount());
        }
        assertFalse(itc.hasNext());

        tc.add(new CloudTermImpl(String.valueOf(10), 10));
        itc = tc.iterator(-1);
        for (int i = 9; i >= 0; i--) {
            assertTrue(itc.hasNext());
            assertEquals(i + 1, itc.next().getCount());
        }
        assertFalse(itc.hasNext());
    }

    @Test
    public void testImmutable() {
        TermCloud tc = new TermCloud(10);
        for (int i = 0; i < 10; i++) {
            tc.add(new CloudTermImpl(String.valueOf(i), i));
        }
        Iterator<CloudTerm> itc = tc.iterator(-1);
        try {
            itc.remove();
            fail();
        } catch (UnsupportedOperationException e) {

        }
    }
    
    

}
