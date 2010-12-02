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

public abstract class XStreamWritable {

  protected String xsiLocation;
  protected String schemaLocation;
  protected String version;
  protected String namespace;

  public String getXsiLocation() {
    return xsiLocation;
  }

  public String getSchemaLocation() {
    return schemaLocation;
  }

  public String getVersion() {
    return version;
  }

  public String getNamespace() {
    return namespace;
  }

  protected static void setupNamespaceAliasing(XStream xstream) {
    xstream.aliasAttribute(XStreamWritable.class, "xsiLocation", "xmlns:xsi");
    xstream.aliasAttribute(XStreamWritable.class, "schemaLocation", "xsi:schemaLocation");
    xstream.aliasAttribute(XStreamWritable.class, "namespace", "xmlns");
    xstream.useAttributeFor(XStreamWritable.class, "version");
  }

}
