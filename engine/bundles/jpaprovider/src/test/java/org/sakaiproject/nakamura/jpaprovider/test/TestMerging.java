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

package org.sakaiproject.nakamura.jpaprovider.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.sakaiproject.nakamura.jpaprovider.AmalgamatingClassloader;
import org.sakaiproject.nakamura.jpaprovider.PersistenceBundleMonitor;
import org.sakaiproject.nakamura.jpaprovider.xstream.OrmEntity;
import org.sakaiproject.nakamura.jpaprovider.xstream.OrmSettings;
import org.sakaiproject.nakamura.jpaprovider.xstream.PersistenceSettings;
import org.sakaiproject.nakamura.jpaprovider.xstream.PersistenceUnit;
import org.sakaiproject.nakamura.jpaprovider.xstream.Property;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestMerging {

  private Bundle createDummyBundle(String puName, String persistenceXml, String ormXml)
      throws IOException {
    Map<String, String> dummyFiles1 = new HashMap<String, String>();
    dummyFiles1.put(AmalgamatingClassloader.PERSISTENCE_XML, persistenceXml);
    dummyFiles1.put(AmalgamatingClassloader.ORM_XML, ormXml);
    return new DummyJpaBundle(puName, dummyFiles1, Bundle.STARTING);
  }

  @Test
  public void testMergePersistences() throws Exception {
    PersistenceBundleMonitor monitor = new PersistenceBundleMonitor();
    monitor.start(new DummyBundleContext(new DummyBundle()));
    Bundle fakeBundle = createDummyBundle("default", "persistence1.xml", "orm1.xml");
    monitor.bundleChanged(new BundleEvent(BundleEvent.STARTING, fakeBundle));
    fakeBundle = createDummyBundle("default", "persistence2.xml", "orm2.xml");
    monitor.bundleChanged(new BundleEvent(BundleEvent.STARTING, fakeBundle));
    ClassLoader amalgamatingClassloader = PersistenceBundleMonitor.getAmalgamatedClassloader();
    PersistenceSettings settings = PersistenceSettings.parse(amalgamatingClassloader
        .getResourceAsStream("META-INF/persistence.xml"));
    assertEquals("Expected one persistence unit", 1, settings.getPersistenceUnits().size());
    PersistenceUnit unit = settings.getPersistenceUnits().get(0);
    Map<String, String> props = unit.getProperties();
    assertEquals("Expected testproperty to be set", "testvalue", props.get("testproperty"));
    assertEquals("Expected testproperty2 to be set", "testvalue2", props.get("testproperty2"));
    List<String> classes = unit.getClasses();
    assertNotNull("Expected classes not to be null", classes);
    assertEquals("Expected 4 classes", 5, classes.size());
    OrmSettings ormSettings = OrmSettings.parse(amalgamatingClassloader
        .getResourceAsStream("META-INF/orm.xml"));
    List<OrmEntity> entities = ormSettings.getEntities();
    assertEquals("Expected to find 4 ORM entities", 5, entities.size());
  }

  @Test
  public void testMergeIntoNullProperties() {
    PersistenceUnit pu = new PersistenceUnit();
    List<Property> props = new ArrayList<Property>();
    Property prop = new Property();
    props.add(prop);
    pu.addProperties(props);
    assertSame("Expected property to have been added", prop, pu.getPropertiesList().get(0));
  }

  public void testMergeNullProperties() {
    PersistenceUnit pu = new PersistenceUnit();
    pu.addProperties(null);
  }
}
