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
package org.sakaiproject.nakamura.userinitializer;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Component to make selected pre-defined Jackrabbit Authorizable objects into
 * Sakai 3 User or Group entities by setting required properties and applying
 * required post-processors.
 */
@Component(metatype = true, immediate = true)
public class UserInitializerService {
  private static final Logger LOGGER = LoggerFactory.getLogger(UserInitializerService.class);

  @Reference
  protected AuthorizablePostProcessService authorizablePostProcessService;
  @Reference
  protected SlingRepository repository;

  private DefaultAuthorizablesLoader defaultAuthorizablesLoader = new DefaultAuthorizablesLoader();

  //----------- OSGi integration ----------------------------

  @Activate
  protected void activate(ComponentContext componentContext) {
    LOGGER.debug("activate called");
    defaultAuthorizablesLoader.initDefaultUsers(authorizablePostProcessService,
        componentContext.getBundleContext().getBundle(), repository);
  }
}
