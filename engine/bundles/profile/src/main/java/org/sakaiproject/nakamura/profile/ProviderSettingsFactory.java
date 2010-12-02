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
package org.sakaiproject.nakamura.profile;

import org.sakaiproject.nakamura.api.profile.ProviderSettings;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 */
public class ProviderSettingsFactory {

  protected static final String PROVIDER_SETTINGS = "/var/profile/providersByName";

  /**
   * {@inheritDoc}
   * 
   * @throws RepositoryException
   * @throws
   * 
   * @see org.sakaiproject.nakamura.api.profile.ProviderSettingsFactory#newProviderSettings(java.lang.String,
   *      javax.jcr.Node)
   */
  public ProviderSettings newProviderSettings(String path, Node node)
      throws RepositoryException {
    if (node.hasProperty("sakai:source")) {
      if ("external".equals(node.getProperty("sakai:source").getString())) {
        System.err.println("Is external " + node.getProperty("sakai:source").getString());
        
        Session session = node.getSession();
        String providerSettings = appendPath(PROVIDER_SETTINGS, path);
        System.err.println("Locating "+providerSettings);
        if (session.nodeExists(providerSettings)) {
          return new ProviderSettingsImpl(node, session.getNode(providerSettings));
        } else {
          System.err.println("No Settings "+providerSettings);   
        }
      }
    }
    return null;
  }

  /**
   * @param string
   * @param name
   * @return
   */
  private String appendPath(String path, String name) {
    System.err.println("Appending ["+path+"]["+name);
    if (path.endsWith("/")) {
      return path + name;
    }
    if (name.startsWith("/")) {
      return path + name;
    }
    return path + "/" + name;
  }

}
