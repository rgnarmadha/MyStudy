package org.sakaiproject.nakamura.message.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MessageRouteImplTest {

  @Test
  public void testNullRoute() {
    MessageRouteImpl mri = new MessageRouteImpl(null);
    assertNull(mri.getRcpt());
    assertNull(mri.getTransport());
  }

  @Test
  public void testEmptyRoute() {
    MessageRouteImpl mri = new MessageRouteImpl("");
    assertEquals("internal", mri.getTransport());
    assertEquals("", mri.getRcpt());
  }

  @Test
  public void testInternalRoute() {
    MessageRouteImpl mri = new MessageRouteImpl(":admin");
    assertEquals("internal", mri.getTransport());
    assertEquals("admin", mri.getRcpt());
  }

  @Test
  public void testExternalRoute() {
    MessageRouteImpl mri = new MessageRouteImpl("smtp:admin@localhost");
    assertEquals("smtp", mri.getTransport());
    assertEquals("admin@localhost", mri.getRcpt());
  }
}
