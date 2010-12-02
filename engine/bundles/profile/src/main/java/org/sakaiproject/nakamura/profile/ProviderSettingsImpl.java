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

import org.sakaiproject.nakamura.api.profile.ProviderSettings;
import org.sakaiproject.nakamura.util.JcrUtils;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

/**
 *
 */
public class ProviderSettingsImpl implements ProviderSettings {

  private String provider;
  private Node profileNode;
  private Node settingsNode;
  private Node providerNode;

  /**
   * @param node
   * @param node
   * @throws RepositoryException
   * @throws PathNotFoundException
   * @throws ValueFormatException
   */
  public ProviderSettingsImpl(Node profileNode, Node settingsNode)
      throws RepositoryException {
    this.profileNode = profileNode;
    this.settingsNode = settingsNode;
    if (settingsNode.hasProperty(ProviderSettings.PROFILE_PROVIDER)) {
      provider = settingsNode.getProperty(ProviderSettings.PROFILE_PROVIDER).getString();
    }
    if (settingsNode.hasProperty(ProviderSettings.PROFILE_PROVIDER_SETTINGS)) {
      String providerSettingsPath = settingsNode.getProperty(
          ProviderSettings.PROFILE_PROVIDER_SETTINGS).getString();
      Session session = profileNode.getSession();
      if (session.nodeExists(providerSettingsPath)) {
        this.providerNode = session.getNode(providerSettingsPath);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.profile.ProviderSettings#getProvider()
   */
  public String getProvider() {
    return provider;
  }

  public String[] getProfileSettingsProperty(String propertyName)
      throws RepositoryException {
    return getStringProperty(settingsNode, propertyName);
  }

  /**
   * @param settingsNode2
   * @param propertyName
   * @return
   */
  private String[] getStringProperty(Node node, String propertyName)
      throws RepositoryException {
    if (node == null) {
      return new String[0];
    }
    Value[] v = JcrUtils.getValues(node, propertyName);
    if (v == null || v.length == 0) {
      return new String[0];
    }
    String[] s = new String[v.length];
    for (int i = 0; i < s.length; i++) {
      s[i] = v[i].getString();
    }
    return s;
  }

  public String[] getProviderConfigProperty(String propertyName)
      throws RepositoryException {
    return getStringProperty(providerNode, propertyName);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.profile.ProviderSettings#getNode()
   */
  public Node getNode() {
    return profileNode;
  }

}
