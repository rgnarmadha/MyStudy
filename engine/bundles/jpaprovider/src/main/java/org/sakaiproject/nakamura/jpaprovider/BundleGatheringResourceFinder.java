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

package org.sakaiproject.nakamura.jpaprovider;

import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class BundleGatheringResourceFinder {

  private static final Logger LOG = LoggerFactory.getLogger(BundleGatheringResourceFinder.class);
  private Collection<Bundle> bundles;

  public BundleGatheringResourceFinder(Collection<Bundle> bundles) {
    this.bundles = bundles;
  }

  public List<URL> getResources(String string) {
    List<URL> result = new LinkedList<URL>();
    for (Bundle bundle : bundles) {
      URL resource = bundle.getResource(string);
      if (resource != null) {
        result.add(resource);
      }
    }
    return result;
  }

  public Class<?> loadClass(String name) {
    for (Bundle bundle : bundles) {
      try {
        return bundle.loadClass(name);
      } catch (Exception e) {
        /* Try another bundle */
      }
    }
    LOG.warn("Unable to find class '" + name + "' in bundles");
    return null;
  }

}
