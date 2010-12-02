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
package org.sakaiproject.nakamura.registry;

import org.sakaiproject.nakamura.api.registry.Provider;
import org.sakaiproject.nakamura.api.registry.Registry;
import org.sakaiproject.nakamura.api.registry.RegistryService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The <code>CacheManagerServiceImpl</code>
 * 
 * @scr.component immediate="true" label="RegistryServiceImpl"
 *                description="Implementation of the Cache Manager Service"
 *                name="org.sakaiproject.nakamura.api.registry.RegistryService"
 * @scr.service interface="org.sakaiproject.nakamura.api.registry.RegistryService"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="service.description" value="Registry Service Implementation"
 */
public class RegistryServiceImpl implements RegistryService {

  private Map<String, Registry<?, ? extends Provider<?>>> providerMap = new ConcurrentHashMap<String, Registry<?, ? extends Provider<?>>>();

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.user.ProviderRegistryService#getProviderRegistry(org.sakaiproject.nakamura.api.user.ProviderRegistryType)
   */
  @SuppressWarnings("unchecked")
  public <V, T extends Provider<V>> Registry<V, T> getRegistry(String type) {
    Registry<V, ? extends Provider> providerRegistry = (Registry<V, ? extends Provider>) providerMap
        .get(type);
    if (providerRegistry == null) {
      providerRegistry = new RegistryImpl<V, T>();
      providerMap.put(type, (Registry<?, ? extends Provider<?>>) providerRegistry);
    }
    return (Registry<V, T>) providerRegistry;
  }

}
