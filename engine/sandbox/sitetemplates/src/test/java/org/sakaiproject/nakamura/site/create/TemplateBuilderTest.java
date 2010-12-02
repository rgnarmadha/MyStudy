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
package org.sakaiproject.nakamura.site.create;

import static org.apache.jackrabbit.JcrConstants.JCR_CREATED;
import static org.apache.jackrabbit.JcrConstants.JCR_UUID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Lists;

import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.util.IOUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 *
 */
public class TemplateBuilderTest {

  private JSONObject json;

  @Before
  public void setUp() throws Exception {
    if (json == null) {
      InputStream in = getClass().getResourceAsStream("data.json");
      String s = IOUtils.readFully(in, "UTF-8");
      json = new JSONObject(s);
    }
  }

  @Test
  public void testNodeProperties() throws Exception {
    TemplateBuilder builder = new TemplateBuilder();
    builder.setJson(json);

    List<String> propertiesToIgnore = Lists.newArrayList(JCR_UUID, JCR_CREATED);
    Map<String, Object> map = new HashMap<String, Object>();

    MockNode node = new MockNode("/path/to/node");
    node.setProperty(JCR_UUID, "aaaaa-23bbe3-abcde");
    node.setProperty("sakai:int", 2);
    node.setProperty("sakai:s", "foobar");
    node.setProperty("sakai:multi", new String[] { "foo", "bar" });

    builder.addProperties(node, map, propertiesToIgnore);

    assertFalse(map.containsKey(JCR_UUID));
    assertFalse(map.containsKey(JCR_CREATED));

    assertEquals(2, ((Value) map.get("sakai:int")).getLong());
    assertEquals("foobar", ((Value) map.get("sakai:s")).getString());
    assertEquals(2, ((Value[]) map.get("sakai:multi")).length);
  }

  @Test
  public void testConditionalStatement() throws RepositoryException {
    String condition = "@@site.properties.id==mysite?@@";
    TemplateBuilder builder = new TemplateBuilder();
    builder.setJson(json);
    Value val = createValue(condition);

    assertTrue(builder.isConditionalStatement(val));
    val = createValue("foo");
    assertFalse(builder.isConditionalStatement(val));

    assertTrue(builder.isConditionTrue(condition));

    // Boolean
    condition = "@@site.properties.private==false?@@";
    assertTrue(builder.isConditionTrue(condition));

    // int
    condition = "@@site.properties.int==2?@@";
    assertTrue(builder.isConditionTrue(condition));
    condition = "@@site.properties.int==5?@@";
    assertFalse(builder.isConditionTrue(condition));
    condition = "@@site.properties.int==foo?@@";
    assertFalse(builder.isConditionTrue(condition));

    // double
    condition = "@@site.properties.double==2.3?@@";
    assertTrue(builder.isConditionTrue(condition));

    // long
    condition = "@@site.properties.long==12345678912345?@@";
    assertTrue(builder.isConditionTrue(condition));
  }

  @Test
  public void testLoopStatement() throws RepositoryException {
    TemplateBuilder builder = new TemplateBuilder();
    builder.setJson(json);
    Value val = createValue("@@site.pages(...)@@");
    assertTrue(builder.isLoopStatement(val));
    val = createValue("foo");
    assertFalse(builder.isLoopStatement(val));
  }

  @Test
  public void testPlaceHolders() throws RepositoryException {
    TemplateBuilder builder = new TemplateBuilder();
    builder.setJson(json);
    Value val = createValue("@@site.pages.id@@");

    assertTrue(builder.isPlaceHolder(val));

    val = createValue("foo");
    assertFalse(builder.isPlaceHolder(val));
  }

  private Value createValue(String v) {
    return new MockValue(v);
  }

}
