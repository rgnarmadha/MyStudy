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
package org.sakaiproject.nakamura.util.parameters;

import junit.framework.Assert;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;


/**
 *
 */
public class UtilTest  {

  @Test
  public void testFromIdentityEncodedString() {
    byte[] b = Util.fromIdentityEncodedString("ABCDE");
    Assert.assertEquals("ABCDE".length(), b.length);
    Assert.assertEquals('A', b[0]);
    Assert.assertEquals('B', b[1]);
    Assert.assertEquals('C', b[2]);
    Assert.assertEquals('D', b[3]);
    Assert.assertEquals('E', b[4]);    
  }
  @Test
  public void testFromIdentityEncodedStringByte() {
    String s = Util.toIdentityEncodedString(new byte[]{'A','B','C','D','E'});
    Assert.assertEquals("ABCDE", s);
  }
  
  
  @Test
  public void testGetInputStream() throws IOException {
    InputStream in = Util.getInputStream("ABCDE");
    Assert.assertEquals('A', in.read());
    Assert.assertEquals('B', in.read());
    Assert.assertEquals('C', in.read());
    Assert.assertEquals('D', in.read());
    Assert.assertEquals('E', in.read());    
    Assert.assertEquals(-1, in.read());    
  }
  
  
}
