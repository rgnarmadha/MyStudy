/**
 * 
 */
package org.sakaiproject.nakamura.testutils.easymock;

import static org.easymock.EasyMock.reportMatcher;

import org.easymock.IArgumentMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class UnorderedArrayMatcher<T> implements IArgumentMatcher {
  private T[] expected;
  private static final Logger LOG = LoggerFactory.getLogger(UnorderedArrayMatcher.class);

  @edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"EI_EXPOSE_REP2"})
  public UnorderedArrayMatcher(T[] expected) {
    this.expected = expected;
  }

  public static <V> V[] aryUnorderedEq(V[] arg) {
    reportMatcher(new UnorderedArrayMatcher<V>(arg));
    return null;
  }

  public void appendTo(StringBuffer buffer) {
    buffer.append("aryUnorderedEq(");
    buffer.append(expected.getClass().getName());
    buffer.append(" with values \"");
    buffer.append(Arrays.toString(expected));
    buffer.append("\")");
  }

  public static <T> boolean unorderedArrayEquals(T[] as, T[] bs) {
    if (as.length != bs.length) {
      LOG.info("Array length mismatch. Expected: " + as.length + " got " + bs.length);
      return false;
    }
    Set<T> aSet = new HashSet<T>();
    Set<T> bSet = new HashSet<T>();
    for (int i = 0; i < as.length; i++) {
      aSet.add(as[i]);
      bSet.add(bs[i]);
    }
    return bSet.containsAll(aSet) && aSet.containsAll(bSet);
  }

  @SuppressWarnings("unchecked")
  public boolean matches(Object matchable) {
    if (matchable == null || !(matchable instanceof Object[])) {
      LOG.info("Other object was not an array");
      return false;
    }
    T[] other = (T[]) matchable;
    try {
      return unorderedArrayEquals(expected, other);
    } catch (ClassCastException e) {
      return false;
    }
  }

}
