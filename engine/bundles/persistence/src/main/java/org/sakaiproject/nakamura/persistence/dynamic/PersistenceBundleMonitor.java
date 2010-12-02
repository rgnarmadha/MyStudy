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

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.eclipse.persistence.internal.jpa.deployment.osgi.BundleProxyClassLoader;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Monitors the bundles, but must be a singleton 
 */

@SuppressWarnings(value={"CD_CIRCULAR_DEPENDENCY"},justification="Unable to avoid and get classloading to work")
public class PersistenceBundleMonitor implements BundleActivator, SynchronousBundleListener {

  private static final Logger LOG = LoggerFactory.getLogger(PersistenceBundleMonitor.class);

  public static final String SAKAI_JPA_PERSISTENCE_UNITS_BUNDLE_HEADER = "Sakai-JPA-PersistenceUnits";

  // these maps are used to retrieve the classloader used for different bundles
  private Map<String, List<Bundle>> puToBundle = Collections
      .synchronizedMap(new HashMap<String, List<Bundle>>());
  private Map<Bundle, String[]> bundleToPUs = Collections
      .synchronizedMap(new HashMap<Bundle, String[]>());
  private Set<Bundle> allPuBundles = Collections.synchronizedSet(new HashSet<Bundle>());

  private ClassLoader contextClassLoader;
  
  /**
   * 
   */
  public PersistenceBundleMonitor() {
  }

  /**
   * Add a bundle to the list of bundles managed by this persistence provider
   * The bundle is indexed so it's classloader can be accessed
   * 
   * @param bundle
   * @param persistenceUnitNames
   */
  public void addBundle(Bundle bundle, String[] persistenceUnitNames) {
    for (int i = 0; i < persistenceUnitNames.length; i++) {
      String name = persistenceUnitNames[i];
      List<Bundle> list = puToBundle.get(name);
      if (list == null) {
        list = new ArrayList<Bundle>();
        puToBundle.put(name, list);
      }
      list.add(bundle);
    }
    bundleToPUs.put(bundle, persistenceUnitNames);
    allPuBundles.add(bundle);
  }

  /**
   * Removed a bundle from the list of bundles managed by this persistence
   * provider This typically happens on deactivation.
   * 
   * @param bundle
   */
  public void removeBundle(Bundle bundle) {
    String[] persistenceUnitNames = bundleToPUs.remove(bundle);
    if (persistenceUnitNames != null) {
      for (int i = 0; i < persistenceUnitNames.length; i++) {
        String name = persistenceUnitNames[i];
        List<Bundle> bundles = puToBundle.get(name);
        if (bundles != null) {
          bundles.remove(bundle);
          if (bundles.size() == 0) {
            puToBundle.remove(name);
          }
        }
      }
    }
    allPuBundles.remove(bundle);
  }

  /**
   * Simply add bundles to our bundle list as they start and remove them as they
   * stop
   */
  public void bundleChanged(BundleEvent event) {
    switch (event.getType()) {
    case BundleEvent.STARTING:
      registerBundle(event.getBundle());
      break;

    case BundleEvent.STOPPING:
      deregisterBundle(event.getBundle());
      break;
    }
  }

  /**
   * On start, we do two things We register a listener for bundles and we start
   * our JPA server
   */
  @SuppressWarnings(value={"DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED"},justification="In OSGi Environment")
  public void start(BundleContext bundleContext) throws Exception {
    LOG.info("Starting to monitor for persistence bundles");
    contextClassLoader = new BundleProxyClassLoader(bundleContext.getBundle());
    bundleContext.addBundleListener(this);
    Bundle bundles[] = bundleContext.getBundles();
    for (int i = 0; i < bundles.length; i++) {
      Bundle bundle = bundles[i];
      registerBundle(bundle);
    }  }

  /**
   * Store a reference to a bundle as it is started so the bundle can be
   * accessed later
   * 
   * @param bundle
   */
  private void registerBundle(Bundle bundle) {
    if ((bundle.getState() & (Bundle.STARTING | Bundle.ACTIVE)) != 0) {
      try {
        String[] persistenceUnitNames = getPersistenceUnitNames(bundle);
        if (persistenceUnitNames != null) {
          addBundle(bundle, persistenceUnitNames);
        }
      } catch (Exception e) {
        AbstractSessionLog.getLog().logThrowable(SessionLog.WARNING, e);
      }
    }
  }

  private String[] getPersistenceUnitNames(Bundle bundle) {
    String names = (String) bundle.getHeaders().get(SAKAI_JPA_PERSISTENCE_UNITS_BUNDLE_HEADER);
    if (names != null) {
      return names.split(",");
    } else {
      return null;
    }
  }

  private void deregisterBundle(Bundle bundle) {
    removeBundle(bundle);
  }

  public void stop(BundleContext context) throws Exception {
    context.removeBundleListener(this);
  }

  public BundleGatheringResourceFinder getBundleResourceFinder() {
    if (allPuBundles.size() == 0) {
      LOG.warn("No bundles found");
      return null;
    }
    return new BundleGatheringResourceFinder(allPuBundles);
  }

  @SuppressWarnings(justification="OSGi Environment", value={"DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED"})
  public ClassLoader getAmalgamatedClassloader() throws IOException {
    BundleGatheringResourceFinder currentLoader = getBundleResourceFinder();
    if (currentLoader == null) {
      LOG.warn("No persistence xmls");
      return null;
    }

    LOG.debug("Looking for persistence.xmls");
    List<URL> persistences = currentLoader.getResources("META-INF/persistence.xml");
    List<URL> orms = currentLoader.getResources("META-INF/orm.xml");
    AmalgamatingClassloader loader = new AmalgamatingClassloader(contextClassLoader,this);
    for (URL persistence : persistences) {
      loader.importPersistenceXml(persistence);
    }
    for (URL orm : orms) {
      loader.importOrmXml(orm);
    }
    return loader;
  }

}
