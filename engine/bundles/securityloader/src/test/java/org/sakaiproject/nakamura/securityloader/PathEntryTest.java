/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.securityloader;

import junit.framework.Assert;

import org.apache.sling.commons.osgi.ManifestHeader.Entry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

/**
 *
 */
public class PathEntryTest {
  private static final String TEST_SECURITY_HEADER = "SLING-INF/acl/personal-acl.json;path:=/a;overwrite:=false;uninstall:=false;,"
      + "SLING-INF/acl2/personal-acl.json;overwrite:=true;uninstall:=false;path:=/b,"
      + "SLING-INF/acl3/personal-acl.json;overwrite:=false;uninstall:=true;path:=/c";
  @Mock
  private Entry entry;
  @Mock
  private Bundle bundle;

  @Before
  public void before() {

    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testPathEntryDefaults() {

    Mockito.when(entry.getValue()).thenReturn("/path");

    PathEntry pathEntry = new PathEntry(entry);
    Assert.assertEquals("/path", pathEntry.getPath());
    Assert.assertNull(pathEntry.getTarget());
    Assert.assertFalse(pathEntry.isOverwrite());
    Assert.assertFalse(pathEntry.isUninstall());

    Mockito.verify(entry).getValue();
    Mockito.verify(entry).getDirectiveValue(PathEntry.OVERWRITE_DIRECTIVE);
    Mockito.verify(entry).getDirectiveValue(PathEntry.UNINSTALL_DIRECTIVE);
    Mockito.verify(entry).getDirectiveValue(PathEntry.PATH_DIRECTIVE);

  }

  @Test
  public void testPathEntryOverwrite() {

    Mockito.when(entry.getValue()).thenReturn("/path2");
    Mockito.when(entry.getDirectiveValue(PathEntry.PATH_DIRECTIVE)).thenReturn(
        "/targetpath");
    Mockito.when(entry.getDirectiveValue(PathEntry.OVERWRITE_DIRECTIVE)).thenReturn(
        "true");
    Mockito.when(entry.getDirectiveValue(PathEntry.UNINSTALL_DIRECTIVE)).thenReturn(
        "false");

    PathEntry pathEntry = new PathEntry(entry);
    Assert.assertEquals("/path2", pathEntry.getPath());
    Assert.assertEquals("/targetpath", pathEntry.getTarget());
    Assert.assertTrue(pathEntry.isOverwrite());
    Assert.assertFalse(pathEntry.isUninstall());

    Mockito.verify(entry).getValue();
    Mockito.verify(entry).getDirectiveValue(PathEntry.OVERWRITE_DIRECTIVE);
    Mockito.verify(entry).getDirectiveValue(PathEntry.UNINSTALL_DIRECTIVE);
    Mockito.verify(entry).getDirectiveValue(PathEntry.PATH_DIRECTIVE);

  }

  @Test
  public void testPathEntryUninstall() {

    Mockito.when(entry.getValue()).thenReturn("/path2");
    Mockito.when(entry.getDirectiveValue(PathEntry.PATH_DIRECTIVE)).thenReturn(
        "/targetpath");
    Mockito.when(entry.getDirectiveValue(PathEntry.OVERWRITE_DIRECTIVE)).thenReturn(
        "false");
    Mockito.when(entry.getDirectiveValue(PathEntry.UNINSTALL_DIRECTIVE)).thenReturn(
        "true");

    PathEntry pathEntry = new PathEntry(entry);
    Assert.assertEquals("/path2", pathEntry.getPath());
    Assert.assertEquals("/targetpath", pathEntry.getTarget());
    Assert.assertFalse(pathEntry.isOverwrite());
    Assert.assertTrue(pathEntry.isUninstall());

    Mockito.verify(entry).getValue();
    Mockito.verify(entry).getDirectiveValue(PathEntry.OVERWRITE_DIRECTIVE);
    Mockito.verify(entry).getDirectiveValue(PathEntry.UNINSTALL_DIRECTIVE);
    Mockito.verify(entry).getDirectiveValue(PathEntry.PATH_DIRECTIVE);

  }

  @Test
  public void testGetContentPaths() {
    Dictionary<String, String> headers = new Hashtable<String, String>();
    headers.put(PathEntry.SECURITY_HEADER, TEST_SECURITY_HEADER);
    Mockito.when(bundle.getHeaders()).thenReturn(headers);
    Iterator<PathEntry> pei = PathEntry.getContentPaths(bundle);
    Assert.assertTrue(pei.hasNext());
    PathEntry pe = pei.next();
    checkEntry(pe, "SLING-INF/acl/personal-acl.json", "/a", false, false);
    Assert.assertTrue(pei.hasNext());
    pe = pei.next();
    checkEntry(pe, "SLING-INF/acl2/personal-acl.json", "/b", true, false);
    Assert.assertTrue(pei.hasNext());
    pe = pei.next();
    checkEntry(pe, "SLING-INF/acl3/personal-acl.json", "/c", false, true);
    Assert.assertFalse(pei.hasNext());

  }
  @Test
  public void testGetNoContentPaths() {
    Dictionary<String, String> headers = new Hashtable<String, String>();
    Mockito.when(bundle.getHeaders()).thenReturn(headers);
    Iterator<PathEntry> pei = PathEntry.getContentPaths(bundle);
    Assert.assertNull(pei);
  }

  /**
   * @param pe
   * @param string
   * @param string2
   * @param b
   * @param c
   */
  private void checkEntry(PathEntry pe, String path, String target, boolean overwrite,
      boolean uninstall) {
    Assert.assertEquals(path, pe.getPath());
    Assert.assertEquals(target, pe.getTarget());
    Assert.assertEquals(overwrite, pe.isOverwrite());
    Assert.assertEquals(uninstall, pe.isUninstall());

  }

}
