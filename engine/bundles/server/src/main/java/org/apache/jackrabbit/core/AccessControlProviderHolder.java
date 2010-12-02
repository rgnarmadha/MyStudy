package org.apache.jackrabbit.core;

import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.WeakHashMap;


public class AccessControlProviderHolder  {
  private static class ReferenceQueueHandler extends Thread {

    ReferenceQueueHandler(ThreadGroup g, String name) {
      super(g, name);
    }
    public void run() {
      for(;;) {
        try {
          AccessControlProviderReference ref = (AccessControlProviderReference) queue.remove(0);
          if ( ref != null ) {
            ref.close();
          }
          Thread.yield();
        } catch (IllegalArgumentException e) {
          LOGGER.debug(e.getMessage(),e);
          e.printStackTrace();
        } catch (InterruptedException e) {
          LOGGER.debug(e.getMessage(),e);
        }
      }
    };
  }



  static final Logger LOGGER = LoggerFactory.getLogger(AccessControlProviderHolder.class);



  static {
    ThreadGroup tg = Thread.currentThread().getThreadGroup();
    for (ThreadGroup tgn = tg;
         tgn != null;
         tg = tgn, tgn = tg.getParent());
    Thread handler = new ReferenceQueueHandler(tg, "ThreadBoundHolderCleaner");
    handler.setPriority(Thread.MAX_PRIORITY);
    handler.setDaemon(true);
    handler.start();
  }

  public static ReferenceQueue<AccessControlProvider> queue = new ReferenceQueue<AccessControlProvider>();


  public class AccessControlProviderReference extends SoftReference<AccessControlProvider> {

    private SystemSession systemSession;
    private AccessControlProvider referent;
    private long lastUsed;
    private int nused;

    public AccessControlProviderReference(AccessControlProvider provider,
        SystemSession systemSession) {
      super(provider, queue);
      this.systemSession = systemSession;
      this.referent = provider;
      used();
    }



    protected void close() {
      if ( referent != null) {
        LOGGER.debug("{} Closing Provider {}", workspaceName, referent);
        referent.close();
        referent = null;
      }
      if ( systemSession != null ) {
        LOGGER.debug("{} Closing Session {}", workspaceName, systemSession);
        systemSession.logout();
        systemSession = null;
      }
    }



    public void used() {
      lastUsed = System.currentTimeMillis();
      nused++;
    }

  }



  /**
   * This provides the lookup on thread, ensuring that when the thread disappears the
   * AccessProvider disappears from the map. If we are running short of memory, the SoftReference will be broken by the GC and the AccessControlProvider closed.
   */
  private WeakHashMap<Thread, AccessControlProviderReference> threadMap = new WeakHashMap<Thread, AccessControlProviderReference>();
  private String workspaceName;
  private long maxAge;
  private int maxUsed;
  private List<AccessControlProviderReference> removeQueue = new ArrayList<AccessControlProviderReference>();

  public AccessControlProviderHolder(String workspaceName, long maxAge, int maxUsed ) {
    this.workspaceName = workspaceName;
    this.maxAge = maxAge;
    this.maxUsed = maxUsed;
  }

  public void close() {

      for (AccessControlProviderReference acpc : threadMap.values()) {
        acpc.close();
      }
      threadMap.clear();
  }

  public AccessControlProvider get() {
    synchronized (threadMap) {
      clean();
      AccessControlProviderReference ref = threadMap.get(Thread.currentThread());
      if ( ref == null ) {
        return null;
      } else {
        ref.used();
        return ref.get();
      }
    }
  }

  private void clean() {
    long evictBefore = System.currentTimeMillis() - maxAge;
    long closeBefore = System.currentTimeMillis() - 30000L;
    List<Thread> toRemove = new ArrayList<Thread>();
    for ( Entry<Thread, AccessControlProviderReference> e : threadMap.entrySet() ) {
      AccessControlProviderReference r = e.getValue();
      if ( r != null ) {
        if ( r.nused > maxUsed || r.lastUsed < evictBefore ) {
          toRemove.add(e.getKey());
        }
      } else {
        toRemove.add(e.getKey());
      }
    }
    //create a temp list to add new items to and check
    List<AccessControlProviderReference> toCheckRemove = new ArrayList<AccessControlProviderReference>(removeQueue);
    removeQueue = new ArrayList<AccessControlProviderReference>();
    for ( Thread t : toRemove ) {
      toCheckRemove.add(threadMap.remove(t));
    }
    // check which ones to save, and which ones to close
    for ( AccessControlProviderReference acp : toCheckRemove  ) {
      if ( acp != null ) {
        if ( acp.lastUsed < closeBefore ) {
          LOGGER.debug("{} Evicting  {} size {} ", new Object[] { workspaceName, acp, threadMap.size()});
          acp.close();          
        } else {
          removeQueue.add(acp);
        }
      }
    }
  }

  public void set(AccessControlProvider provider, SystemSession systemSession) {
    synchronized (threadMap) {
      clean();
      AccessControlProviderReference acpc = new AccessControlProviderReference(provider,systemSession);
      threadMap.put(Thread.currentThread(), acpc);
      LOGGER.debug("{} Added Access Control Provider for {} size {} ", new Object[]{ workspaceName, acpc, threadMap.size()});
    }
  }


}
