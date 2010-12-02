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

import org.mockito.Mockito;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
public class ExternalNodeConfig {

  private Node configNode;
  private Node settingsNode;

  /**
   * @param settingsNode
   * @param configNode
   */
  private ExternalNodeConfig(Node settingsNode, Node configNode) {
    this.settingsNode = settingsNode;
    this.configNode = configNode;
  }

  /**
   * @return the configNode
   */
  public Node getConfigNode() {
    return configNode;
  }

  /**
   * @return the settingsNode
   */
  public Node getSettingsNode() {
    return settingsNode;
  }


  /**
   * @param node2
   * @param nodeName
   * @param path
   * @param providerName
   * @param providerConfigPath
   * @throws RepositoryException
   */
  public static ExternalNodeConfig configExternal(Node externalNode, String nodeName, String path,
      String providerName, String providerConfigPath) throws RepositoryException {
    if ( !path.startsWith("/") ) {
      path = "/"+path;
    }
    if ( !path.endsWith("/")) {
      path = path + "/";
    }
    String settingsNodeName = ProviderSettingsFactory.PROVIDER_SETTINGS+path+nodeName;

    System.err.println("External Settings are at "+settingsNodeName);


    Session session = externalNode.getSession();
    Property sourceProperty = Mockito.mock(Property.class);
    Node settingsNode = Mockito.mock(Node.class);
    Node configNode = Mockito.mock(Node.class);
    Property providerProperty = Mockito.mock(Property.class);
    PropertyIterator pi = Mockito.mock(PropertyIterator.class);

    Mockito.when(externalNode.getProperties()).thenReturn(pi);
    Mockito.when(externalNode.hasProperty("sakai:source")).thenReturn(true);
    Mockito.when(externalNode.getProperty("sakai:source")).thenReturn(sourceProperty);
    Mockito.when(sourceProperty.getString()).thenReturn("external");
    Mockito.when(externalNode.getName()).thenReturn(nodeName);
    Mockito.when(session.nodeExists(settingsNodeName)).thenReturn(true);
    Mockito.when(session.nodeExists(providerConfigPath)).thenReturn(true);
    Mockito.when(session.getNode(settingsNodeName)).thenReturn(settingsNode);
    Mockito.when(settingsNode.hasProperty("sakai:profile-provider")).thenReturn(true);
    Mockito.when(settingsNode.hasProperty("sakai:profile-provider-settings")).thenReturn(true);
    Mockito.when(settingsNode.getProperty("sakai:profile-provider")).thenReturn(providerProperty);
    Mockito.when(providerProperty.getString()).thenReturn(providerName,providerConfigPath);
    Mockito.when(settingsNode.getProperty("sakai:profile-provider-settings")).thenReturn(providerProperty);
    Mockito.when(settingsNode.getSession()).thenReturn(session);
    Mockito.when(session.getNode(providerConfigPath)).thenReturn(configNode);


    return new ExternalNodeConfig(settingsNode, configNode);
  }

}
