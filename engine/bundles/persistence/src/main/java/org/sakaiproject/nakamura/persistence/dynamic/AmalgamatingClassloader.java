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

import com.thoughtworks.xstream.XStream;

import org.sakaiproject.nakamura.persistence.dynamic.xstream.OrmEntity;
import org.sakaiproject.nakamura.persistence.dynamic.xstream.OrmSettings;
import org.sakaiproject.nakamura.persistence.dynamic.xstream.PersistenceSettings;
import org.sakaiproject.nakamura.persistence.dynamic.xstream.PersistenceUnit;
import org.sakaiproject.nakamura.persistence.dynamic.xstream.XStreamWritable;
import org.sakaiproject.nakamura.util.UrlEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class AmalgamatingClassloader extends ClassLoader {

  private static final Logger LOG = LoggerFactory.getLogger(AmalgamatingClassloader.class);

  public static final String NAKAMURA_PERSISTENCE_XML = "META-INF/nakamura-persistence.xml";
  public static final String PERSISTENCE_XML = "META-INF/persistence.xml";
  public static final String NAKAMURA_ORM_XML = "META-INF/nakamura-orm.xml";
  public static final String ORM_XML = "META-INF/orm.xml";

  private Map<String, PersistenceUnit> settingsMap = new HashMap<String, PersistenceUnit>();
  private PersistenceSettings basePersistenceSettings;
  private OrmSettings baseOrmSettings;
  private URL persistenceXMLurl;
  private URL ormXMLurl;

  private PersistenceBundleMonitor persistenceBundleMonitor;

  public AmalgamatingClassloader(ClassLoader classLoader, PersistenceBundleMonitor persistenceBundleMonitor) {
    super(classLoader);
    this.persistenceBundleMonitor = persistenceBundleMonitor;
    basePersistenceSettings = PersistenceSettings.parse(classLoader
        .getResourceAsStream(NAKAMURA_PERSISTENCE_XML));
    for (PersistenceUnit unit : basePersistenceSettings.getPersistenceUnits()) {
      settingsMap.put(unit.getName(), unit);
    }
    baseOrmSettings = OrmSettings.parse(classLoader.getResourceAsStream(NAKAMURA_ORM_XML));
  }

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    try
    {
      return super.loadClass(name);
    }
    catch (ClassNotFoundException cnfe)
    {
      BundleGatheringResourceFinder finder = persistenceBundleMonitor.getBundleResourceFinder();
      Class<?> result = finder.loadClass(name);
      if (result != null) {
        return result;
      }
      throw cnfe;
    }
  }

  public void importPersistenceXml(URL persistence) throws IOException {
    PersistenceSettings newSettings = PersistenceSettings.parse(persistence.openStream());
    for (PersistenceUnit unit : newSettings.getPersistenceUnits()) {
      PersistenceUnit existingUnit = settingsMap.get(unit.getName());
      if (existingUnit == null) {
        settingsMap.put(unit.getName(), unit);
      } else {
        existingUnit.addClasses(unit.getClasses());
        existingUnit.addProperties(unit.getPropertiesList());
      }
    }
  }

  public void importOrmXml(URL orm) throws IOException {
    OrmSettings settings = OrmSettings.parse(orm.openStream());
    for (OrmEntity entity : settings.getEntities()) {
      baseOrmSettings.addEntity(entity);
    }
  }

  private URL getResourceWithOverride(String name) throws IOException {
    if (PERSISTENCE_XML.equals(name)) {
      if (persistenceXMLurl == null) {
        persistenceXMLurl = constructUrl(PersistenceSettings.getXStream(), basePersistenceSettings,
            PERSISTENCE_XML);
      }
      return persistenceXMLurl;
    } else if (ORM_XML.equals(name)) {
      if (ormXMLurl == null) {
        ormXMLurl = constructUrl(OrmSettings.getXStream(), baseOrmSettings, ORM_XML);
      }
      return ormXMLurl;
    }
    return null;
  }

  @Override
  public URL getResource(String name) {
    try {
      return getResourceWithOverride(name);
    } catch (IOException ioe) {
      LOG.warn("Failed to get resource with override cause {} ",ioe.getMessage());
      return super.getResource(name);
    }
  }

  public Enumeration<URL> getResources(final String name) throws IOException {
    URL overriddenURL = getResourceWithOverride(name);
    if (overriddenURL != null) {
      return new UrlEnumeration(overriddenURL);
    } else {
      return super.getResources(name);
    }
  }

  /**
   * Constructs a temporary file that merges together the requested filename as
   * it is found in different artifacts (jars). The URL to the merged file is
   * returned.
   * 
   * @param filename
   *          The file to look for in the classloader.
   * @return The merged result of the found filenames.
   * @throws IOException
   */
  private URL constructUrl(XStream xstream, XStreamWritable writable, String filename)
      throws IOException {
    LOG.debug(filename + " " + writable);

    // The base directory must be empty since JPA will scan it searching for
    // classes.
    
    File file = new File(System.getProperty("java.io.tmpdir") + "/sakai." 
        + System.getProperty("user.name") + "/"
        + System.currentTimeMillis() + "/" + filename);
    if (file.getParentFile().mkdirs()) {
      LOG.debug("Created " + file);
    }

    FileOutputStream out = new FileOutputStream(file);
    try {
      xstream.toXML(writable, out);
    } finally {
      out.close();
    }
    URL url = null;
    try {
      url = file.toURI().toURL();
    } catch (MalformedURLException e) {
      LOG.error("cannot convert file to URL " + e.toString());
    }
    LOG.debug("URL: " + url);
    return url;
  }

}
