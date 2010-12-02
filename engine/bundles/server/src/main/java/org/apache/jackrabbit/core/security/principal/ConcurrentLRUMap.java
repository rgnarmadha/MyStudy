package org.apache.jackrabbit.core.security.principal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A reasonably dumb LRU map, but its concurrent.
 * @param <K>
 * @param <V>
 */
public class ConcurrentLRUMap<K, V> implements Map<K, V> {

  public class Holder<T> {

    protected long last;
    protected V value;
    private K key;

    public Holder(K key, V value) {
      this.key = key;
      this.value = value;
      this.last = System.currentTimeMillis();
    }

    public int compareTo(Holder<T> o) {
      return (int) (this.last - o.last);
    }

    @Override
    public boolean equals(Object obj) {
      try {
        @SuppressWarnings("unchecked")
        Holder<T> t = (Holder<T>) obj;
        return value.equals(t.value);
      } catch (ClassCastException e) {
        return false;
      }
    }

  }

  /**
   * 
   */
  private static final long serialVersionUID = 6397790801684912025L;
  private Map<K, Holder<V>> delegate = new ConcurrentHashMap<K, Holder<V>>();
  private int maxSize = 100;

  public ConcurrentLRUMap() {
  }

  public ConcurrentLRUMap(int size) {
    maxSize = size;
  }

  public int size() {
    return delegate.size();
  }

  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  public boolean containsKey(Object key) {
    return delegate.containsKey(key);
  }

  public boolean containsValue(Object value) {
    @SuppressWarnings("unchecked")
    Holder<V> v = new Holder<V>(null, (V) value);
    return delegate.containsValue(v);
  }

  public V put(K key, V value) {
    if ( delegate.size() > maxSize ) {
      List<Holder<V>> l = new ArrayList<Holder<V>>(delegate.values());
      Collections.sort(l,new Comparator<Holder<V>>() {

        public int compare(Holder<V> o1, Holder<V> o2) {
          return (int) (o1.last - o2.last);
        }
      });
      int i = 0;
      while ( delegate.size() > maxSize && i < l.size() ) {
        delegate.remove(l.get(i++).key);
      }
    }
    Holder<V> v = delegate.put(key, new Holder<V>(key, value));
    if (v == null) {
      return null;
    }
    return v.value;
  }

  public V remove(Object key) {
    Holder<V> v = delegate.remove(key);
    if (v == null) {
      return null;
    }
    return v.value;
  }

  public void putAll(Map<? extends K, ? extends V> t) {
    for ( java.util.Map.Entry<? extends K, ? extends V> e : t.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  public void clear() {
    delegate.clear();
  }

  public Set<K> keySet() {
    return delegate.keySet();
  }

  public Collection<V> values() {
    List<V> values = new ArrayList<V>();
    for (Holder<V> v : delegate.values()) {
      values.add(v.value);
    }
    return values;
  }

  public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> set = new HashSet<Map.Entry<K, V>>();
    for (final Entry<K, Holder<V>> e : delegate.entrySet()) {
      set.add(new Entry<K, V>() {

        public K getKey() {
          return e.getKey();
        }

        public V getValue() {
          return ((Holder<V>) e.getValue()).value;
        }

        public V setValue(V value) {
          Holder<V> h = e.setValue((Holder<V>) new Holder<V>(e.getKey(), value));
          if (h == null) {
            return null;
          }
          return h.value;
        }
      });
    }
    return set;
  }

  public V get(Object key) {
    Holder<V> v = delegate.get(key);
    if (v == null) {
      return null;
    }
    v.last = System.currentTimeMillis();
    return v.value;
  }
}
