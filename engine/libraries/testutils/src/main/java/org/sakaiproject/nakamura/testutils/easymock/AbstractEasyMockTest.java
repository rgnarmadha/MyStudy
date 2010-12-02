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
package org.sakaiproject.nakamura.testutils.easymock;

import static org.easymock.EasyMock.expect;

import org.apache.jackrabbit.api.security.principal.ItemBasedPrincipal;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.easymock.EasyMock;
import org.junit.Before;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.PropertyDefinition;

public class AbstractEasyMockTest {
  private List<Object> mocks;

  @Before
  public void setUp() throws Exception {
    mocks = new ArrayList<Object>();
  }

  protected <T> T createMock(Class<T> c) {
    T result = org.easymock.EasyMock.createMock(c);
    mocks.add(result);
    return result;
  }

  protected <T> T createMock(String name, Class<T> c) {
    T result = org.easymock.EasyMock.createMock(name, c);
    mocks.add(result);
    return result;
  }

  protected <T> T createNiceMock(Class<T> c) {
    T result = org.easymock.EasyMock.createNiceMock(c);
    mocks.add(result);
    return result;
  }

  protected <T> T createNiceMock(String name, Class<T> c) {
    T result = org.easymock.EasyMock.createNiceMock(name, c);
    mocks.add(result);
    return result;
  }

  protected void replay() {
    org.easymock.EasyMock.replay(mocks.toArray());
  }

  protected void verify() {
    org.easymock.EasyMock.verify(mocks.toArray());
  }
  
  protected void addStringPropertyToNode(Node node, String propertyName,
      String propertyValue) throws ValueFormatException, RepositoryException {
    Property property = createMock(Property.class);
    expect(property.getString()).andReturn(propertyValue).anyTimes();
    PropertyDefinition definition = createMock(PropertyDefinition.class);
    expect(property.getDefinition()).andReturn(definition).anyTimes();
    expect(definition.isMultiple()).andReturn(false).anyTimes();

    expect(node.hasProperty(propertyName)).andReturn(true);
    expect(node.getProperty(propertyName)).andReturn(property);
  }
  
  protected void addPropertyToNode(Node node, String propertyName, Value[] values) throws ValueFormatException, RepositoryException {
    Property property = createMock(Property.class);
    expect(property.getValues()).andReturn(values).anyTimes();
    PropertyDefinition definition = createMock(PropertyDefinition.class);
    expect(property.getDefinition()).andReturn(definition).anyTimes();
    expect(definition.isMultiple()).andReturn(true).anyTimes();

    expect(node.hasProperty(propertyName)).andReturn(true);
    expect(node.getProperty(propertyName)).andReturn(property);
  }
  
  protected void addStringRequestParameter(SlingHttpServletRequest request,
      String key, String value) {
    RequestParameter param = createMock(RequestParameter.class);
    expect(param.getString()).andReturn(value).anyTimes();
    expect(request.getRequestParameter(key)).andReturn(param).anyTimes();
  }
  
  /**
   * Add an inputstream on a request parameter.
   * @param request
   * @param key
   * @param stream
   * @param size
   * @param filename
   * @throws IOException
   */
  protected void addFileUploadRequestParameter(SlingHttpServletRequest request,
      String key, InputStream stream, long size, String filename) throws IOException {
    RequestParameter param = createMock(RequestParameter.class);
    expect(param.getSize()).andReturn(size).anyTimes();
    expect(param.isFormField()).andReturn(false).anyTimes();
    expect(param.getFileName()).andReturn(filename).anyTimes();
    expect(param.getInputStream()).andReturn(stream).anyTimes();
    expect(request.getRequestParameter(key)).andReturn(param).anyTimes();
  }
  
  /**
   * If you are using the bundlecontext in a tracker somewhere, this is a handy utility.
   * @param className The name of the class you want to track.
   * @return {@link BundleContext}
   * @throws InvalidSyntaxException
   */
  protected BundleContext expectServiceTrackerCalls(String className) throws InvalidSyntaxException {
    BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
    Filter filter = EasyMock.createNiceMock(Filter.class);
    EasyMock.replay(filter);
    EasyMock.expect(
        bundleContext.createFilter("(objectClass=" + className + ")"))
        .andReturn(filter).anyTimes();
    bundleContext.addServiceListener((ServiceListener) EasyMock.anyObject(),
        EasyMock.eq("(objectClass=" + className + ")"));
    EasyMock.expectLastCall().anyTimes();
    EasyMock.expect(bundleContext.getServiceReferences(className, null))
        .andReturn(new ServiceReference[0]).anyTimes();
    EasyMock.replay(bundleContext);
    return bundleContext;
  }
  
  /**
   * @param id
   *          The id of the {@link User user} or {@link Group group}.
   * @param isGroup
   *          Whether or not this authorizable is a group.
   * @param doReplay
   *          Whether or not this mock should be replayed.
   * @return A mocked {@link Authorizable Authorizable}.
   * @throws RepositoryException
   */
  protected Authorizable createAuthorizable(String id, boolean isGroup, boolean doReplay)
      throws RepositoryException {
    Authorizable au = EasyMock.createMock(Authorizable.class);
    expect(au.getID()).andReturn(id).anyTimes();
    expect(au.isGroup()).andReturn(isGroup).anyTimes();
    ItemBasedPrincipal p = EasyMock.createMock(ItemBasedPrincipal.class);
    String hashedPath = "/"+id.substring(0,1)+"/"+id.substring(0,2)+"/"+id;
    expect(p.getPath()).andReturn("rep:" + hashedPath).anyTimes();
    expect(au.getPrincipal()).andReturn(p).anyTimes();
    expect(au.hasProperty("path")).andReturn(true).anyTimes();
    Value v = EasyMock.createNiceMock(Value.class);
    expect(v.getString()).andReturn(hashedPath).anyTimes();
    expect(au.getProperty("path")).andReturn(new Value[] { v }).anyTimes();
    EasyMock.replay(p);
    EasyMock.replay(v);
    if (doReplay) {
      EasyMock.replay(au);
    }
    return au;
  }

  /**
   * Adds a bunch of authorizables to a {@link UserManager user manager}.
   * 
   * @param um
   *          The UserManager where the authorizables should be added to. Pass in null to
   *          create a new one.
   * @param doReplay
   *          Whether the UserManager should be put in replay state.
   * @param authorizables
   *          The authorizables that need to be added.
   * @return
   * @throws RepositoryException 
   */
  protected UserManager createUserManager(UserManager um, boolean doReplay,
      Authorizable... authorizables) throws RepositoryException {
    if (um == null) {
      um = EasyMock.createMock(UserManager.class);
    }
    for (Authorizable au : authorizables) {
      expect(um.getAuthorizable(au.getID())).andReturn(au).anyTimes();
    }
    if (doReplay) {
      EasyMock.replay(um);
    }
    return um;
  }

}
