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
package org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic;

import org.apache.jackrabbit.core.security.authorization.acl.RulesPrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.jackrabbit.core.security.principal.PrincipalProviderRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class PrincipalProviderRegistryManagerImpl extends ServiceTracker implements
    PrincipalProviderRegistryManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrincipalProviderRegistryManagerImpl.class);
  private List<PrincipalProvider> testServices = new ArrayList<PrincipalProvider>();
  private Map<DynamicProviderRegistryImpl, DynamicProviderRegistryImpl> serviceListeners = new HashMap<DynamicProviderRegistryImpl, DynamicProviderRegistryImpl>();
  private BundleContext bundleContext;

  /**
   * @param bundleContext
   */
  public PrincipalProviderRegistryManagerImpl(BundleContext bundleContext) {
    super(bundleContext, PrincipalProvider.class.getName(),null);
    this.bundleContext = bundleContext;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic.PrincipalProviderRegistryManager#getPrincipalProvider(org.apache.jackrabbit.core.security.principal.PrincipalProvider)
   */
  public PrincipalProviderRegistry getPrincipalProvider(PrincipalProvider defaultPrincipalProvider) {
    Object[] services = getServices();
    PrincipalProvider[] providers = null;
    if (services != null) {
      providers = new PrincipalProvider[services.length];
      System.arraycopy(services, 0, providers, 0, services.length);
    }
    DynamicProviderRegistryImpl dpp = new DynamicProviderRegistryImpl(defaultPrincipalProvider, providers, testServices);
    LOGGER.info("Creating Principal provider registry and keeping reference, if there are lots of these messages in the log, there is a memory leak in progress.");
    serviceListeners .put(dpp,dpp);
    return dpp;
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
   */
  @Override
  public Object addingService(ServiceReference reference) {
    PrincipalProvider pp = (PrincipalProvider) bundleContext.getService(reference);
    if ( pp != null ) {
      synchronized (serviceListeners) {
        for ( DynamicProviderRegistryImpl d : serviceListeners.values() ) {
          d.addService(pp);
        }
      }
    }
    return super.addingService(reference);
  }
  /**
   * {@inheritDoc}
   * @see org.osgi.util.tracker.ServiceTracker#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
   */
  @Override
  public void modifiedService(ServiceReference reference, Object service) {
      synchronized (serviceListeners) {
        for ( DynamicProviderRegistryImpl d : serviceListeners.values() ) {
          d.updateService((PrincipalProvider) service);
        }
      }
    super.modifiedService(reference, service);
  }

  /**
   * {@inheritDoc}
   * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
   */
  @Override
  public void removedService(ServiceReference reference, Object service) {
      synchronized (serviceListeners) {
        for ( DynamicProviderRegistryImpl d : serviceListeners.values() ) {
          d.removeService((PrincipalProvider) service);
        }
      }
    super.removedService(reference, service);
  }


  /**
   * @param rulesPrincipalProvider
   */
  protected void addProvider(RulesPrincipalProvider rulesPrincipalProvider) {
    testServices.add(rulesPrincipalProvider);
  }




}
