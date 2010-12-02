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

import static org.junit.Assert.*;

import org.apache.sling.jcr.jackrabbit.server.impl.index.CloudTermImpl;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 *
 */
public class TermTest {

    @Test
    public void testIsSameName() {
        CloudTermImpl t1 = new CloudTermImpl("a",1);
        CloudTermImpl t2 = new CloudTermImpl("a",2);
        CloudTermImpl t3 = new CloudTermImpl("b",3);
        assertTrue(t1.isSameTerm(t2));
        assertTrue(t2.isSameTerm(t1));
        assertFalse(t3.isSameTerm(t1));
        assertFalse(t1.isSameTerm(t3));
    }
    
    @Test
    public void testMerge() {
        CloudTermImpl t1 = new CloudTermImpl("a",1);
        CloudTermImpl t2 = new CloudTermImpl("a",2);
        CloudTermImpl t3 = new CloudTermImpl("b",100);
        t1.merge(t2);
        t1.merge(t3);
        assertEquals(3,t1.getCount());
        assertEquals(2,t2.getCount());
        assertEquals(100,t3.getCount());
    }
    
    @Test
    public void testName() {
        CloudTermImpl t1 = new CloudTermImpl("a",1);
        assertEquals("a",t1.getName());
    }
    
    @Test
    public void testCompare() {
        List<CloudTermImpl> tl = new ArrayList<CloudTermImpl>();
        tl.add(new CloudTermImpl("1",1));
        tl.add(new CloudTermImpl("2",2));
        tl.add(new CloudTermImpl("z",3));
        tl.add(new CloudTermImpl("3",3));
        tl.add(new CloudTermImpl("a",3));
        tl.add(new CloudTermImpl("4",4));
        Collections.sort(tl);
        CloudTermImpl last = new CloudTermImpl("z",100);
        for ( CloudTermImpl t : tl ) {
            if ( last.getCount() == t.getCount() ) {
                assertTrue(last.getName().compareTo(t.getName()) <= 0);
            } else {
                assertTrue(last.getCount() > t.getCount());
            }
            last = t;
        }
    }
}
