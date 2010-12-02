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
package org.sakaiproject.nakamura.chat;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.chat.ChatManagerService;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;

/**
 * The <code>ChatManagerServiceImpl</code>
 */
@Component(immediate = true, label = "ChatManagerServiceImpl", description = "Implementation of the Chat Manager Service")
@Service(value = ChatManagerService.class)
@Properties(value = {
    @Property(name = "service.vendor", value = "The Sakai Foundation"),
    @Property(name = "service.description", value = "Chat Manager Implementation") })
@Reference(name = "CacheManagerService", referenceInterface = CacheManagerService.class)
public class ChatManagerServiceImpl implements ChatManagerService {

  private static final String CHAT_CACHE = "chat";

  private CacheManagerService cacheManagerService;

  protected void bindCacheManagerService(CacheManagerService cacheManagerService) {
    this.cacheManagerService = cacheManagerService;
  }

  protected void unbindCacheManagerService(
      CacheManagerService cacheManagerService) {
    this.cacheManagerService = cacheManagerService;
  }

  /**
   * Gets the cache.
   * 
   * @return
   */
  private Cache<Long> getCachedMap() {
    return cacheManagerService.getCache(CHAT_CACHE,
        CacheScope.CLUSTERREPLICATED);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.chat.ChatManagerService#clear()
   */
  public void clear() {
    getCachedMap().clear();
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.chat.ChatManagerService#get(java.lang.String)
   */
  public Long get(String userID) {
    return (Long) getCachedMap().get(userID);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.chat.ChatManagerService#put(java.lang.String, long)
   */
  public void put(String userID, long time) {
    getCachedMap().put(userID, time);
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.sakaiproject.nakamura.api.chat.ChatManagerService#remove(java.lang.String)
   */
  public void remove(String userID) {
    getCachedMap().remove(userID);
  }

}
