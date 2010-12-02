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
package org.sakaiproject.nakamura.version.impl;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.apache.sling.api.request.RequestPathInfo;
import org.junit.Test;
import org.sakaiproject.nakamura.version.impl.VersionRequestPathInfo;

/**
 * 
 */
public class VersionRequestPathInfoTest {

  @Test
  public void testGetVersionName() {
    assertEquals("1.1", VersionRequestPathInfo.getVersionName("version.,1.1,.tidy","json"));
    assertEquals("1", VersionRequestPathInfo.getVersionName("version.1.tidy","json"));
    assertEquals("1.1.tidy.json", VersionRequestPathInfo.getVersionName("version.,1.1.tidy","json"));
    assertEquals("1.1", VersionRequestPathInfo.getVersionName("version.,1.1",null));
    assertEquals("1.1", VersionRequestPathInfo.getVersionName("version.,1.1,",null));
  }

  @Test
  public void testRemoveVersionName() {
    assertEquals("tidy.json", VersionRequestPathInfo.removeVersionName("version.,1.1,.tidy","json"));
    assertEquals("tidy.json", VersionRequestPathInfo.removeVersionName("version.1.tidy","json"));
    assertEquals("json", VersionRequestPathInfo.removeVersionName("version.,1.1.tidy,","json"));
    assertEquals(null, VersionRequestPathInfo.removeVersionName("version.,1.1",null));
    assertEquals(null, VersionRequestPathInfo.removeVersionName("version.,1.1,",null));
  }

  @Test
  public void testRemoveVersion() {
    assertEquals("sdfsdfs.sdfsdf.sdfsdfsdf", VersionRequestPathInfo.removeVersionName("version.sdfsdfds.sdfsdfs.sdfsdf.sdfsdfsdf", null));
    assertEquals("sdfsdfs.sdfsdf.sdfsdfsdf", VersionRequestPathInfo.removeVersionName("version.,sdf...sdfds,.sdfsdfs.sdfsdf.sdfsdfsdf", null));
    assertEquals(null, VersionRequestPathInfo.removeVersionName("version.,sdf...sdfds,", null));
    assertEquals(null, VersionRequestPathInfo.removeVersionName("version.sdf", null));
    assertEquals("sdfsdfs.sdfsdf.sdfsdfsdf", VersionRequestPathInfo.removeVersionName("version.sdfsdfds.sdfsdfs.sdfsdf.sdfsdfsdf", null));
    assertEquals("sdfsdfs.sdfsdf.sdfsdfsdf", VersionRequestPathInfo.removeVersionName("version.,sdf...sdfds,.sdfsdfs.sdfsdf.sdfsdfsdf", null));
    assertEquals(null, VersionRequestPathInfo.removeVersionName("version.,sdf...sdfds,",null));
    assertEquals(null, VersionRequestPathInfo.removeVersionName("version.sdf",null));
  }

  @Test
  public void testGetVersionPathInfoComma() {
    RequestPathInfo requestPathInfo = createMock(RequestPathInfo.class);
    
    expect(requestPathInfo.getResourcePath()).andReturn("/test/path/node").anyTimes();
    expect(requestPathInfo.getSuffix()).andReturn(null).anyTimes();
    expect(requestPathInfo.getSelectorString()).andReturn("version.,1.1,.5").anyTimes();
    expect(requestPathInfo.getExtension()).andReturn("json").anyTimes();
    
    replay(requestPathInfo);
    VersionRequestPathInfo versionRequestPathInfo = new VersionRequestPathInfo(requestPathInfo);
    assertEquals("json",versionRequestPathInfo.getExtension());
    assertEquals("/test/path/node",versionRequestPathInfo.getResourcePath());
    assertArrayEquals(new String[]{"5"},versionRequestPathInfo.getSelectors());
    assertEquals("5",versionRequestPathInfo.getSelectorString());
    assertEquals(null,versionRequestPathInfo.getSuffix());
    verify(requestPathInfo);
  }
  @Test
  public void testGetVersionPathInfoCommaLonger() {
    RequestPathInfo requestPathInfo = createMock(RequestPathInfo.class);
    
    expect(requestPathInfo.getResourcePath()).andReturn("/test/path/node").anyTimes();
    expect(requestPathInfo.getSuffix()).andReturn(null).anyTimes();
    expect(requestPathInfo.getSelectorString()).andReturn("version.,1.1,.5.tidy").anyTimes();
    expect(requestPathInfo.getExtension()).andReturn("json").anyTimes();
    
    replay(requestPathInfo);
    VersionRequestPathInfo versionRequestPathInfo = new VersionRequestPathInfo(requestPathInfo);
    assertEquals("json",versionRequestPathInfo.getExtension());
    assertEquals("/test/path/node",versionRequestPathInfo.getResourcePath());
    assertArrayEquals(new String[]{"5","tidy"},versionRequestPathInfo.getSelectors());
    assertEquals("5.tidy",versionRequestPathInfo.getSelectorString());
    assertEquals(null,versionRequestPathInfo.getSuffix());
    verify(requestPathInfo);
  }

}
