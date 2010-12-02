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

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.jcr.Node;

/**
 * Provide external Maps of profile.
 */
public interface ProfileProvider {

  /**
   * The name of the Profile Provider. This has to match the property set on the node at
   * /var/profile/providersByName/**
   */
  static final String PROVIDER_NAME = "sakai.profile.provider.name";

  /**
   * Generate a Map of Nodes to Future Maps for this provider, where the Future Map will
   * replace the node subtree at the Node referenced in the map.
   *
   * @param list
   *          a list of {@link ProviderSettings} to be processed efficiently as a batch
   *          operation.
   * @return
   */
  Map<? extends Node, ? extends Future<Map<String, Object>>> getProvidedMap(
      List<ProviderSettings> list);

}
