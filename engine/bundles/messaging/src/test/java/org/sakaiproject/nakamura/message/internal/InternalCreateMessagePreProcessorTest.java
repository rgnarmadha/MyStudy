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
package org.sakaiproject.nakamura.message.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.junit.Test;
import org.sakaiproject.nakamura.api.message.MessageConstants;
import org.sakaiproject.nakamura.api.message.MessagingException;

/**
 *
 */
public class InternalCreateMessagePreProcessorTest {

  @Test
  public void testCheck() {

    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    RequestParameter param = mock(RequestParameter.class);
    when(request.getRequestParameter(MessageConstants.PROP_SAKAI_TO)).thenReturn(param);

    InternalCreateMessagePreProcessor proc = new InternalCreateMessagePreProcessor();
    proc.checkRequest(request);

  }

  @Test
  public void testBadCheck() {

    SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
    when(request.getRequestParameter(MessageConstants.PROP_SAKAI_TO)).thenReturn(null);

    InternalCreateMessagePreProcessor proc = new InternalCreateMessagePreProcessor();
    try {
      proc.checkRequest(request);
      fail("This should have thrown a MessagingException.");
    } catch (MessagingException e) {
      assertEquals(400, e.getCode());
    }

  }
}
