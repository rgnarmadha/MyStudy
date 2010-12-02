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

import org.osgi.framework.BundleContext;
import org.sakaiproject.nakamura.api.registry.ComponentLifecycle;
import org.sakaiproject.nakamura.api.registry.Registry;
import org.sakaiproject.nakamura.api.registry.RegistryService;
import org.sakaiproject.nakamura.api.registry.utils.RegistryServiceUtil;
import org.sakaiproject.nakamura.guice.AbstractOsgiModule;
import org.sakaiproject.nakamura.guice.GuiceActivator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Activator extends GuiceActivator {

  protected static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.guice.GuiceActivator#getModule()
   */
  @Override
  protected AbstractOsgiModule getModule(BundleContext bundleContext) {
    return new ActivatorModule(bundleContext);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.guice.GuiceActivator#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext bundleContext) throws Exception {
    super.start(bundleContext);
    Registry<String, ComponentLifecycle> components = RegistryServiceUtil
        .getLifecycleRegistry(injector.getInstance(RegistryService.class));
    List<ComponentLifecycle> componentList = components.getList();
    for (int i = componentList.size() - 1; i >= 0; i--) {
      LOGGER.info("Init for "+componentList.get(i));
      componentList.get(i).init();
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.guice.GuiceActivator#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext bundleContext) throws Exception {
    Registry<String, ComponentLifecycle> components = RegistryServiceUtil
        .getLifecycleRegistry(injector.getInstance(RegistryService.class));
    for (ComponentLifecycle component : components.getList()) {
      LOGGER.info("Destroy for "+component);
      component.destroy();
    }
    super.stop(bundleContext);
  }

}
