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

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

import org.osgi.framework.BundleContext;
import org.sakaiproject.nakamura.api.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Provides configuration support.
 */
public abstract class AbstractOsgiModule extends AbstractModule {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOsgiModule.class);
  private Set<Class<?>> exports = Sets.newHashSet();
  protected BundleContext bundleContext;

  public AbstractOsgiModule(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }

  /**
   * {@inheritDoc}
   * 
   * @see com.google.inject.AbstractModule#configure()
   */
  @Override
  protected void configure() {
    OsgiServiceProvider<ConfigurationService> configurationServiceProvider = new OsgiServiceProvider<ConfigurationService>(
        ConfigurationService.class, getBundleContext());
    ConfigurationService configurationService = configurationServiceProvider.get();
    Map<String, String> config = configurationService.getProperties();
    if (config != null) {
      Names.bindProperties(this.binder(), config);
    } else {
      LOGGER.warn("No Configuration Properties found");
    }

  }

  /**
   * @return the current bundle context
   */
  protected BundleContext getBundleContext() {
    return bundleContext;
  }

  /**
   * @param class1
   * @return
   */
  protected <T> Class<T> export(Class<T> clazz) {
    exports.add(clazz);
    return clazz;
  }

  /**
   * @param class1
   * @return
   */
  protected <T> OsgiServiceProvider<T> importService(Class<T> class1) {
    return new OsgiServiceProvider<T>(class1, getBundleContext());
  }

  /**
   * @return the exports
   */
  public Set<Class<?>> getExports() {
    return exports;
  }

}
