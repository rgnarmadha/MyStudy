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

import com.google.inject.Provider;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Create a generic Guice provider that binds to a service in OSGi
 */
public class OsgiServiceProvider<T> implements Provider<T>, ServiceListener,
    InvocationHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(OsgiServiceProvider.class);
  private BundleContext bundleContext;
  private T service;
  private String serviceName;
  private T osgiService;
  private ServiceEvent lastEvent;

  /**
   * @param the
   *          interface representing the service
   * @param bundleContext
   *          the bundle context of this bundle
   * @throws InvalidSyntaxException
   */
  @SuppressWarnings("unchecked")
  public OsgiServiceProvider(Class<T> serviceClass, BundleContext bundleContext) {
    this.bundleContext = bundleContext;

    // convert the service class into a reference, since the provision of the service
    // class
    // may change.
    try {
      bundleContext.addServiceListener(this, "(&(objectclass="
          + serviceClass.getName() + "))");
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException("Listener syntax was wrong " + e.getMessage(), e);
    }
    
    Class[] c = serviceClass.getInterfaces();
    if ( serviceClass.isInterface() ) {
      Class[] ciall = new Class[c.length+1];
      ciall[0] = serviceClass;
      int i = 1;
      for ( Class ci : c ) {
        ciall[i++] = ci;
      }
      c = ciall;
    }
    service = (T) Proxy.newProxyInstance(serviceClass.getClassLoader(), c, this);
    serviceName = serviceClass.getName();
    try {
      ServiceReference serviceReference = bundleContext.getServiceReference(serviceName);
      osgiService = (T) bundleContext.getService(serviceReference);      
    } catch ( Exception e) {
      LOGGER.info("Service "+serviceName+"not yet available, will register when it is");
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see com.google.inject.Provider#get()
   */
  public T get() {
    return service;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
   */
  @SuppressWarnings("unchecked")
  public void serviceChanged(ServiceEvent event) {
    lastEvent = event;
    switch (event.getType()) {
    case ServiceEvent.MODIFIED:
    case ServiceEvent.REGISTERED:
      LOGGER.info("Registered "+event.getServiceReference());
      ServiceReference serviceReference = bundleContext.getServiceReference(serviceName);
      osgiService = (T) bundleContext.getService(serviceReference);
      break;
    case ServiceEvent.UNREGISTERING:
      LOGGER.info("UnRegistered "+event.getServiceReference());
      osgiService = null;
      break;
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object,
   *      java.lang.reflect.Method, java.lang.Object[])
   */
  public Object invoke(Object object, Method method, Object[] args) throws Throwable {
    if (osgiService == null) {
      throw new IllegalStateException("Service "+serviceName+" is not yet available, last Event was  :"
          + lastEvent);
    }
    return method.invoke(osgiService, args);
  }
}
