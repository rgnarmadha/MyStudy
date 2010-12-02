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

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Row;

;
/**
 *
 */
public class RowUtilsTest {

  private List<Object> mocks;

  @Before
  public void setUp() {
    mocks = new ArrayList<Object>();
  }

  @Test
  public void testGetNode() throws RepositoryException {
    String absPath = "/foo/bar";
    Row row = getRow("jcr:path", absPath);
    Node node = createMock(Node.class);
    Session session = createMock(Session.class);
    expect(session.getItem(absPath)).andReturn(node).once();
    replay();
    RowUtils.getNode(row, session);
  }
  
  @Test
  public void testPath() throws RepositoryException {
    Row row = getRow("jcr:path", "/foo/bar");
    replay();
    String path = RowUtils.getPath(row);
    assertEquals("Path is not the same", "/foo/bar", path);
  }

  @Test
  public void testScore() throws RepositoryException {
    Row row = createMock(Row.class);
    Value value = createMock(Value.class);
    expect(value.getLong()).andReturn(Long.parseLong("1554"));
    expect(row.getValue("jcr:score")).andReturn(value);
    replay();
    long score = RowUtils.getScore(row);
    assertEquals("Score is not the same", 1554, score);
  }

  @Test
  public void testNamedExcerpt() throws RepositoryException {
    String excerptName = "bar";
    Row row = getRow("rep:excerpt(" + excerptName + ")", "foo");
    replay();
    String result = RowUtils.getExcerpt(row, excerptName);
    assertEquals("Excerpt is not the same", "foo", result);
  }

  @Test
  public void testExcerpt() throws RepositoryException {
    Row row = getRow("rep:excerpt(.)", "foo");
    replay();
    String result = RowUtils.getExcerpt(row);
    assertEquals("Excerpt is not the same", "foo", result);
  }
  
  @Test
  public void testDefaultExcerptContent() throws RepositoryException {
    Row row = getRow("rep:excerpt(jcr:content)", "foo");
    replay();
    String result = RowUtils.getDefaultExcerpt(row);
    assertEquals("Excerpt is not the same", "foo", result);
  }
  
  @Test
  public void testDefaultExcerptProp() throws RepositoryException {
    Row row = createMock(Row.class);
    Value value = createMock(Value.class);
    expect(value.getString()).andReturn("foo");
    expect(row.getValue("rep:excerpt(jcr:content)")).andReturn(null).once();
    expect(row.getValue("rep:excerpt(.)")).andReturn(value).once();
    replay();
    String result = RowUtils.getDefaultExcerpt(row);
    assertEquals("Excerpt is not the same", "foo", result);
  }

  private Row getRow(String key, String val) throws RepositoryException {
    Row row = createMock(Row.class);
    Value value = createMock(Value.class);
    expect(value.getString()).andReturn(val);
    expect(row.getValue(key)).andReturn(value);
    return row;
  }

  /*
   * Helper methods for mocking.
   */

  protected <T> T createMock(Class<T> c) {
    T result = org.easymock.EasyMock.createMock(c);
    mocks.add(result);
    return result;
  }

  protected void replay() {
    org.easymock.EasyMock.replay(mocks.toArray());
  }
}
