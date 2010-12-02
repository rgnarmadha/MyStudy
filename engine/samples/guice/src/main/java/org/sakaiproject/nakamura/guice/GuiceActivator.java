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
import com.google.inject.Guice;
import com.google.inject.Injector;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/**
 * The Guice Activator activates a guice based bundle and exports the the
 * services listed in the list annotated with the SerivceExportList annotation.
 * Services in the list may optionally be annotated with the
 * ServiceExportDescription annotation to give more precise control over how the
 * service is exported to OSGi.
 */
public abstract class GuiceActivator implements BundleActivator {

  protected Injector injector;
  private Set<Class<?>> services;
  private Set<RequiresStop> stoppers;

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext bundleContext) throws Exception {
    AbstractOsgiModule module = getModule(bundleContext);
    injector = Guice.createInjector(module);

    services = module.getExports();
    stoppers = new HashSet<RequiresStop>();
    for (Class<?> serviceClass : services) {
      Object service = injector.getInstance(serviceClass);
      if (service instanceof RequiresStop) {
        stoppers.add((RequiresStop) service);
      }
      if (serviceClass.isAnnotationPresent(ServiceExportDescription.class)) {
        ServiceExportDescription ks = serviceClass.getAnnotation(ServiceExportDescription.class);
        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
        Class<?>[] serviceClasses = ks.serviceClasses();
        String[] serviceClassNames = new String[serviceClasses.length];
        int i = 0;
        for (Class<?> c : serviceClasses) {
          serviceClassNames[i++] = c.getName();
        }
        dictionary.put(Constants.OBJECTCLASS, serviceClassNames);
        dictionary.put(Constants.SERVICE_DESCRIPTION, ks.serviceDescription());
        dictionary.put(Constants.SERVICE_VENDOR, ks.seviceVendor());
        bundleContext.registerService(serviceClassNames, service, dictionary);
      } else {
        Class<?>[] implementedInterfaces = serviceClass.getInterfaces();
        Set<Class<?>> interfaces = Sets.newHashSet();
        if (serviceClass.isInterface()) {
          interfaces.add(serviceClass);
        }
        for (Class<?> c : implementedInterfaces) {
          interfaces.add(c);
        }
        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
        String[] serviceClassNames = new String[interfaces.size()];
        int i = 0;
        for (Class<?> c : interfaces) {
          serviceClassNames[i++] = c.getName();
        }
        dictionary.put(Constants.OBJECTCLASS, serviceClassNames);
        dictionary.put(Constants.SERVICE_DESCRIPTION, serviceClass.getName());
        dictionary.put(Constants.SERVICE_VENDOR, "The Sakai Foundation");
        bundleContext.registerService(serviceClassNames, service, dictionary);
      }
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext bundleContext) throws Exception {
    for (RequiresStop o : stoppers) {
      o.stop();
    }
    stoppers = null;
  }

  /**
   * @return
   */
  protected abstract AbstractOsgiModule getModule(BundleContext bundleContext);

}
