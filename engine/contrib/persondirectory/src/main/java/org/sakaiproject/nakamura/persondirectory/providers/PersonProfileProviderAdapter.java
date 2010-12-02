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
package org.sakaiproject.nakamura.persondirectory.providers;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.persondirectory.PersonProvider;
import org.sakaiproject.nakamura.api.persondirectory.PersonProviderException;
import org.sakaiproject.nakamura.api.profile.ProfileProvider;
import org.sakaiproject.nakamura.api.profile.ProviderSettings;
import org.sakaiproject.nakamura.util.ImmediateFuture;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.jcr.Node;

/**
 *
 */
@Component(immediate = true, description = "A Service Implementation of the Profile Provider that connects to a Person Provider", name = "Profile Provider Adapter")
@Service()
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "A ProfileProvider that connects to a Person Provider"),
    @Property(name = ProfileProvider.PROVIDER_NAME, value = "person") })
public class PersonProfileProviderAdapter implements ProfileProvider {

  @Reference
  protected PersonProvider personProvider;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.profile.ProfileProvider#getProvidedMap(java.util.List)
   */
  public Map<? extends Node, ? extends Future<Map<String, Object>>> getProvidedMap(
      List<ProviderSettings> list) {

    Map<Node, Future<Map<String, Object>>> resultMap = new HashMap<Node, Future<Map<String, Object>>>();

    for (ProviderSettings s : list) {
      Node n = s.getNode();
      try {
        Map<String, Object> profile = personProvider.getProfileSection(n);
        resultMap.put(n, new ImmediateFuture<Map<String, Object>>(profile));
      } catch (PersonProviderException e) {
        Map<String, Object> profileError = new HashMap<String, Object>();
        profileError.put("error", e.getMessage());
        resultMap.put(n, new ImmediateFuture<Map<String, Object>>(profileError));
      }
    }

    return resultMap;
  }

}
