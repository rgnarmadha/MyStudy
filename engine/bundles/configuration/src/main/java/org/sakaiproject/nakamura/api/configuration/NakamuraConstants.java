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

package org.sakaiproject.nakamura.api.configuration;

import com.google.common.base.ReferenceType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.ReferenceMap;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/* XXX PROBLEM. I've renamed this the StubConfigurationServiceImpl from NakamuraConstants. It does contain
 * some nakamura constants, but also is a stub configuration service. Really, the nakamura constants should
 * be moved into a separate file, for things like annotations, etc, to use. However, I'm not sure I
 * understand what the scr plugin is doing here enough, yet, to do that refactor. This is something which
 * is high on my todo-list. -- dan
 * 
 * ieb: Please leave as NakamuraConstants. OSGi forces the constant declaration and the ManagedService
 * to be in the same class *if* you want the standard OSGi configuration tools to work. We are therefore
 * forced to bind the API to the impl (yuck!) to avoid writing the constants names in multiple places.
 * We *could* unbind but then we would have to reference then API twice and its possible that this would 
 * break the scr plugin. I have tried to make this cleaner, but OSGi will not let us. 
 * 
 */

/**
 * Holds the configuration properties that are used in nakamura. This is an OSGi Managed
 * service that collects together all the configuration properties.
 */
