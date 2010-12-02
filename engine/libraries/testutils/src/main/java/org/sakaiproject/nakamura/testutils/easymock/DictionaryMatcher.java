/**
 * 
 */
package org.sakaiproject.nakamura.testutils.easymock;

import static org.easymock.EasyMock.reportMatcher;

import org.easymock.IArgumentMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Enumeration;

public class DictionaryMatcher<K, V> implements IArgumentMatcher {
  private Dictionary<K, V> expected;
  private boolean allowUnorderedSubkeys;
  private static final Logger LOG = LoggerFactory.getLogger(DictionaryMatcher.class);

  public DictionaryMatcher(Dictionary<K, V> dictionary, boolean allowUnorderedSubkeys) {
    this.expected = dictionary;
    this.allowUnorderedSubkeys = allowUnorderedSubkeys;
  }

  public static <T, U> Dictionary<T, U> eqDictionary(Dictionary<T, U> arg) {
    reportMatcher(new DictionaryMatcher<T, U>(arg, false));
    return null;
  }

  public static <T, U> Dictionary<T, U> eqDictionaryUnorderedSubkeys(Dictionary<T, U> arg) {
    reportMatcher(new DictionaryMatcher<T, U>(arg, true));
    return null;
  }

  public void appendTo(StringBuffer buffer) {
    buffer.append("eqDictionary(");
    buffer.append(expected.getClass().getName());
    buffer.append(" with values \"");
    buffer.append(expected.toString());
    buffer.append("\")");
  }

  private static <K> boolean deepEquals(K key, Object a, Object b, boolean allowUnordered) {
    if (a instanceof Object[]) {
      if (!(b instanceof Object[])) {
        LOG.info("Other element was not an array for key: " + key);
        return false;
      }
      Object[] as = (Object[]) a;
      Object[] bs = (Object[]) b;
      if (as.length != bs.length) {
        LOG.info("Array length mismatch. Expected: " + as.length + " got " + bs.length);
        return false;
      }
      if (!allowUnordered) {
        for (int i = 0; i < as.length; i++) {
          if (!deepEquals(key, as[i], bs[i], allowUnordered)) {
            return false;
          }
        }
      } else {
        return UnorderedArrayMatcher.unorderedArrayEquals(as, bs);
      }
    } else {
      if (!a.equals(b)) {
        LOG.info("Elements were not equal. Expected " + a + " got " + b);
        return false;
      }
    }
    return true;
  }

  public boolean matches(Object matchable) {
    if (matchable == null || !(matchable instanceof Dictionary)) {
      LOG.info("Other object was not a dictionary");
      return false;
    }
    Dictionary<?, ?> other = (Dictionary<?, ?>) matchable;
    try {
      if (other.size() != expected.size()) {
        LOG.info("Dictionaries different sizes. Expected " + expected.size() + " got "
            + other.size());
        return false;
      }
      Enumeration<K> keys = expected.keys();
      while (keys.hasMoreElements()) {
        K key = keys.nextElement();
        if (expected.get(key) == null && other.get(key) == null) {
          continue;
        }
        if (expected.get(key) != null && other.get(key) == null) {
          LOG.info("Submitting dictionary missing key: " + key);
          return false;
        }
        if (!deepEquals(key, expected.get(key), other.get(key), allowUnorderedSubkeys)) {
          return false;
        }
      }
    } catch (ClassCastException e) {
      return false;
    }
    return true;
  }

}
