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
package org.sakaiproject.nakamura.api.registry.utils;

import org.sakaiproject.nakamura.api.registry.ComponentLifecycle;
import org.sakaiproject.nakamura.api.registry.Registry;
import org.sakaiproject.nakamura.api.registry.RegistryService;

/**
 * 
 */
public class RegistryServiceUtil {

  private static final String COMPONENT_LIFECYCLE_REGISTRY = "component-lifecycle";

  /**
   * @param activator
   */
  public static void addComponentLifecycle(RegistryService registryService, ComponentLifecycle lifecycleComponent) {
    getLifecycleRegistry(registryService).add(lifecycleComponent);
  }

  /**
   * @param registryService 
   * @return
   */
  public static Registry<String, ComponentLifecycle> getLifecycleRegistry(RegistryService registryService) {
    return registryService.getRegistry(COMPONENT_LIFECYCLE_REGISTRY);
    
  }

  /**
   * @param instance
   * @param activator
   */
  public static void removeComponentLifecycle(RegistryService registryService,
      ComponentLifecycle lifecycleComponent) {
    getLifecycleRegistry(registryService).remove(lifecycleComponent);
  }

}
