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

package org.sakaiproject.nakamura.jpaprovider.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.io.InputStream;
import java.util.List;

@XStreamAlias("persistence")
public class PersistenceSettings extends XStreamWritable {

  private static final Class<?>[] CLASSES = { PersistenceSettings.class, PersistenceUnit.class,
      Property.class };

  @XStreamImplicit(itemFieldName = "persistence-unit")
  private List<PersistenceUnit> persistenceUnit;

  public List<PersistenceUnit> getPersistenceUnits() {
    return persistenceUnit;
  }

  public static Class<?>[] getPersistenceClasses() {
    return CLASSES.clone();
  }

  public static XStream getXStream() {
    XStream xstream = new XStream();
    xstream.processAnnotations(PersistenceSettings.getPersistenceClasses());
    setupNamespaceAliasing(xstream);
    xstream.useAttributeFor(PersistenceUnit.class, "name");
    xstream.registerConverter(new PropertyConverter());
    return xstream;
  }

  public static PersistenceSettings parse(InputStream stream) {
    return (PersistenceSettings) getXStream().fromXML(stream);
  }

}
