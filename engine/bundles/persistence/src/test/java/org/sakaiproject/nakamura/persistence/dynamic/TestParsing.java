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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.sakaiproject.nakamura.persistence.dynamic.xstream.OrmEntity;
import org.sakaiproject.nakamura.persistence.dynamic.xstream.OrmSettings;
import org.sakaiproject.nakamura.persistence.dynamic.xstream.PersistenceSettings;
import org.sakaiproject.nakamura.persistence.dynamic.xstream.PersistenceUnit;
import org.sakaiproject.nakamura.persistence.dynamic.xstream.XStreamWritable;

import java.util.List;
import java.util.Map;

public class TestParsing {

  private void checkNamespaceAppears(XStreamWritable xstreamObject) {
    assertNotNull("Expected xsi ref", xstreamObject.getXsiLocation());
    assertNotNull("Expected schemaLocation", xstreamObject.getSchemaLocation());
    assertNotNull("Expected version", xstreamObject.getVersion());
    assertNotNull("Expected namespace", xstreamObject.getNamespace());
  }

  @Test
  public void testProviderXmlParse() {
    PersistenceSettings settings = PersistenceSettings.parse(this.getClass().getClassLoader()
        .getResourceAsStream("persistence1.xml"));
    List<PersistenceUnit> units = settings.getPersistenceUnits();
    checkNamespaceAppears(settings);
    assertEquals("Expected there to be one unit", 1, units.size());
    PersistenceUnit unit = units.get(0);
    assertEquals("Expected name to be set", "default", unit.getName());
    List<String> persistedClasses = unit.getClasses();
    assertEquals("Expected there to be two classes", 2, persistedClasses.size());
    Map<String, String> properties = unit.getProperties();
    assertEquals("Expected testproperty to be set", "testvalue", properties.get("testproperty"));
  }

  @Test
  public void testOrmXmlParse() {
    OrmSettings settings = OrmSettings.parse(this.getClass().getClassLoader().getResourceAsStream(
        "orm1.xml"));
    checkNamespaceAppears(settings);
    List<OrmEntity> entities = settings.getEntities();
    assertEquals("Expected there to be two entities", 2, entities.size());
    assertEquals("Expected class value to be recorded",
        "org.sakaiproject.nakamura.jpaprovider.model.SystemUser", entities.get(0)
            .getClassName());
  }
}
