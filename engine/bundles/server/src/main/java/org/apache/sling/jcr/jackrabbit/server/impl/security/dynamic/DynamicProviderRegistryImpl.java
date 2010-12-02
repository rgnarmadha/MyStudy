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

import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.jackrabbit.core.security.principal.ProviderRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class DynamicProviderRegistryImpl extends ProviderRegistryImpl {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicProviderRegistryImpl.class);
  private Map<String, PrincipalProvider> providers = new LinkedHashMap<String, PrincipalProvider>();
  /**
   * @param defaultPrincipalProvider
   * @param testServices
   * @param objects
   */
  public DynamicProviderRegistryImpl(PrincipalProvider defaultPrincipalProvider, PrincipalProvider[] serviceBasedProviders, List<PrincipalProvider> testServices) {
    super(defaultPrincipalProvider);
    if ( serviceBasedProviders != null ) {
      for ( PrincipalProvider p : serviceBasedProviders ) {
        LOGGER.info("Adding Principal Provider {} ",p);
        providers.put(p.getClass().getName(),p);
      }
    }
    if ( testServices != null ) {
      for ( PrincipalProvider p : testServices ) {
        LOGGER.info("Adding Test {} ",p);
        providers.put(p.getClass().getName(),p);
      }
    }
  }

  /**
   * {@inheritDoc}
   * @see org.apache.jackrabbit.core.security.principal.ProviderRegistryImpl#getProvider(java.lang.String)
   */
  @Override
  public PrincipalProvider getProvider(String className) {
    if (providers.containsKey(className)) {
      return providers.get(className);
    }
    return super.getProvider(className);
  }
  /**
   * {@inheritDoc}
   * @see org.apache.jackrabbit.core.security.principal.ProviderRegistryImpl#getProviders()
   */
  @Override
  public PrincipalProvider[] getProviders() {
    synchronized (providers) {
      PrincipalProvider[] pp = super.getProviders();
      Collection<PrincipalProvider> ppc = providers.values();
      PrincipalProvider[] ppl = ppc.toArray(new PrincipalProvider[ppc.size()]);
      PrincipalProvider[] ppf = new PrincipalProvider[pp.length+ppl.length];
      System.arraycopy(pp, 0, ppf, 0, pp.length);
      System.arraycopy(ppl, 0, ppf, pp.length, ppl.length);
      return ppf;
    }
  }

  /**
   * @param pp
   */
  public void addService(PrincipalProvider pp) {
    synchronized (providers) {
      if ( pp != null) {
        providers.put(pp.getClass().getName(), pp);
      }
    }
  }

  /**
   * @param service
   */
  public void updateService(PrincipalProvider pp) {
     addService(pp);
  }

  /**
   * @param service
   */
  public void removeService(PrincipalProvider pp) {
    synchronized (providers) {
      if ( pp != null) {
        providers.remove(pp.getClass().getName());
      }
    }
  }




}
