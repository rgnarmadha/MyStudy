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
package org.apache.sling.jcr.jackrabbit.server.index;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


/**
 *
 */
public class TermCloudExtractorTest {

  static class L1 {
    @SuppressWarnings("unused")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"URF_UNREAD_FIELD"})
    private L2 a = new L2();
  }
  static class L2 {
    @SuppressWarnings("unused")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"URF_UNREAD_FIELD"})
    private L3 b = new L3();
  }
  static class L3 {
    @SuppressWarnings("unused")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"URF_UNREAD_FIELD"})
    private L4 c = new L4();
  }
  static class L4 {
    @SuppressWarnings("unused")
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value={"URF_UNREAD_FIELD"})
    private String d = "hello";
  }
  
  @Test
  public void testPrivateFieldReflect() {
    L1 obj = new L1();
    TermCloudExtractor termCloudExtractor = new  TermCloudExtractor();
    String e = termCloudExtractor.adaptTo(obj, "a","b","c","d" );
    assertEquals("hello",e);
  }
}
