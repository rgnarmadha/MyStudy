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


import edu.umd.cs.findbugs.annotations.SuppressWarnings;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.sakaiproject.nakamura.api.locking.Lock;
import org.sakaiproject.nakamura.api.locking.LockManager;
import org.sakaiproject.nakamura.api.locking.LockTimeoutException;
import org.sakaiproject.nakamura.api.memory.Cache;
import org.sakaiproject.nakamura.api.memory.CacheManagerService;
import org.sakaiproject.nakamura.api.memory.CacheScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * A lock manager that uses a cluster replicated cache to manage the locks
 */
@Component(immediate = true)
@Service
@SuppressWarnings(justification="Circular dependency noted ", value={"CD_CIRCULAR_DEPENDENCY"})
public class LockManagerImpl implements LockManager {

  /**
   * The name of the cluster replicated cache. This cache must be configured with a
   * suitable expiry to allow removal o stale locks and configured with a cluster wide
   * replication.
   */
  private static final String LOCKMAP = "lockmanager.lockmap";
  /**
   *
   */
  private static final String REQUEST_LOCKS = "lockmanager.requestmap";
  /**
   * The Logger
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LockManagerImpl.class);
  /**
   * debug flag set at service creation.
   */
  private static final boolean debug = LOGGER.isDebugEnabled();

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  @Property(value = "In JVM Lock Manager")
  static final String SERVICE_DESCRIPTION = "service.description";

  /**
   * Service dependency, the Cache Manager
   */
  @Reference
  private CacheManagerService cacheManagerService;
  /**
   * container for Locks.
   */
  private Cache<LockImpl> lockMap;
  /**
   * The id of this instance of this class.
   */
  private long instanceId;
  /**
   *
   */
  private SecureRandom random;
  /**
   *
   */
  private ThreadLocal<Long> threadId = new ThreadLocal<Long>() {
    /**
     * {@inheritDoc}
     *
     * @see java.lang.ThreadLocal#initialValue()
     */
    @Override
    protected Long initialValue() {
      return random.nextLong();
    }
  };
  private Object monitor = new Object();

  /**
   * @throws NoSuchAlgorithmException
   *
   */
  public LockManagerImpl()
      throws NoSuchAlgorithmException {
    random = SecureRandom.getInstance("SHA1PRNG");
    instanceId = random.nextLong();
  }

  public Lock getLock(String id) {
    return getLock(id, true);
  }

  /**
   * @param id
   * @return
   */
  public Lock getLock(String id, boolean create) {
    LockImpl lock = lockMap.get(id);
    if (create) {
      if (lock == null || !lock.isLocked()) {
        synchronized (monitor) {
          lock = lockMap.get(id);
          if (lock == null || !lock.isLocked()) {
            Cache<LockImpl> requestLocks = getRequestLocks();
            lock = new LockImpl(id, random.nextLong(), threadId.get(), instanceId);
            lockMap.put(id, lock);
            requestLocks.put(id, lock);
          }
        }
      }
    }
    if (lock != null) {
      lock.bind(this);
    }
    return lock;
  }

  /**
   * @return
   */
  private Cache<LockImpl> getRequestLocks() {
    return cacheManagerService.getCache(REQUEST_LOCKS, CacheScope.REQUEST);
  }

  /**
   * Unlock only if the current thread is the owner.
   *
   * @param lock
   */
  protected void unlock(LockImpl lock) {
    if (lock.isOwner() && lock.isLocked()) {
      if (debug) {
        LOGGER.debug(Thread.currentThread() + " unlocked " + lock.getLocked());
      }
      lock.setLocked(false);
      synchronized (monitor) {
        lockMap.remove(lock.getLocked());
      }
    }
  }

  /**
   * @return
   */
  protected long getInstanceId() {
    return instanceId;
  }

  /**
   * @return
   */
  public long getThreadId() {
    return threadId.get();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.locking.LockManager#lock(java.lang.String)
   */
  public Lock waitForLock(String id) throws LockTimeoutException {
    long sleepTime = 100;
    int tries = 0;
    if (debug) {
      LOGGER.debug(Thread.currentThread() + " locking " + id);
    }
    while (tries++ < 300) {
      Lock lock = getLock(id);
      if (lock != null && lock.isOwner()) {
        if (debug) {
          LOGGER.debug(Thread.currentThread() + " lock Granted " + lock.getLocked());
        }
        return lock;
      }
      if (sleepTime < 500) {
        sleepTime = sleepTime + 10;
      }
      try {
       
        if (debug && tries % 5 == 0) {
          LOGGER.debug(Thread.currentThread() + " locking " + id);
        }
        if (tries % 100 == 0) {
          LOGGER.warn(Thread.currentThread() + " Waiting for " + sleepTime
              + " ms " + tries);
        }
        Thread.sleep(sleepTime);
      } catch (InterruptedException e) {
      }
    }
    throw new LockTimeoutException("Failed to lock node " + id);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.nakamura.api.locking.LockManager#clearLocks()
   */
  public void clearLocks() {
    Cache<LockImpl> requestLocks = getRequestLocks();
    // clearing the requestLocks will invoke unbind which will unlock.
    requestLocks.clear();
  }

 
  /**
   * @param cacheManagerService
   */
  protected void bindCacheManagerService(CacheManagerService cacheManagerService) {
    this.cacheManagerService = cacheManagerService;
    lockMap = cacheManagerService.getCache(LOCKMAP, CacheScope.CLUSTERREPLICATED);
  }
  /**
   * @param cacheManagerService
   */
  protected void unbindCacheManagerService(CacheManagerService cacheManagerService) {
    this.cacheManagerService = null;
    lockMap = null;
  }
 
 
}
