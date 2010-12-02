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
package org.sakaiproject.nakamura.http.i18n;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotSame;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class CapturingHttpServletResponseTest {
  @Mock HttpServletResponse response;

  ServletOutputStream sos;
  StringWriter sw;
  PrintWriter pw;
  ByteArrayOutputStream baos;
  CapturingHttpServletResponse i18nResponse;

  @Before
  public void setUp() throws Exception {
    sw = new StringWriter();
    pw = new PrintWriter(sw);
    when(response.getWriter()).thenReturn(pw);

    baos = new ByteArrayOutputStream();
    sos = new ServletOutputStream() {

      @Override
      public void write(int b) throws IOException {
        baos.write(b);
      }
    };
    when(response.getOutputStream()).thenReturn(sos);

    i18nResponse = new CapturingHttpServletResponse(response);
  }

  @Test
  public void getOutputStream() throws Exception {
    assertNotSame(sos, i18nResponse.getOutputStream());
  }

  @Test
  public void getWriter() throws Exception {
    assertNotSame(pw, i18nResponse.getWriter());
  }

  @Test
  public void writeToOutputStream() throws Exception {
    i18nResponse.getOutputStream().print("words in a test");
    assertEquals("words in a test", i18nResponse.toString());
  }

  @Test
  public void writeToWriter() throws Exception {
    i18nResponse.getWriter().print("words in a test");
    assertEquals("words in a test", i18nResponse.toString());
  }
}
