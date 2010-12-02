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

import org.sakaiproject.nakamura.jpaprovider.AmalgamatingClassloader;
import org.sakaiproject.nakamura.jpaprovider.PersistenceBundleMonitor;
import org.sakaiproject.nakamura.jpaprovider.xstream.PersistenceSettings;

import java.io.IOException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

public class DummyJpaBundle extends DummyBundle {

  private String puName;
  private Map<String, String> files;
  private int state;

  public DummyJpaBundle(String puName, Map<String, String> files, int state) throws IOException {
    this.state = state;
    this.files = files;
    PersistenceSettings settings = PersistenceSettings.parse(getClass().getClassLoader()
        .getResourceAsStream(files.get(AmalgamatingClassloader.PERSISTENCE_XML)));
    this.puName = settings.getPersistenceUnits().get(0).getName();
  }

  @SuppressWarnings("rawtypes")
  public Dictionary getHeaders() {
    Hashtable<String, String> result = new Hashtable<String, String>();
    result.put(PersistenceBundleMonitor.SAKAI_JPA_PERSISTENCE_UNITS_BUNDLE_HEADER, puName);
    return result;
  }

  @SuppressWarnings("rawtypes")
  public Dictionary getHeaders(String arg0) {
    return getHeaders();
  }

  public URL getResource(String resourceName) {
    if (files.containsKey(resourceName)) {
      return getClass().getClassLoader().getResource(files.get(resourceName));
    }
    return null;
  }

  public int getState() {
    return state;
  }

}
