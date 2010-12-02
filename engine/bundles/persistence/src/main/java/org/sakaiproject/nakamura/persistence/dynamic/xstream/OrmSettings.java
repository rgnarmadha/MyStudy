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

package org.sakaiproject.nakamura.persistence.dynamic.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@XStreamAlias("entity-mappings")
public class OrmSettings extends XStreamWritable {

  private static final Class<?>[] CLASSES = { OrmSettings.class, OrmEntity.class };

  @XStreamImplicit(itemFieldName = "entity")
  private List<OrmEntity> entities;

  public List<OrmEntity> getEntities() {
    return entities;
  }

  public static Class<?>[] getOrmClasses() {
    return CLASSES.clone();
  }

  public static XStream getXStream() {
    XStream xstream = new XStream();
    xstream.processAnnotations(OrmSettings.getOrmClasses());
    setupNamespaceAliasing(xstream);
    xstream.aliasSystemAttribute("type", "class");
    xstream.registerConverter(new EntityConverter());
    return xstream;
  }

  public static OrmSettings parse(InputStream stream) {
    return (OrmSettings) getXStream().fromXML(stream);
  }

  public void addEntity(OrmEntity entity) {
    if (entities == null) {
      entities = new ArrayList<OrmEntity>();
    }
    entities.add(entity);
  }

}
