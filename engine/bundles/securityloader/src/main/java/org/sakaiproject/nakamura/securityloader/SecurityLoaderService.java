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
package org.sakaiproject.nakamura.securityloader;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.engine.SlingSettingsService;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.servlets.post.Modification;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.sakaiproject.nakamura.api.user.AuthorizableEvent.Operation;
import org.sakaiproject.nakamura.api.user.AuthorizableEventUtil;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;

/**
 * Load Security related items. Much of this code is based on the ContentLoader in sling,
 * no point in creating something different just for the sake of it.
 */
@Component(metatype = true)
public class SecurityLoaderService implements SynchronousBundleListener {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(SecurityLoaderService.class);

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "Sakai Security Loader Implementation")
  static final String SERVICE_DESCRIPTION = "service.description";

  public static final String PROPERTY_SECURITY_LOADED = "security-loaded";
  private static final String PROPERTY_SECURITY_LOADED_AT = "security-load-time";
  private static final String PROPERTY_SECURITY_LOADED_BY = "security-loaded-by";
  private static final String PROPERTY_SECURITY_UNLOADED_AT = "security-unload-time";
  private static final String PROPERTY_SECURITY_UNLOADED_BY = "security-unloaded-by";
  public static final String PROPERTY_SECURITY_PATHS = "uninstall-paths";

  public static final String BUNDLE_SECURITY_NODE = "/var/sling/bundle-security";

  /**
   * To be used for the encryption. E.g. for passwords in
   * {@link javax.jcr.SimpleCredentials#getPassword()}  SimpleCredentials}
   */
  private static final String DEFAULT_PASSWORD_DIGEST_ALGORITHM = "sha1";

  @Property(value = DEFAULT_PASSWORD_DIGEST_ALGORITHM)
  private static final String PROP_PASSWORD_DIGEST_ALGORITHM = "password.digest.algorithm";

  @Reference
  protected SlingSettingsService settingsService;

  /**
   * The JCR Repository we access to resolve resources
   */
  @Reference
  protected SlingRepository repository;

  @Reference(bind = "bindEventAdmin", unbind = "bindEventAdmin")
  protected EventAdmin eventAdmin;

  @Reference
  protected AuthorizablePostProcessService authorizablePostProcessService;;

  /**
   * List of currently updated bundles.
   */
  private final Set<String> updatedBundles = new HashSet<String>();

  /**
   * The initial security loader which is called to load initial security up into the
   * repository when the providing bundle is installed.
   */
  private SecurityLoader initialSecurityLoader;

  /**
   * The id of the current instance
   */
  private String slingId;

  private String passwordDigestAlgoritm;

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.framework.BundleListener#bundleChanged(org.osgi.framework.BundleEvent)
   */
  public void bundleChanged(BundleEvent event) {
    //
    // NOTE:
    // This is synchronous - take care to not block the system !!
    //

    Session session = null;
    switch (event.getType()) {
    case BundleEvent.STARTING:
      // register content when the bundle content is available
      // as node types are registered when the bundle is installed
      // we can safely add the content at this point.
      try {
        session = this.getSession();
        final boolean isUpdate = this.updatedBundles.remove(event.getBundle()
            .getSymbolicName());
        initialSecurityLoader.registerBundle(session, event.getBundle(), isUpdate);
      } catch (Throwable t) {
        LOGGER.error("bundleChanged: Problem loading initial security content of bundle "
            + event.getBundle().getSymbolicName() + " ("
            + event.getBundle().getBundleId() + ")", t);
      } finally {
        this.ungetSession(session);
      }
      break;
    case BundleEvent.UPDATED:
      // we just add the symbolic name to the list of updated bundles
      // we will use this info when the new start event is triggered
      this.updatedBundles.add(event.getBundle().getSymbolicName());
      break;
    case BundleEvent.UNINSTALLED:
      try {
        session = this.getSession();
        initialSecurityLoader.unregisterBundle(session, event.getBundle());
      } catch (Throwable t) {
        LOGGER.error("bundleChanged: Problem unloading initial content of bundle "
            + event.getBundle().getSymbolicName() + " ("
            + event.getBundle().getBundleId() + ")", t);
      } finally {
        this.ungetSession(session);
      }
      break;
    }
  }

  /** Activates this component, called by SCR before registering as a service */
  protected void activate(ComponentContext componentContext) {


    this.slingId = this.settingsService.getSlingId();
    this.initialSecurityLoader = new Loader(this, authorizablePostProcessService);

    componentContext.getBundleContext().addBundleListener(this);
    Dictionary<?, ?> props = componentContext.getProperties();

    Object propValue = props.get(PROP_PASSWORD_DIGEST_ALGORITHM);
    if (propValue instanceof String) {
      passwordDigestAlgoritm = (String)propValue;
    } else {
      passwordDigestAlgoritm = DEFAULT_PASSWORD_DIGEST_ALGORITHM;
    }

    Session session = null;
    try {
      session = this.getSession();
      this.createRepositoryPath(session, BUNDLE_SECURITY_NODE);
      LOGGER.debug("Activated - attempting to load content from all "
          + "bundles which are neither INSTALLED nor UNINSTALLED");

      int ignored = 0;
      Bundle[] bundles = componentContext.getBundleContext().getBundles();
      for (Bundle bundle : bundles) {
        if ((bundle.getState() & (Bundle.INSTALLED | Bundle.UNINSTALLED)) == 0) {
          // load content for bundles which are neither INSTALLED nor
          // UNINSTALLED
          try {
            initialSecurityLoader.registerBundle(session, bundle, false);
          } catch (Throwable t) {
            LOGGER.error("Problem loading initial content of bundle "
                + bundle.getSymbolicName() + " (" + bundle.getBundleId() + ")", t);
          }
        } else {
          ignored++;
        }

      }

      LOGGER
          .info(
              "Out of {} bundles, {} were not in a suitable state for initial content loading",
              bundles.length, ignored);

    } catch (Throwable t) {
      LOGGER.error("activate: Problem while loading initial content and"
          + " registering mappings for existing bundles", t);
    } finally {
      this.ungetSession(session);
    }
  }

  /** Deativates this component, called by SCR to take out of service */
  protected void deactivate(ComponentContext componentContext) {
    componentContext.getBundleContext().removeBundleListener(this);

    if (this.initialSecurityLoader != null) {
      this.initialSecurityLoader.dispose();
      this.initialSecurityLoader = null;
    }
  }

  /** Returns the JCR repository used by this service. */
  protected SlingRepository getRepository() {
    return repository;
  }

  /**
   * Returns an administrative session to the default workspace.
   */
  private Session getSession() throws RepositoryException {
    return getRepository().loginAdministrative(null);
  }

  /**
   * Return the administrative session and close it.
   */
  private void ungetSession(final Session session) {
    if (session != null) {
      try {
        session.logout();
      } catch (Throwable t) {
        LOGGER.error("Unable to log out of session: " + t.getMessage(), t);
      }
    }
  }

  /**
   * Return the bundle content info and make an exclusive lock.
   *
   * @param session
   * @param bundle
   * @return The map of bundle content info or null.
   * @throws RepositoryException
   */
  public Map<String, Object> getBundleContentInfo(final Session session,
      final Bundle bundle, boolean create) throws RepositoryException {
    final String nodeName = bundle.getSymbolicName();
    final Node parentNode = (Node) session.getItem(BUNDLE_SECURITY_NODE);
    if (!parentNode.hasNode(nodeName)) {
      if (!create) {
        return null;
      }
      try {
        final Node bcNode = parentNode.addNode(nodeName, "nt:unstructured");
        bcNode.addMixin("mix:lockable");
        session.save();
      } catch (RepositoryException re) {
        // for concurrency issues (running in a cluster) we ignore exceptions
        LOGGER.warn("Unable to create node " + nodeName, re);
        session.refresh(true);
      }
    }
    final Node bcNode = parentNode.getNode(nodeName);
    if (bcNode.isLocked()) {
      return null;
    }
    try {
      LockManager lockManager = session.getWorkspace().getLockManager();
      lockManager
          .lock(bcNode.getPath(), false, true, Long.MAX_VALUE, session.getUserID());
    } catch (LockException le) {
      return null;
    }
    final Map<String, Object> info = new HashMap<String, Object>();
    if (bcNode.hasProperty(PROPERTY_SECURITY_LOADED)) {
      info.put(PROPERTY_SECURITY_LOADED, bcNode.getProperty(PROPERTY_SECURITY_LOADED)
          .getBoolean());
    } else {
      info.put(PROPERTY_SECURITY_LOADED, false);
    }
    if (bcNode.hasProperty(PROPERTY_SECURITY_PATHS)) {
      final Value[] values = bcNode.getProperty(PROPERTY_SECURITY_PATHS).getValues();
      final String[] s = new String[values.length];
      for (int i = 0; i < values.length; i++) {
        s[i] = values[i].getString();
      }
      info.put(PROPERTY_SECURITY_PATHS, s);
    }
    return info;
  }

  public void unlockBundleContentInfo(final Session session, final Bundle bundle,
      final boolean contentLoaded, final List<String> createdNodes)
      throws RepositoryException {
    final String nodeName = bundle.getSymbolicName();
    final Node parentNode = (Node) session.getItem(BUNDLE_SECURITY_NODE);
    final Node bcNode = parentNode.getNode(nodeName);
    if (contentLoaded) {
      bcNode.setProperty(PROPERTY_SECURITY_LOADED, contentLoaded);
      bcNode.setProperty(PROPERTY_SECURITY_LOADED_AT, Calendar.getInstance());
      bcNode.setProperty(PROPERTY_SECURITY_LOADED_BY, this.slingId);
      bcNode.setProperty(PROPERTY_SECURITY_UNLOADED_AT, (String) null);
      bcNode.setProperty(PROPERTY_SECURITY_UNLOADED_BY, (String) null);
      if (createdNodes != null && createdNodes.size() > 0) {
        bcNode.setProperty(PROPERTY_SECURITY_PATHS, createdNodes
            .toArray(new String[createdNodes.size()]));
      }
      session.save();
    }
    LockManager lockManager = session.getWorkspace().getLockManager();
    lockManager.unlock(bcNode.getPath());
  }

  public void contentIsUninstalled(final Session session, final Bundle bundle) {
    final String nodeName = bundle.getSymbolicName();
    try {
      final Node parentNode = (Node) session.getItem(BUNDLE_SECURITY_NODE);
      if (parentNode.hasNode(nodeName)) {
        final Node bcNode = parentNode.getNode(nodeName);
        bcNode.setProperty(PROPERTY_SECURITY_LOADED, false);
        bcNode.setProperty(PROPERTY_SECURITY_UNLOADED_AT, Calendar.getInstance());
        bcNode.setProperty(PROPERTY_SECURITY_UNLOADED_BY, this.slingId);
        session.save();
      }
    } catch (RepositoryException re) {
      LOGGER.error("Unable to update bundle content info.", re);
    }
  }

  /**
   * Fire events, into OSGi, one synchronous one asynchronous.
   *
   * @param operation
   *          the operation being performed.
   * @param session
   *          the session performing operation.
   * @param request
   *          the request that triggered the operation.
   * @param authorizable
   *          the authorizable that is the target of the operation.
   * @param changes
   *          a list of {@link Modification} caused by the operation.
   */
  public void fireEvent(Operation operation, Session session, Authorizable authorizable,
      List<Modification> changes) {
    try {
      eventAdmin.postEvent(AuthorizableEventUtil.newAuthorizableEvent(operation, session.getUserID(), authorizable.getID(), null));
    } catch (Throwable t) {
      LOGGER.warn("Failed to fire event", t);
    }
  }

  public void fireEvent(String path, String acl ) {
    try {
      Dictionary<String, Object> d = new Hashtable<String, Object>();
      d.put("path", path);
      d.put("acl", acl);
      eventAdmin.postEvent(new Event(SecurityLoaderService.class.getName().replace('.','/'),d));
    } catch (Throwable t) {
      LOGGER.warn("Failed to fire event", t);
    }
  }

  /**
   * @param eventAdmin
   *          the new EventAdmin service to bind to this service.
   */
  protected void bindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = eventAdmin;
  }

  /**
   * @param eventAdmin
   *          the EventAdminService to be unbound from this service.
   */
  protected void unbindEventAdmin(EventAdmin eventAdmin) {
    this.eventAdmin = null;
  }

  /**
   * @param string
   * @return
   */
  /**
   * Digest the given password using the configured digest algorithm
   *
   * @param pwd
   *          the value to digest
   * @return the digested value
   * @throws IllegalArgumentException
   */
  public String digestPassword(String pwd) throws IllegalArgumentException {
    try {
      StringBuffer password = new StringBuffer();
      password.append("{").append(passwordDigestAlgoritm).append("}");
      password.append(Text.digest(passwordDigestAlgoritm, pwd.getBytes("UTF-8")));
      return password.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e.toString());
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(e.toString());
    }
  }

  protected void createRepositoryPath(final Session writerSession, final String repositoryPath)
  throws RepositoryException {
      if ( !writerSession.itemExists(repositoryPath) ) {
          Node node = writerSession.getRootNode();
          String path = repositoryPath.substring(1);
          int pos = path.lastIndexOf('/');
          if ( pos != -1 ) {
              final StringTokenizer st = new StringTokenizer(path.substring(0, pos), "/");
              while ( st.hasMoreTokens() ) {
                  final String token = st.nextToken();
                  if ( !node.hasNode(token) ) {
                      node.addNode(token, "sling:Folder");
            writerSession.save();
                  }
                  LOGGER.info("Gettign node "+token+" from "+node);
                  node = node.getNode(token);
              }
              path = path.substring(pos + 1);
          }
          if ( !node.hasNode(path) ) {
              node.addNode(path, "sling:Folder");
        writerSession.save();
          }
      }
  }

}
