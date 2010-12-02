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

import static org.easymock.EasyMock.createMock;

import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Test;

import java.io.IOException;
import java.util.Dictionary;

/**
 *
 */
public class ResponseWrapperTest {

  @Test
  public void testResponse() throws IOException {
    SlingHttpServletResponse response = createMock(SlingHttpServletResponse.class);
    ResponseWrapper wrapper = new ResponseWrapper(response);

    // Random
    wrapper.setCharacterEncoding("UTF-8");
    assertEquals("UTF-8", wrapper.getCharacterEncoding());

    // set some headers
    wrapper.setHeader("foo", "bar");
    wrapper.addHeader("alfa", "beta");
    wrapper.addIntHeader("life", 42);
    wrapper.setContentType("text/plain");
    wrapper.getWriter().write("Lorum lipsum.");
    Dictionary<String, String> headers = wrapper.getResponseHeaders();
    assertEquals(200, wrapper.getResponseStatus()); // Default code should be 200
    assertEquals(false, wrapper.isCommitted()); // Should always be false!
    assertEquals("bar", headers.get("foo"));
    assertEquals("beta", headers.get("alfa"));
    assertEquals("42", headers.get("life"));
    assertEquals("text/plain", wrapper.getContentType());
    assertEquals("text/plain", headers.get("Content-Type"));
    assertEquals("42", headers.get("life"));
    assertEquals("Lorum lipsum.", wrapper.getDataAsString());

    // Test status codes
    wrapper.sendError(404);
    assertEquals(404, wrapper.getResponseStatus());
    wrapper.sendError(404, "foo");
    assertEquals(404, wrapper.getResponseStatus());
    wrapper.setStatus(200);
    assertEquals(200, wrapper.getResponseStatus());
    wrapper.setStatus(200, "foo");
    assertEquals(200, wrapper.getResponseStatus());

  }

}
