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
package org.sakaiproject.nakamura.guice;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.sakaiproject.nakamura.testutils.easymock.DictionaryMatcher.eqDictionary;
import static org.sakaiproject.nakamura.testutils.easymock.DictionaryMatcher.eqDictionaryUnorderedSubkeys;
import static org.sakaiproject.nakamura.testutils.easymock.UnorderedArrayMatcher.aryUnorderedEq;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.sakaiproject.nakamura.api.configuration.ConfigurationService;
import org.sakaiproject.nakamura.guice.GuiceActivator;
import org.sakaiproject.nakamura.guice.OsgiServiceProvider;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

public class TestGuiceActivation {

  static final String testServiceDescription = "My Test Service";
  static final String testVendor = "My Test Vendor";

  @Test
  public void testServiceExportWithDeclaration() throws Exception {
    BundleContext mockContext = createMock(BundleContext.class);
    Set<Class<?>> exports = new HashSet<Class<?>>();
    exports.add(DummyServiceWithExport.class);
    String[] expectedServiceClassNames = new String[] { DummyServiceWithExport.class.getName() };
    GuiceActivator activator = new DummyGuiceActivator(new DummyOsgiModule(mockContext, exports));
    Dictionary<String, Object> expectedParams = new Hashtable<String, Object>();
    expectedParams.put(Constants.OBJECTCLASS, expectedServiceClassNames);
    expectedParams.put(Constants.SERVICE_DESCRIPTION, testServiceDescription);
    expectedParams.put(Constants.SERVICE_VENDOR, testVendor);
    ConfigurationService configService = createMock(ConfigurationService.class);
    ServiceReference serviceReference = createMock(ServiceReference.class);
    expect(
        mockContext.registerService(aryEq(expectedServiceClassNames),
            isA(DummyServiceWithExport.class), eqDictionary(expectedParams))).andReturn(null);
    mockContext.addServiceListener(isA(OsgiServiceProvider.class), isA(String.class));
    expect(mockContext.getServiceReference(ConfigurationService.class.getName())).andReturn(
        serviceReference);
    expect(mockContext.getService(serviceReference)).andReturn(configService);
    expect(configService.getProperties()).andReturn(new HashMap<String, String>());
    replay(mockContext, configService, serviceReference);
    activator.start(mockContext);
    activator.stop(mockContext);
    verify(mockContext, configService, serviceReference);
  }

  @Test
  public void testServiceExportWithoutDeclaration() throws Exception {
    BundleContext mockContext = createMock(BundleContext.class);
    Set<Class<?>> exports = new HashSet<Class<?>>();
    exports.add(DummyServiceInterfaceA.class);
    exports.add(DummyServiceInterfaceB.class);
    exports.add(DummyServiceWithoutExport.class);
    GuiceActivator activator = new DummyGuiceActivator(new DummyOsgiModuleWithConfiguration(
        mockContext, exports));

    Dictionary<String, Object> expectedParams = new Hashtable<String, Object>();
    expectedParams.put(Constants.OBJECTCLASS,
        new String[] { DummyServiceInterfaceA.class.getName() });
    expectedParams.put(Constants.SERVICE_DESCRIPTION, DummyServiceInterfaceA.class.getName());
    expectedParams.put(Constants.SERVICE_VENDOR, "The Sakai Foundation");
    expect(
        mockContext.registerService(aryEq(new String[] { DummyServiceInterfaceA.class.getName() }),
            isA(DummyServiceWithoutExport.class), eqDictionary(expectedParams))).andReturn(null);

    Dictionary<String, Object> expectedParams2 = new Hashtable<String, Object>();
    expectedParams2.put(Constants.OBJECTCLASS, new String[] { DummyServiceInterfaceB.class
        .getName() });
    expectedParams2.put(Constants.SERVICE_DESCRIPTION, DummyServiceInterfaceB.class.getName());
    expectedParams2.put(Constants.SERVICE_VENDOR, "The Sakai Foundation");
    expect(
        mockContext.registerService(aryEq(new String[] { DummyServiceInterfaceB.class.getName() }),
            isA(DummyServiceWithoutExport.class), eqDictionary(expectedParams2))).andReturn(null);

    Dictionary<String, Object> expectedParams3 = new Hashtable<String, Object>();
    expectedParams3.put(Constants.OBJECTCLASS, new String[] {
        DummyServiceInterfaceA.class.getName(), DummyServiceInterfaceB.class.getName() });
    expectedParams3.put(Constants.SERVICE_DESCRIPTION, DummyServiceWithoutExport.class.getName());
    expectedParams3.put(Constants.SERVICE_VENDOR, "The Sakai Foundation");
    expect(
        mockContext.registerService(aryUnorderedEq(new String[] {
            DummyServiceInterfaceA.class.getName(), DummyServiceInterfaceB.class.getName() }),
            isA(DummyServiceWithoutExport.class), eqDictionaryUnorderedSubkeys(expectedParams3)))
        .andReturn(null);

    ConfigurationService configService = createMock(ConfigurationService.class);
    ServiceReference serviceReference = createMock(ServiceReference.class);
    mockContext.addServiceListener(isA(OsgiServiceProvider.class), isA(String.class));
    expect(mockContext.getServiceReference(ConfigurationService.class.getName())).andReturn(
        serviceReference);
    expect(mockContext.getService(serviceReference)).andReturn(configService);
    expect(configService.getProperties()).andReturn(null);
    replay(mockContext, configService, serviceReference);
    activator.start(mockContext);
    verify(mockContext, configService, serviceReference);
  }
}
