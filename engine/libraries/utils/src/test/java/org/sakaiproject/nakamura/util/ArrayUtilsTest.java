package org.sakaiproject.nakamura.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class ArrayUtilsTest {

  @Test
  public void testCopy() {
    String[] from = { "Lorem", "ipsum", "dolor", "sit" };
    String[] to = new String[from.length];
    ArrayUtils.copy(from, to);
    // We should have two different objects
    assertFalse(from == to);
    assertArrayEquals(from, to);
  }
}
