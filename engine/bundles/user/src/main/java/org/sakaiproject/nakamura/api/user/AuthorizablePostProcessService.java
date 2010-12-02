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
package org.sakaiproject.nakamura.api.user;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.util.osgi.BoundService;

import java.util.Map;

import javax.jcr.Session;

/**
 * The AuthorizablePostProcessService lets Jackrabbit Authorizable objects be used
 * as richer Sakai User and Group entities. Most of the additional functionality is
 * contributed by AuthorizablePostProcessor services.
 */
public interface AuthorizablePostProcessService extends BoundService {

  /**
   * This method is a shortcut to process a Jacrkabbit Authorizable without passing
   * any extra parameters.
   *
   * @param authorizable
   * @param session
   * @param change
   * @throws Exception
   */
  void process(Authorizable authorizable, Session session, ModificationType change)
    throws Exception;

  /**
   * Do whatever's needed to make the given change to a Jackrabbit Authorizable
   * be reflected in Sakai User and Group entities. The method should be called
   * after creation or modification of the Authorizable but before deletion.
   *
   * @param authorizable
   * @param session
   * @param change
   * @param parameters a map of non-persisted parameters to be passed on for
   * possible use by processing services
   * @throws Exception
   */
  void process(Authorizable authorizable, Session session, ModificationType change, Map<String, Object[]> parameters)
    throws Exception;

  /**
   * Use Sling request parameters as a basis for processing.
   *
   * @param authorizable
   * @param session
   * @param change
   * @param request
   * @throws Exception
   */
  void process(Authorizable authorizable, Session session, ModificationType change, SlingHttpServletRequest request)
    throws Exception;
}
