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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import org.apache.sling.api.SlingHttpServletRequest;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 *
 */
public class RequestWrapperTest {

  @SuppressWarnings("unchecked")
  @Test
  public void test() throws UnsupportedEncodingException {
    SlingHttpServletRequest request = createMock(SlingHttpServletRequest.class);
    expect(request.getCharacterEncoding()).andReturn("UTF-8");
    replay(request);

    RequestInfo requestInfo = new RequestInfo();
    requestInfo.setMethod("POST");
    requestInfo.setUrl("/foo/bar");
    RequestWrapper wrapper = new RequestWrapper(request, requestInfo);


    Hashtable<String, String[]> parameters = new Hashtable<String, String[]>();
    parameters.put("foo", new String[] { "bar" });
    parameters.put("alfabet", new String[] { "alfa", "beta" });
    requestInfo.setParameters(parameters);

    assertEquals("bar", wrapper.getRequestParameter("foo").getString());
    assertEquals("bar", wrapper.getParameter("foo"));
    assertEquals(null, wrapper.getParameter("non-existing"));
    assertEquals(2, wrapper.getParameterValues("alfabet").length);
    assertEquals("POST", wrapper.getMethod());
    assertEquals("bar", wrapper.getParameterValues("foo")[0]);
    assertEquals("bar", ((String[]) wrapper.getParameterMap().get("foo"))[0]);
    assertEquals("alfa", wrapper.getRequestParameterMap().getValues("alfabet")[0]
        .getString());
    assertEquals("alfa", wrapper.getRequestParameters("alfabet")[0].getString());
    Enumeration<String> en = wrapper.getParameterNames();
    String s = (String) en.nextElement();
    assertEquals("alfabet", s);
    s = (String) en.nextElement();
    assertEquals("foo", s);

  }

}
