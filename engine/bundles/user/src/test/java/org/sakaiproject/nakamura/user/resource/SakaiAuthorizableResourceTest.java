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
package org.sakaiproject.nakamura.user.resource;

import junit.framework.Assert;

import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.easymock.EasyMock;
import org.junit.Test;
import org.sakaiproject.nakamura.testutils.easymock.AbstractEasyMockTest;

import java.util.Iterator;
import java.util.Map.Entry;

import javax.jcr.RepositoryException;
import javax.jcr.Value;


/**
 *
 */
public class SakaiAuthorizableResourceTest extends AbstractEasyMockTest {

  
  @SuppressWarnings("unchecked")
  @Test
  public void test() throws RepositoryException {
    User user = createNiceMock(User.class);
    ResourceResolver resourceResolver = createNiceMock(ResourceResolver.class);
    Iterator<String> iterator = createNiceMock(Iterator.class);
    Value value = createNiceMock(Value.class);
    EasyMock.expect(user.getPropertyNames()).andReturn(iterator);
    EasyMock.expect(iterator.hasNext()).andReturn(true);
    EasyMock.expect(iterator.next()).andReturn("test1");
    EasyMock.expect(user.getProperty("test1")).andReturn(new Value[] {
      value,
      value,
    });
    EasyMock.expect(user.getID()).andReturn("ieb");
    EasyMock.expect(user.getPrincipal()).andReturn(new UserPrincipal("ieb"));
    
    replay();
    SakaiAuthorizableResource s = new SakaiAuthorizableResource(user, resourceResolver, "/user");
    SakaiAuthorizableValueMap m = (SakaiAuthorizableValueMap) s.adaptTo(ValueMap.class);
    Assert.assertNotNull(m);
    for ( Entry<String, Object> e : m.entrySet()) {
      Assert.assertNotNull(e.getKey());
      Assert.assertNotNull(e.getValue());
    }
    verify();
  }
}
