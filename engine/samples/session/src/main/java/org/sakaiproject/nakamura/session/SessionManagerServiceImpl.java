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
package org.sakaiproject.nakamura.session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;
import org.sakaiproject.nakamura.api.session.SessionManagerService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * The <code>SessionManagerServiceImpl</code>
 */
@Component(immediate = true, label = "SessionManagerServiceImpl", description = "Implementation of the Session Manager Service", name = "org.sakaiproject.nakamura.api.session.SessionManagerService")
@Service(value = SessionManagerService.class)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.descriptionr", value = "Session Manager Service Implementation") })
public class SessionManagerServiceImpl implements SessionManagerService {

  private static final String REQUEST_CACHE = "request";
  private static final String CURRENT_REQUEST = "_r";

  @Reference(bind = "bindCacheManagerService", name = "cacheManagerService", unbind = "unbindCacheManagerService")
  private CacheManagerService cacheManagerService;

  protected Cache<HttpServletRequest> getRequestScope() {
    return cacheManagerService.getCache(REQUEST_CACHE, CacheScope.REQUEST);
  }

  public void bindRequest(HttpServletRequest request) {
    getRequestScope().put(CURRENT_REQUEST, request);
  }

  public HttpServletRequest getCurrentRequest() {
    return getRequestScope().get(CURRENT_REQUEST);
  }
  
  public void unbindRequest(HttpServletRequest request) {
    cacheManagerService.unbind(CacheScope.REQUEST);
  }

  public HttpSession getCurrentSession() {
    try {
      return getCurrentRequest().getSession();
    } catch (NullPointerException npe) {
      return null;
    }
  }

  public String getCurrentUserId() {
    try {
      return getCurrentRequest().getRemoteUser();
    } catch (NullPointerException npe) {
      return null;
    }
  }

  /**
   * @param cacheManagerService
   */
  protected void bindCacheManagerService(CacheManagerService cacheManagerService) {
    this.cacheManagerService = cacheManagerService;
  }

  /**
   * @param cacheManagerService
   */
  protected void unbindCacheManagerService(CacheManagerService cacheManagerService) {
    this.cacheManagerService = null;
  }

}
