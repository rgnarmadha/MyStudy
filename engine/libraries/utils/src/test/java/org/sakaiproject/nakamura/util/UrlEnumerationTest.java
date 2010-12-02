package org.sakaiproject.nakamura.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.net.URI;
import java.net.URL;
import java.util.Enumeration;

public class UrlEnumerationTest {

  @Test
  public void testUrlEnumeration() throws Exception {
    Enumeration<URL> eu = new UrlEnumeration(new URL("http://example.com"));
    assertTrue(eu.hasMoreElements());
    assertEquals(new URI("http://example.com"), eu.nextElement().toURI());
    assertFalse(eu.hasMoreElements());
    assertNull(eu.nextElement());
  }
}