@Component(immediate = true, metatype = true, label = "Sakai Configuration Service", description = "Provides Configuration for nakamura components")
@Service(value = ConfigurationService.class)
public class NakamuraConstants implements ConfigurationService, ManagedService {

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "Implementation of the Configuration Service that provides setup properties for all nakamura bundles.")
  static final String SERVICE_DESCRIPTION = "service.description";

  @Property(value = "/userenv", name = "JCR User Environment Base Path", description = "The location of system private data in the repo, read only, one time configuration per repository.")
  public static final String JCR_USERENV_BASE = "jcruserenv.base";
  /**
   * list of templates in the form type=path;type=path;
   */
  @Property(value = "jcruserenv.templates=student=/configuration/defaults/usertypes/userenv-student.json;researcher=/configuration/defaults/usertypes/userenv-researcher.json;")
  public static final String JCR_USERENV_TEMPLATES = "jcruserenv.templates";
  /**
   * The default template for a user
   */
  @Property(value = "/configuration/defaults/usertypes/userenv-default.json")
  public static final String JCR_DEFAULT_TEMPLATE = "jcruserenv.templates.default";
  /**
   * list of profile templates in the form type=path;type=path;
   */
  @Property(value = "/configuration/defaults/usertypes/profile-student.json;researcher=/configuration/defaults/usertypes/profile-researcher.json;")
  public static final String JCR_PROFILE_TEMPLATES = "jcrprofile.templates";
  /**
   * The default template for a user
   */
  @Property(value = "/configuration/defaults/usertypes/profile-default.json")
  public static final String JCR_PROFILE_DEFAUT_TEMPLATES = "jcrprofile.templates.default";
  /**
   * The template locations for site creation.
   */
  @Property(value = "project=/configuration/defaults/sitetypes/project-site.json;course=/configuration/defaults/sitetypes/course-site.json;")
  public static final String JCR_SITE_TEMPLATES = "jcrsite.templates";
  /**
   * Default site template.
   */
  @Property(value = "/configuration/defaults/sitetypes/default-site.json")
  public static final String JCR_SITE_DEFAULT_TEMPLATE = "jcrsite.templates.default";

  /**
   * The property name defining the users public data
   */
  @Property(value = "/_user/public")
  public static final String PRIVATE_SHARED_PATH_BASE = "jcrprivateshared.base";
  /**
   * The property name defining the data that is completely private to the user.
   */
  @Property(value = "/_user/private")
  public static final String PRIVATE_PATH_BASE = "jcrprivate.base";
  /**
   * Setting: The time to live of User Env objects the local cache, this should be set in
   * the nakamura properties file.
   */
  @Property(value = "600000")
  public static final String TTL = "userenvironment.ttl";

  /**
   * This controls whether anonymous account creation is enabled. It also enables the
   * ability to to check for the existence of eids in the system anonymously....without
   * being superuser.
   */
  @Property(value = "true")
  public static final String PROP_ANON_ACCOUNTING = "rest.user.anonymous.account.creation";
  /**
   * Control over the JPA Entity Manager scope, can be THREAD if its really stable
   * although this means the filter must commit, or REQUEST, then the standard filter
   * manages commits
   */
  @Property(value = "REQUEST")
  public static final String ENTITY_MANAGER_SCOPE = "jpa.entitymanager.scope";

  @Property(value = "org.apache.derby.jdbc.EmbeddedDriver")
  public static final String JDBC_DRIVER_NAME = "jdbc.driver";
  @Property(value = "jdbc:derby:target/testdb;create=true")
  public static final String JDBC_URL = "jdbc.url";
  @Property(value = "sa")
  public static final String JDBC_USERNAME = "jdbc.username";
  @Property
  public static final String JDBC_PASSWORD = "jdbc.password";
  @Property(value = "values(1)")
  public static final String JDBC_VALIDATION_QUERY = "jdbc.validation";
  @Property(value = "false")
  public static final String JDBC_DEFAULT_READ_ONLY = "jdbc.defaultReadOnly";
  @Property(value = "true")
  public static final String JDBC_DEFAULT_AUTO_COMMIT = "jdbc.defaultAutoCommit";
  @Property(value = "false")
  public static final String JDBC_DEFAULT_PREPARED_STATEMENTS = "jdbc.defaultPreparedStatement";
  @Property(value = "600")
  public static final String TRANSACTION_TIMEOUT_SECONDS = "transaction.timeoutSeconds";
  @Property(value = "1")
  public static final String DB_MIN_WRITE = "eclipselink.write.min";
  @Property(value = "1")
  public static final String DB_MIN_NUM_READ = "eclipselink.read.min";
  @Property(value = "default")
  public static final String DB_UNITNAME = "jpa.unitname";
  @Property(value = "SAKAIID")
  public static final String SESSION_COOKIE = "http.global.cookiename";

  @Property
  public static final String SUBJECT_PROVIDER_REGISTRY = "subjectstatement.provider";
  /**
   * The name of the registry used for this type of service.
   */
  @Property
  public static final String AUTHENTICATION_PROVIDER_REGISTRY = "authentication.provider.registry";
  @Property
  public static final String MANAGER_PROVIDER_REGISTRY = "authentication.manager.provider.registry";
  /**
   * The name of the registry used for this type of service.
   */
  @Property
  public static final String USER_PROVIDER_REGISTRY = "user.provider.registry";

  // constant properties
  /**
   * Name of the groupdef file.
   */
  public static final String GROUP_FILE_NAME = "groupdef.json";
  /**
   * The Name of the userenv file in the system.
   */
  public static final String USERENV = "userenv";

  /**
   * The name of the profile file.
   */
  public static final String PROFILE = "profile";

  /**
   * The name of the friends file
   */
  public static final String FRIENDS_FILE = "friends.json";

  /**
   * Attribute used in the session to store a list of group memberships.
   */
  public static final String GROUPMEMBERSHIP = "userenv.grouplist";
  /**
   * A null user environment file.
   */
  public static final String NULLUSERENV = "userenv.null";
  /**
   */
  public static final String JSON_CLASSMAP = "jsonconverter.classmap";
  /**
   */
  public static final String OUTBOX = "outbox";
  /**
   */
  public static final String MESSAGES = "messages";
  private static final String OVERRIDE_SETTINGS = "override.properties";
  private static final String DEFAULT_SETTINGS = "default.properties";

  private static final Logger logger = LoggerFactory.getLogger(Object.class);

  /**
   * The configuration map
   */
  private ImmutableMap<String, String> configMap;
  private Map<String, ConfigurationListener> listeners = new ReferenceMap<String, ConfigurationListener>(
      ReferenceType.STRONG, ReferenceType.WEAK);

  /**
   * @throws IOException
   * @throws IOException
   * 
   */

  private void loadPropertiesFromStream(Map<String, String> map, InputStream in)
      throws IOException {
    if (in == null)
      return;
    Properties p = new Properties();
    try {
      p.load(in);
    } finally {
      in.close();
    }
    for (Enumeration<?> e = p.keys(); e.hasMoreElements();) {
      String k = (String) e.nextElement();
      if (!map.containsKey(k))
        map.put(k, (String) p.get(k));
    }
  }

  private InputStream findInputStreamInPathToRoot(File dir, String filename)
      throws IOException {
    while (dir != null) {
      File file = new File(dir, filename);
      logger.info("Trying " + file.getCanonicalPath());
      if (file.exists() && file.isFile()) {
        logger.info("Found " + filename + " at " + file.getCanonicalPath());
        return new FileInputStream(file);
      }
      dir = dir.getParentFile();
    }
    return null;
  }

  public NakamuraConstants() throws IOException {
    Map<String, String> constants = new HashMap<String, String>();
    File cwd = new File(new File(".").getCanonicalPath()); // Need to use full path to cwd
                                                           // for getParentFile to work.
    InputStream file = findInputStreamInPathToRoot(cwd, OVERRIDE_SETTINGS);
    if (file != null) {
      loadPropertiesFromStream(constants, file);
    }
    loadPropertiesFromStream(constants, getClass().getResourceAsStream(OVERRIDE_SETTINGS));
    loadPropertiesFromStream(constants, getClass().getResourceAsStream(DEFAULT_SETTINGS));
    Builder<String, String> builder = ImmutableMap.builder();
    for (Map.Entry<String, String> e : constants.entrySet())
      builder.put(e.getKey(), e.getValue());
    configMap = builder.build();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("rawtypes")
  public void updated(Dictionary config) throws ConfigurationException {
    Builder<String, String> builder = ImmutableMap.builder();
    for (Enumeration<?> e = config.keys(); e.hasMoreElements();) {
      String k = (String) e.nextElement();
      builder.put(k, (String) config.get(k));
    }
    configMap = builder.build();
    notifyUpdate(configMap);
  }

  /**
   * @param configMap2
   */
  private void notifyUpdate(Map<String, String> configMapLocal) {
    List<ConfigurationListener> savedListners = Lists.immutableList(listeners.values());
    for (ConfigurationListener listener : savedListners) {
      listener.update(configMapLocal);
    }
  }

  public Map<String, String> getProperties() {
    return configMap;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.configuration.ConfigurationService#getProperty()
   */
  public String getProperty(String key) {
    return configMap.get(key);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.configuration.ConfigurationService#addListener(org.sakaiproject.nakamura.api.configuration.ConfigutationListener)
   */
  public void addListener(ConfigurationListener listener) {
    listeners.put(String.valueOf(listener), listener);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.configuration.ConfigurationService#removeListener(org.sakaiproject.nakamura.api.configuration.ConfigutationListener)
   */
  public void removeListener(ConfigurationListener listener) {
    listeners.remove(String.valueOf(listener));
  }

}
