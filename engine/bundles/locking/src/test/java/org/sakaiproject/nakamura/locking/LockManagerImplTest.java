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
package org.sakaiproject.nakamura.locking;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.locking.Lock;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;
import org.sakaiproject.nakamura.memory.MapCacheImpl;

/**
 *
 */
public class LockManagerImplTest {

  private Cache<Object> lockCache;
  private Cache<Object> requestCache;
  private CacheManagerService cacheManagerService;
  private LockManagerImpl lockManager;

  /**
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    lockCache = new MapCacheImpl<Object>();
    requestCache = new MapCacheImpl<Object>();

    cacheManagerService = createMock(CacheManagerService.class);
    expect(
        cacheManagerService.getCache("lockmanager.lockmap",
            CacheScope.CLUSTERREPLICATED)).andReturn(lockCache).anyTimes();
    expect(
        cacheManagerService.getCache("lockmanager.requestmap",
            CacheScope.REQUEST)).andReturn(requestCache).anyTimes();

    replay(cacheManagerService);

    lockManager = new LockManagerImpl();
    lockManager.bindCacheManagerService(cacheManagerService);
  }

  /**
   * 
   */
  @After
  public void tearDown() {
    lockManager.unbindCacheManagerService(cacheManagerService);
    verify(cacheManagerService);
  }

  @Test
  public void testGetLock() {
    Lock l = lockManager.getLock("foo");
    assertEquals("foo", l.getLocked());
    assertEquals(true, l.isOwner());
  }

  @Test
  public void testUnlock() {
    LockImpl l = (LockImpl) lockManager.getLock("foo");
    assertEquals(true, l.isLocked());
    lockManager.unlock(l);
    assertEquals(false, l.isLocked());
  }

  @Test
  public void testClearLocks() {
    LockImpl l = (LockImpl) lockManager.getLock("foo");
    assertEquals(true, l.isLocked());
    lockManager.clearLocks();
    Lock lock = lockManager.getLock("foo", false);
    assertEquals(null, lock);
  }

}
