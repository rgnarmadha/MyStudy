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
package org.sakaiproject.nakamura.search.processors;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.easymock.EasyMock;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class PageSearchPropertyProviderTest extends AbstractEasyMockTest {

  @Test
  public void test() {
    SlingHttpServletRequest request = createNiceMock(SlingHttpServletRequest.class);
    Map<String, String> propertiesMap = new HashMap<String, String>();
    
    RequestParameter pathParam = createNiceMock(RequestParameter.class);
    RequestParameter p1 = createNiceMock(RequestParameter.class);
    RequestParameter p1v = createNiceMock(RequestParameter.class);
    RequestParameter p1o = createNiceMock(RequestParameter.class);
    RequestParameter[] properties = new RequestParameter[] {
        p1
    };
    RequestParameter[] values = new RequestParameter[] {
        p1v
    };
    RequestParameter[] operators = new RequestParameter[] {
        p1o
    };
    EasyMock.expect(request.getRequestParameter("path")).andReturn(pathParam);
    EasyMock.expect(request.getRequestParameters("properties")).andReturn(properties);
    EasyMock.expect(request.getRequestParameters("values")).andReturn(values);
    EasyMock.expect(request.getRequestParameters("operators")).andReturn(operators);
    
    EasyMock.expect(p1.getString()).andReturn("name").anyTimes();
    EasyMock.expect(p1v.getString()).andReturn("value").anyTimes();
    EasyMock.expect(p1o.getString()).andReturn("=").anyTimes();
    EasyMock.expect(pathParam.getString()).andReturn("/test").anyTimes();
    
    Resource resource = createNiceMock(Resource.class);
    EasyMock.expect(request.getResource()).andReturn(resource);
    EasyMock.expect(resource.getPath()).andReturn("/test");
    
    replay();
    PageSearchPropertyProvider pageSearchPropertyProvider = new PageSearchPropertyProvider();
    pageSearchPropertyProvider.loadUserProperties(request, propertiesMap);
    
    verify();
  }
  
}
