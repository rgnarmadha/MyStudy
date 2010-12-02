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
package org.sakaiproject.nakamura.batch;

import static org.junit.Assert.assertEquals;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.sling.commons.json.JSONException;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class JsonValueMapTest {

  private String json = "{'boolProp' : true, 'intProp' : 23, 'objProp' : {'alfa' : 'beta'}}";

  @Test
  public void testMap() throws JSONException {
    JsonValueMap map = new JsonValueMap(json);

    assertTrue(map.get("boolProp", Boolean.class));
    assertTrue(map.get("nonExestingThing", true));
    assertFalse(map.isEmpty());
    assertEquals(3, map.size());
    assertTrue(map.containsValue(23));

    map.put("happy", "banana");
    assertTrue(map.containsKey("happy"));
    map.remove("happy");
    assertEquals(null, map.get("happy"));
    
    Map<String, Object> newMap = new HashMap<String, Object>();
    newMap.put("new", true);
    map.putAll(newMap);
    assertTrue(map.containsKey("new"));
    assertTrue(map.get("new", Boolean.class));
    
    Set<String> keys = map.keySet();
    assertTrue(keys.contains("boolProp"));
    
    Collection<Object> vals = map.values();
    assertTrue(vals.contains(23));
    
    map.clear();
    assertEquals(0, map.size());
    
    

  }

}
