package org.sakaiproject.nakamura.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class IOUtilsTest {

  @Test
  public void testReadFully() throws Exception {
    InputStream is = this.getClass().getResourceAsStream("lipsum.txt");
    assertNotNull(is);
    String lipsum = IOUtils.readFully(is, "UTF-8");
    InputStream verify = this.getClass().getResourceAsStream("lipsum.txt");
    for (Byte b : lipsum.getBytes()) {
      assertEquals(b.intValue(), verify.read());
    }
    verify.close();
  }

  @Test
  public void testStream() throws Exception {
    InputStream from = this.getClass().getResourceAsStream("lipsum.txt");

    ByteArrayOutputStream to = new ByteArrayOutputStream();
    IOUtils.stream(from, to);
    String lipsum = to.toString("UTF-8");
    assertEquals("Lorem", lipsum.substring(0, 5));

  }
}
