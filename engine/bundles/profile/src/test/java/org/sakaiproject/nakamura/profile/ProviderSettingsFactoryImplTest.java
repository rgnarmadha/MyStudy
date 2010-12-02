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
package org.sakaiproject.nakamura.profile;


import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sakaiproject.nakamura.api.profile.ProviderSettings;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;

/**
 *
 */
public class ProviderSettingsFactoryImplTest {


  @Mock
  private Session session;
  @Mock
  private Node node;
  @Mock
  private Property config1Property;
  @Mock
  private PropertyDefinition propertyDefinition;
  @Mock
  private Value value;
  @Mock
  private Property settings1Property;

  /**
   *
   */
  public ProviderSettingsFactoryImplTest() {
   MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testFactory() throws RepositoryException {
    ProviderSettingsFactory pf = new ProviderSettingsFactory();

    String nodeName = "externalnode";
    String path = "testpath";
    String providerName = "testprovider";
    String providerConfigPath = "/var/profile/ldapconfig";

    Mockito.when(node.getSession()).thenReturn(session);

    ExternalNodeConfig e = ExternalNodeConfig.configExternal(node, nodeName, path, providerName, providerConfigPath);
    Mockito.when(e.getConfigNode().hasProperty("config1")).thenReturn(true);
    Mockito.when(e.getConfigNode().getProperty("config1")).thenReturn(config1Property);
    Mockito.when(config1Property.getDefinition()).thenReturn(propertyDefinition);
    Mockito.when(propertyDefinition.isMultiple()).thenReturn(true);
    Mockito.when(config1Property.getValues()).thenReturn(new Value[]{ value, value });



    Mockito.when(e.getSettingsNode().hasProperty("settings1")).thenReturn(true);
    Mockito.when(e.getSettingsNode().getProperty("settings1")).thenReturn(settings1Property);
    Mockito.when(settings1Property.getDefinition()).thenReturn(propertyDefinition);
    Mockito.when(propertyDefinition.isMultiple()).thenReturn(true);
    Mockito.when(settings1Property.getValues()).thenReturn(new Value[]{ value, value });

    Mockito.when(value.getString()).thenReturn("test1", "test2", "test3", "test4");

    ProviderSettings settings = pf.newProviderSettings("testpath/externalnode", node);

    Assert.assertEquals("testprovider", settings.getProvider());
    Assert.assertArrayEquals(new String[]{"test1","test2"}, settings.getProviderConfigProperty("config1"));
    Assert.assertArrayEquals(new String[]{}, settings.getProviderConfigProperty("config2"));
    Assert.assertArrayEquals(new String[]{"test3","test4"}, settings.getProfileSettingsProperty("settings1"));
    Assert.assertArrayEquals(new String[]{}, settings.getProviderConfigProperty("settings2"));

  }

}
