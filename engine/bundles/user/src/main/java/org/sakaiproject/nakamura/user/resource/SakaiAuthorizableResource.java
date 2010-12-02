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
package org.sakaiproject.nakamura.user.resource;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResource;

import java.util.Map;

/**
 *
 */
public class SakaiAuthorizableResource extends AuthorizableResource {

  private Authorizable authorizable;

  /**
   * @param authorizable
   * @param resourceResolver
   * @param path
   */
  public SakaiAuthorizableResource(Authorizable authorizable,
      ResourceResolver resourceResolver, String path) {
    super(authorizable, resourceResolver, path);
    this.authorizable = authorizable;
  }
  
  /**
   * {@inheritDoc}
   * @see org.apache.sling.jackrabbit.usermanager.impl.resource.AuthorizableResource#adaptTo(java.lang.Class)
   */
  @SuppressWarnings("unchecked")
  @Override
  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
    if (type == Map.class || type == ValueMap.class) {
      return (AdapterType) new SakaiAuthorizableValueMap(authorizable); // unchecked
                                                                   // cast
    }
    return super.adaptTo(type);
  }

}
