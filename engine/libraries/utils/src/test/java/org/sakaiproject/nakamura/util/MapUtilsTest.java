package org.sakaiproject.nakamura.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;

import org.junit.Test;

import java.util.Map;

public class MapUtilsTest {

  @Test
  public void testConvertToImmutableMap() {
    String s = "Lorem=ipsum; dolor = sit ;amet=.";
    Map<String, String> m = MapUtils.convertToImmutableMap(s);
    assertTrue(m instanceof ImmutableMap);
    assertEquals("ipsum", m.get("Lorem"));
    assertEquals("sit", m.get("dolor"));
    assertEquals(".", m.get("amet"));
  }
}
