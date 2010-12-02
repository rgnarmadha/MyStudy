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
package org.sakaiproject.nakamura.api.profile;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 *
 */
public interface ProviderSettings {

  /**
   * The property name of the profile provider. This can usually be found on nodes under
   * /var/search/profile/providersByName/***
   */
  static final String PROFILE_PROVIDER = "sakai:profile-provider";

  /**
   * The property name of the profile provider settings. This will hold the path to the
   * settings for a provider.
   */
  static final String PROFILE_PROVIDER_SETTINGS = "sakai:profile-provider-settings";

  /**
   * @return
   */
  String getProvider();

  /**
   * Get a property, as String[] from the configuration for this provider in the JCR.
   *
   * @param propertyName
   *          the name of the property
   * @return an array of values, if there are none it will be an array of length 0.
   * @throws RepositoryException
   */
  String[] getProviderConfigProperty(String propertyName) throws RepositoryException;

  /**
   * Get the settings for this profile node.
   *
   * @param propertyName
   *          the name of the property.
   * @return an array of values, if there are none it will be an array of length 0.
   * @throws RepositoryException
   */
  String[] getProfileSettingsProperty(String propertyName) throws RepositoryException;

  /**
   * @return the node that this setting relates to.
   */
  Node getNode();

}
