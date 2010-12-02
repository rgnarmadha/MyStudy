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

package org.sakaiproject.nakamura.lifecycle;

import com.google.inject.Scopes;

import org.osgi.framework.BundleContext;
import org.sakaiproject.nakamura.api.registry.RegistryService;
import org.sakaiproject.nakamura.guice.AbstractOsgiModule;

/**
 * Configuration module for persistence bindings.
 */
public class ActivatorModule extends AbstractOsgiModule {

  public ActivatorModule(BundleContext bundleContext) {
    super(bundleContext);
  }

  /**
   * {@inheritDoc}
   *
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    super.configure();

    // we need the registry service.
    bind(RegistryService.class).toProvider(importService(RegistryService.class)).in(Scopes.SINGLETON);
   
  }
 
}
