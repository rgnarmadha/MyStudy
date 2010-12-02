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
package org.sakaiproject.nakamura.util.parameters;

import junit.framework.Assert;

import org.apache.sling.api.request.RequestParameter;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ParameterMapTest {
  private static final String UTF_8 = "UTF-8";
  private ParameterMap pm;

  @Before
  public void before() {
    pm = new ParameterMap();
    pm.addParameter("tp1", new ContainerRequestParameter("test1", UTF_8));
    pm.addParameter("tp2", new ContainerRequestParameter("test2", UTF_8));
    pm.addParameter("tp3", new ContainerRequestParameter("test3", UTF_8));
    pm.addParameter("tp4", new ContainerRequestParameter("test4", UTF_8));
    
  }
  @Test
  public void testSingle() {
    RequestParameter rp = new ContainerRequestParameter("test1", UTF_8);
    Assert.assertFalse(pm.containsKey("testparam"));
    pm.addParameter("testparam", rp);
    Assert.assertTrue(pm.containsKey("testparam"));
    pm.renameParameter("testparam", "newT");
    Assert.assertFalse(pm.containsKey("testparam"));
    Assert.assertTrue(pm.containsKey("newT"));
    String s = pm.getStringValue("newT");
    Assert.assertEquals("test1", s);
    
    String[] sa = pm.getStringValues("newT");
    Assert.assertEquals(1, sa.length);
    Assert.assertEquals(sa[0], "test1");
  }
  
  @Test
  public void testMultiple() {
    pm.addParameter("testparam", new ContainerRequestParameter("a1", UTF_8));
    pm.addParameter("testparam", new ContainerRequestParameter("a2", UTF_8));
    pm.addParameter("testparam", new ContainerRequestParameter("a3", UTF_8));
    
    String s = pm.getStringValue("testparam");
    Assert.assertEquals("a1", s);

    String[] sa = pm.getStringValues("testparam");
    Assert.assertEquals(3, sa.length);
    Assert.assertEquals(sa[0], "a1");
    Assert.assertEquals(sa[1], "a2");
    Assert.assertEquals(sa[2], "a3");
    
    s = pm.getStringValue("testparam-none");
    Assert.assertNull(s);
  }
  
  @Test
  public void testMap() {
    pm.addParameter("newT", new ContainerRequestParameter("test1", UTF_8));
    pm.addParameter("testparam", new ContainerRequestParameter("a1", UTF_8));
    pm.addParameter("testparam", new ContainerRequestParameter("a2", UTF_8));
    pm.addParameter("testparam", new ContainerRequestParameter("a3", UTF_8));
    
    Map<String,String[]> m = pm.getStringParameterMap();
    String[] sa = m.get("testparam");
    Assert.assertEquals(3, sa.length);
    Assert.assertEquals(sa[0], "a1");
    Assert.assertEquals(sa[1], "a2");
    Assert.assertEquals(sa[2], "a3");

    sa = m.get("newT");
    Assert.assertEquals(1, sa.length);
    Assert.assertEquals(sa[0], "test1");
  }
  @Test
  public void testMultipleAdd() {
    RequestParameter[] rpa = new RequestParameter[] {
        new ContainerRequestParameter("test1a", UTF_8),
        new ContainerRequestParameter("test2b", UTF_8),
        new ContainerRequestParameter("test3c", UTF_8),
        new ContainerRequestParameter("test4d", UTF_8)
    };
    pm.setParameters("tt", rpa);
    String[] sa = pm.getStringValues("tt");
    Assert.assertEquals(4, sa.length);
    Assert.assertEquals(sa[0], "test1a");
    Assert.assertEquals(sa[1], "test2b");
    Assert.assertEquals(sa[2], "test3c");
    Assert.assertEquals(sa[3], "test4d");
    
  }
  
  @Test
  public void testNotImplemented() {
    try {
      pm.clear();
      Assert.fail();
    } catch (UnsupportedOperationException e) {
      
    }
    try {
      pm.put("test", new RequestParameter[]{});
      Assert.fail();
    } catch (UnsupportedOperationException e) {
      
    }
    try {
      pm.putAll(new HashMap<String, RequestParameter[]>());
      Assert.fail();
    } catch (UnsupportedOperationException e) {
      
    }
    try {
      pm.remove("dont-remove");
      Assert.fail();
    } catch (UnsupportedOperationException e) {
      
    }
  }
}
