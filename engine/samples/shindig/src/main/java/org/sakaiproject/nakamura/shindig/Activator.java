/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.shindig;

import org.apache.shindig.auth.AuthenticationServletFilter;
import org.apache.shindig.common.servlet.GuiceServletContextListener;
import org.apache.shindig.gadgets.servlet.ConcatProxyServlet;
import org.apache.shindig.gadgets.servlet.GadgetRenderingServlet;
import org.apache.shindig.gadgets.servlet.JsServlet;
import org.apache.shindig.gadgets.servlet.MakeRequestServlet;
import org.apache.shindig.gadgets.servlet.OAuthCallbackServlet;
import org.apache.shindig.gadgets.servlet.ProxyServlet;
import org.apache.shindig.gadgets.servlet.RpcServlet;
import org.apache.shindig.social.opensocial.service.DataServiceServlet;
import org.apache.shindig.social.opensocial.service.JsonRpcServlet;

import org.ops4j.pax.web.service.WebContainer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map.Entry;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;

/**
 * Activator for making Apache Shindig into an OSGi bundle. Registers all the
 * elements (context params, filters, listeners, servlets) from the web.xml
 * found in server-shindig.
 */
public class Activator implements BundleActivator {
  private static final Logger LOG = LoggerFactory.getLogger(Activator.class);
  private static final HashMap<String, Class<? extends HttpServlet>> servletsToRegister = new HashMap<String, Class<? extends HttpServlet>>();
  static {
    servletsToRegister.put("/gadgets/js/*", JsServlet.class);
    servletsToRegister.put("/gadgets/proxy/*", ProxyServlet.class);
    servletsToRegister.put("/gadgets/makeRequest", MakeRequestServlet.class);
    servletsToRegister.put("/gadgets/concat", ConcatProxyServlet.class);
    servletsToRegister.put("/gadgets/oauthcallback", OAuthCallbackServlet.class);
    servletsToRegister.put("/gadgets/ifr", GadgetRenderingServlet.class);
    servletsToRegister.put("/gadgets/metadata", RpcServlet.class);
    servletsToRegister.put("/social/rest/*", DataServiceServlet.class);
    servletsToRegister.put("/social/rpc/*", JsonRpcServlet.class);
  }

  private ServiceTracker paxWebTracker;
  private Filter authFilter;
  private EventListener guiceListener;

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  public void start(BundleContext context) throws Exception {
    paxWebTracker = new ServiceTracker(context, WebContainer.class.getName(), null) {
      @Override
      public Object addingService(ServiceReference reference) {
        /**
         * Note: All settings and configurations below are based on the web.xml
         * from shindig-server
         */
        // get the web container and create a default context
        LOG.debug("Activating Shindig...");
        WebContainer webContainer = (WebContainer) context.getService(reference);
        HttpContext context = webContainer.createDefaultHttpContext();

        // setup context parameters
        LOG.debug("Setting up guice-modules context parameter.");
        Hashtable<String, String> contextParams = new Hashtable<String, String>();
        contextParams.put("guice-modules", "org.apache.shindig.common.PropertiesModule:"
            + "org.apache.shindig.gadgets.DefaultGuiceModule:"
            + "org.apache.shindig.social.core.config.SocialApiGuiceModule:"
            + "org.apache.shindig.gadgets.oauth.OAuthModule:"
            + "org.apache.shindig.common.cache.ehcache.EhCacheModule");
        webContainer.setContextParam(contextParams, context);

        // register listeners first so that it can load a Guice injector into
        // the context
        LOG.debug("Registering Guice servlet context listener.");
        guiceListener = new GuiceServletContextListener();
        webContainer.registerEventListener(guiceListener, context);

        // register filters
        LOG.debug("Registering authentication servlet filter.");
        authFilter = new AuthenticationServletFilter();
        String[] urlPatterns = { "/social/*", "/gadgets/ifr", "/gadgets/makeRequest" };
        String[] servletNames = null;
        Dictionary<String, String> initParams = null;
        webContainer.registerFilter(authFilter, urlPatterns, servletNames, initParams, context);

        // register servlets
        LOG.debug("Registering servlets...");
        for (Entry<String, Class<? extends HttpServlet>> registrant : servletsToRegister.entrySet()) {
          try {
            LOG.debug(" +registering {}", registrant.getKey());
            webContainer.registerServlet(registrant.getKey(), registrant.getValue().newInstance(),
                null, context);
          } catch (Exception e) {
            // Just log errors. If more can be done here, figure it out.
            LOG.error(e.getMessage(), e);
          }
        }
        return super.addingService(reference);
      }

      @Override
      public void removedService(ServiceReference reference, Object service) {
        WebContainer webContainer = (WebContainer) service;

        // unregister servlets
        LOG.debug("Unregistering servlets...");
        for (String servletAlias : servletsToRegister.keySet()) {
          LOG.debug(" +unregistering {}", servletAlias);
          webContainer.unregister(servletAlias);
        }

        // unregister filters
        LOG.debug("Unregistering authentication servlet filter.");
        webContainer.unregisterFilter(authFilter);

        // unregister listeners
        LOG.debug("Unregistering Guice servlet context listener.");
        webContainer.unregisterEventListener(guiceListener);

        super.removedService(reference, service);
      }
    };
    paxWebTracker.open();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  public void stop(BundleContext context) throws Exception {
    paxWebTracker.close();
  }
}
