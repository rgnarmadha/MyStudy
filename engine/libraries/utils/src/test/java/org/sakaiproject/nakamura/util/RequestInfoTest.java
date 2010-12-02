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
package org.sakaiproject.nakamura.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.Test;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

/**
 *
 */
public class RequestInfoTest {

  @Test
  public void testJSON() throws JSONException {
    JSONObject o = new JSONObject();
    o.put("method", "POST");
    o.put("url", "/foo/bar");
    JSONObject parameters = new JSONObject();
    parameters.put("sling:resourceType", "bla");
    JSONArray arr = new JSONArray();
    arr.put("v1").put("v2");
    parameters.put("multi", arr);
    o.put("parameters", parameters);

    RequestInfo info = new RequestInfo(o);
    assertEquals("POST", info.getMethod());
    assertEquals("/foo/bar", info.getUrl());
    Hashtable<String, String[]> table = info.getParameters();
    assertEquals(table.get("sling:resourceType")[0], "bla");
    List<String> lst = Arrays.asList((String[]) table.get("multi"));
    assertTrue(lst.contains("v1"));
    assertTrue(lst.contains("v2"));
  }

}
