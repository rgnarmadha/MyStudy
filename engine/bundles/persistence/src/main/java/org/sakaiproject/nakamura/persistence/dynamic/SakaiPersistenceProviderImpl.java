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

package org.sakaiproject.nakamura.persistence.dynamic;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.persistence.SakaiPersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.osgi.ProviderTrackerCustomizer;


@Component(immediate=true)
@Service(value=SakaiPersistenceProvider.class)
@Property(name=ProviderTrackerCustomizer.PERSISTENCE_PROVIDER, value="org.sakaiproject.nakamura.persistence.dynamic.SakaiPersistenceProvider")
public class SakaiPersistenceProviderImpl extends PersistenceProvider implements SakaiPersistenceProvider {

  private static Logger LOG = LoggerFactory.getLogger(SakaiPersistenceProviderImpl.class);

  private ClassLoader amalgamatedClassloader;

  private PersistenceBundleMonitor persistenceBundleMonitor = new PersistenceBundleMonitor();

  public SakaiPersistenceProviderImpl() {
  }

  protected void activate(ComponentContext componentContext) throws Exception {
    persistenceBundleMonitor.start(componentContext.getBundleContext());
    try {
      amalgamatedClassloader = persistenceBundleMonitor.getAmalgamatedClassloader();
    } catch (IOException e) {
      LOG.error("Unable to create amalgamated classloader", e);
    }
    initializationHelper = new SakaiPersistenceInitializationHelper(
        amalgamatedClassloader);
  }

  protected void deactivate(ComponentContext componentContext) throws Exception {
    persistenceBundleMonitor.stop(componentContext.getBundleContext());
  }
  
  

  @SuppressWarnings({ "unchecked", "rawtypes" })
  @Override
  protected EntityManagerFactory createEntityManagerFactory(String emName,
      Map properties, ClassLoader classLoader) {
    LOG.info("Creating entity manager factory");
    Map nonNullProperties = (properties == null) ? new HashMap() : properties;
    String name = emName;
    if (name == null) {
      name = "";
    }
    nonNullProperties.put(PersistenceUnitProperties.CLASSLOADER, amalgamatedClassloader);
    LOG.info("Persistence Initialization Helper is: " + amalgamatedClassloader);

    ClassLoader saved = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(amalgamatedClassloader);
    try {
      EntityManagerFactory result = super.createEntityManagerFactory(emName, properties,
          amalgamatedClassloader);
      return result;
    } catch (Throwable ex) {
      LOG.error("Error creating Entity Manager with a a custom classloader of "
          + amalgamatedClassloader + " cause:" + ex.getMessage(), ex);
      throw new RuntimeException("Error creating Entity Manager with a a custom classloader of "
          + amalgamatedClassloader + " cause:" + ex.getMessage(), ex);
    } finally {
      Thread.currentThread().setContextClassLoader(saved);
    }
  }

}
