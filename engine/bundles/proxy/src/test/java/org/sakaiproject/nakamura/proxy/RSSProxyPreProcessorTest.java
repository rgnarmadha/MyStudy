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
package org.sakaiproject.nakamura.proxy;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class RSSProxyPreProcessorTest {
  
  private RSSProxyPreProcessor proxyPreProcessor;
  
  
  @Before
  public void setup() {
    proxyPreProcessor = new RSSProxyPreProcessor();
  }
  
  @Test
  public void testNameIsAsExpected() {
    assertEquals("rss", proxyPreProcessor.getName());
  }
  
  @Test
  public void processingLeavesOnlyAcceptStarStarHeader() throws Exception {
    //given
    Map<String, String>headers = new HashMap<String, String>();
    headers.put("Accept-Encoding", "gzip,deflate");
    headers.put("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.6; en-US; rv:1.9.2) Gecko/20100115 Firefox/3.6");
    
    //when
    proxyPreProcessor.preProcessRequest(null, headers, null);
    
    //then
    assertEquals(1, headers.size());
    assertEquals("*/*", headers.get("Accept"));
  }

}
