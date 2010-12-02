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
package org.sakaiproject.nakamura.jaxrs;

import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.core.ThreadLocalResteasyProviderFactory;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.sakaiproject.nakamura.api.jaxrs.JaxRestService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * The <code>ResteasyServlet</code> TODO
 *
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.component immediate="true"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.property name="service.description" value="JAX-RS servlet based on Rest Easy"
 * @scr.property name="sling.servlet.paths" value="/system/jaxrs"
 */

public class ResteasyServlet extends HttpServletDispatcher {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  /**
   *
   */
  public static final String SERVLET_PATH = "/system/jaxrs";
  /**
   *
   */
  public static final String SERVLET_URL_MAPPING = SERVLET_PATH + "/*";

  /**
   *
   */
  private ServletConfig servletConfig;
  /**
   *
   */
  private ServletContext servletContext;
  /**
   *
   */
  private Registry registry;
  /**
   *
   */
  private ResteasyProviderFactory factory = new ResteasyProviderFactory();

  /**
   * Tracks JAXRS beans while alive.
   */
  private ServiceTracker jaxRsResourceTracker;

  private ComponentContext componentContext;

  /**
   * {@inheritDoc}
   *
   * @see org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher#init(javax.servlet.ServletConfig)
   */
  public void init(ServletConfig servletConfig) throws ServletException {
    // Wrap the servlet config and context objects, since some http service impls (e.g.
    // pax) don't
    // handle these parts of the servlet spec correctly
    this.servletConfig = new ServletConfigWrapper(servletConfig);
    this.servletContext = this.servletConfig.getServletContext();

    bootstrap(componentContext);

   
    super.init(this.servletConfig);
  }

  public ServletConfig getServletConfig() {
    return servletConfig;
  }

  /**
   * {@inheritDoc}
   *
   * @see javax.servlet.GenericServlet#getServletContext()
   */
  public ServletContext getServletContext() {
    return servletContext;
  }

  /**
   * Component Activation
   *
   * @param context
   */
  protected void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
   

  }

  /**
   * Deactivate this component
   *
   * @param context
   */
  protected void deactivate(ComponentContext context) {
    jaxRsResourceTracker.close();

  }

  /**
   * @param componentContext
   *
   */
  private void bootstrap(ComponentContext componentContext) {
    ClassLoader bundleClassloader = this.getClass().getClassLoader();
    ClassLoader contextClassloader = Thread.currentThread().getContextClassLoader();
    ResteasyProviderFactory defaultInstance = null;
    try {
      Thread.currentThread().setContextClassLoader(bundleClassloader);
      defaultInstance = ResteasyProviderFactory.getInstance();
    } finally {
      Thread.currentThread().setContextClassLoader(contextClassloader);
    }
    ResteasyProviderFactory.setInstance(new ThreadLocalResteasyProviderFactory(
        defaultInstance));

    servletContext.setAttribute(ResteasyProviderFactory.class.getName(), factory);
    dispatcher = new SynchronousDispatcher(factory);
    registry = dispatcher.getRegistry();
    servletContext.setAttribute(Dispatcher.class.getName(), dispatcher);
    servletContext.setAttribute(Registry.class.getName(), registry);
    RegisterBuiltin.register(factory);
    // Can't seem to get the resteasy input stream provider to work
    factory.addMessageBodyWriter(new InputStreamProvider());
   

    // add in the trackers and register any outstanding beans
   
    BundleContext context = componentContext.getBundleContext();
    ServiceReference[] jaxRsRefs = null;
    try {
      jaxRsRefs = context.getAllServiceReferences(JaxRestService.class.getName(), null);
    } catch (InvalidSyntaxException e) {
      e.printStackTrace();
    }
    if (jaxRsRefs != null) {
      for (ServiceReference jaxRsRef : jaxRsRefs) {
        registry.addSingletonResource(context.getService(jaxRsRef));
      }
    }

    // Track JAX-RS Resources that are added and removed
    jaxRsResourceTracker = new ServiceTracker(componentContext.getBundleContext(),
        JaxRestService.class.getName(), null) {
      /**
       * {@inheritDoc}
       *
       * @see org.osgi.util.tracker.ServiceTracker#addingService(org.osgi.framework.ServiceReference)
       */
      @Override
      public Object addingService(ServiceReference reference) {
        JaxRestService jaxRsResource = (JaxRestService) context.getService(reference);
        registry.addSingletonResource(jaxRsResource);
        return super.addingService(reference);
      }

      /**
       * {@inheritDoc}
       *
       * @see org.osgi.util.tracker.ServiceTracker#removedService(org.osgi.framework.ServiceReference,
       *      java.lang.Object)
       */
      @Override
      public void removedService(ServiceReference reference, Object service) {
        registry.removeRegistrations(context.getService(reference).getClass());
        super.removedService(reference, service);
      }
    };
    jaxRsResourceTracker.open();

   
  }

}
