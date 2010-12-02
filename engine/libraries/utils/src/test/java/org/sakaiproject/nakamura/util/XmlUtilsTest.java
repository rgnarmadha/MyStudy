package org.sakaiproject.nakamura.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class XmlUtilsTest {

  @Test
  public void testEncode() {
    String xmlString = "Lorem<>&\"\t\n\ripsum";
    assertEquals(XmlUtils.encode(xmlString),
        "Lorem&lt;&gt;&amp;&quot;\t\n\ripsum");
    assertEquals(XmlUtils.encode("<"), "&lt;");
    assertEquals(XmlUtils.encode(">"), "&gt;");
    assertEquals(XmlUtils.encode("&"), "&amp;");
    assertEquals(XmlUtils.encode("\""), "&quot;");
    assertEquals(XmlUtils.encode("\t"), "\t");
    assertEquals(XmlUtils.encode("\n"), "\n");
    assertEquals(XmlUtils.encode("\r"), "\r");
    assertEquals(XmlUtils.encode("Lorem"), "Lorem");
    assertEquals(XmlUtils.encode(""), "");
    assertEquals(XmlUtils.encode(null), "");
  }
}
