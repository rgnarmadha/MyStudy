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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class I18nFilterTest {
  @Mock FilterConfig config;
  @Mock Node bundlesNode;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) Node langNode;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) Node deutschNode;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) Node defaultNode;
  @Mock SlingHttpServletRequest request;
  @Mock HttpServletResponse response;
  @Mock ResourceResolver resourceResolver;
  @Mock FilterChain chain;
  @Mock JackrabbitSession session;
  @Mock UserManager um;
  @Mock Iterator<String> propNames;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) Authorizable sessionUser;

  Properties props;
  I18nFilter filter;
  StringWriter sw;
  ByteArrayOutputStream baos;

  @Before
  public void setUp() throws Exception {
    // assume default settings
    props = new Properties();

    filter = new I18nFilter();
    filter.init(config);
    filter.modified(props);

    sw = new StringWriter();
    baos = new ByteArrayOutputStream();

    when(request.getLocale()).thenReturn(new Locale("en", "US"));

    when(request.getResourceResolver()).thenReturn(resourceResolver);
    when(resourceResolver.adaptTo(Session.class)).thenReturn(session);

    when(session.getUserManager()).thenReturn(um);
    when(session.getNode(I18nFilter.DEFAULT_BUNDLES_PATH)).thenReturn(bundlesNode);

    when(um.getAuthorizable(Matchers.anyString())).thenReturn(sessionUser);
    when(sessionUser.getPropertyNames()).thenReturn(propNames);

    when(bundlesNode.getNode(Locale.getDefault().toString() + ".json")).thenReturn(langNode);
    when(langNode.getNode("jcr:content").getProperty("jcr:data").getString()).thenReturn(
        "{'REPLACE_ME':'Yay, In the language bundle!'}");

    when(bundlesNode.getNode("de_DE.json")).thenReturn(deutschNode);
    when(deutschNode.getNode("jcr:content").getProperty("jcr:data").getString()).thenReturn(
        "{'REPLACE_ME':'Wie geht es ihnen?'}");

    when(bundlesNode.getNode("default.json")).thenReturn(defaultNode);
    when(defaultNode.getNode("jcr:content").getProperty("jcr:data").getString()).thenReturn(
        "{'REPLACE_ME':'Yay, In the default bundle!', 'REPLACE_ME_DEFAULT':'Default replacement'}");

    when(response.getWriter()).thenReturn(new PrintWriter(sw));
  }

  @After
  public void tearDown() {
    filter.destroy();
  }

  @Test
  public void dontFilterIfRawRequested() throws Exception {
    when(request.getParameter("raw")).thenReturn("true");

    filter.doFilter(request, response, chain);

    ArgumentCaptor<ServletResponse> acResponse = ArgumentCaptor
        .forClass(ServletResponse.class);
    verifyZeroInteractions(response);
    verify(chain).doFilter(eq(request), acResponse.capture());

    assertFalse(acResponse.getClass().isInstance(CapturingHttpServletResponse.class));
  }

  @Test
  public void dontFilterUnmatchedPath() throws Exception {
    when(request.getPathInfo()).thenReturn("/unfiltered");

    filter.doFilter(request, response, chain);

    ArgumentCaptor<ServletResponse> acResponse = ArgumentCaptor
        .forClass(ServletResponse.class);
    verifyZeroInteractions(response);
    verify(chain).doFilter(eq(request), acResponse.capture());

    assertFalse(acResponse.getClass().isInstance(CapturingHttpServletResponse.class));
  }

  @Test
  public void dontFilterEmptyOutput() throws Exception {
    when(request.getPathInfo()).thenReturn("/dev/index.html");

    filter.doFilter(request, response, chain);

    ArgumentCaptor<ServletResponse> acResponse = ArgumentCaptor
        .forClass(ServletResponse.class);
    verifyZeroInteractions(response);
    verify(chain).doFilter(eq(request), acResponse.capture());

    assertFalse(acResponse.getClass().isInstance(CapturingHttpServletResponse.class));
  }

  @Test
  public void filterMatchedPathNoFoundMessageKeys() throws Exception {
    when(request.getPathInfo()).thenReturn("/dev/index.html");

    // inject some data into the response for the filter to use
    writeToResponse("__MSG__BAD_REPLACEMENT__", false);

    filter.doFilter(request, response, chain);

    ArgumentCaptor<ServletResponse> acResponse = ArgumentCaptor
        .forClass(ServletResponse.class);
    verify(chain).doFilter(eq(request), acResponse.capture());
    verify(response, atLeastOnce()).getWriter();

    assertTrue(acResponse.getValue() instanceof CapturingHttpServletResponse);
    String output = sw.toString();
    assertNotNull(output);
    assertFalse(output.contains("__MSG__BAD_REPLACEMENT__"));
    assertTrue(output.contains("[MESSAGE KEY NOT FOUND 'BAD_REPLACEMENT']"));
  }

  @Test
  public void filterMatchedPathDontShowMissingKeys() throws Exception {
    props.put(I18nFilter.SHOW_MISSING_KEYS, Boolean.FALSE);
    filter.modified(props);

    when(request.getPathInfo()).thenReturn("/dev/index.html");

    // inject some data into the response for the filter to use
    writeToResponse("__MSG__BAD_REPLACEMENT__", false);

    filter.doFilter(request, response, chain);

    ArgumentCaptor<ServletResponse> acResponse = ArgumentCaptor
        .forClass(ServletResponse.class);
    verify(chain).doFilter(eq(request), acResponse.capture());
    verify(response, atLeastOnce()).getWriter();

    assertTrue(acResponse.getValue() instanceof CapturingHttpServletResponse);
    String output = sw.toString();
    assertNotNull(output);
    assertFalse(output.contains("__MSG__BAD_REPLACEMENT__"));
    assertFalse(output.contains("[MESSAGE KEY NOT FOUND 'BAD_REPLACEMENT']"));
  }

  @Test
  public void filterMatchedPathDefaultBundle() throws Exception {
    when(request.getPathInfo()).thenReturn("/dev/index.html");

    // inject some data into the response for the filter to use
    writeToResponse("__MSG__REPLACE_ME_DEFAULT__", false);

    filter.doFilter(request, response, chain);

    ArgumentCaptor<ServletResponse> acResponse = ArgumentCaptor
        .forClass(ServletResponse.class);
    verify(chain).doFilter(eq(request), acResponse.capture());
    verify(response, atLeastOnce()).getWriter();

    assertTrue(acResponse.getValue() instanceof CapturingHttpServletResponse);
    String output = sw.toString();
    assertNotNull(output);
    assertFalse(output.contains("__MSG__REPLACE_ME_DEFAULT__"));
    assertTrue(output.contains("Default replacement"));
  }

  @Test
  public void filterMatchedPathLangBundle() throws Exception {
    when(request.getPathInfo()).thenReturn("/dev/index.html");

    // inject some data into the response for the filter to use
    writeToResponse("__MSG__REPLACE_ME__", false);

    filter.doFilter(request, response, chain);

    ArgumentCaptor<ServletResponse> acResponse = ArgumentCaptor
        .forClass(ServletResponse.class);
    verify(chain).doFilter(eq(request), acResponse.capture());
    verify(response, atLeastOnce()).getWriter();

    assertTrue(acResponse.getValue() instanceof CapturingHttpServletResponse);
    String output = sw.toString();
    assertNotNull(output);
    assertFalse(output.contains("__MSG__REPLACE_ME__"));
    assertTrue(output.contains("Yay, In the language bundle!"));
  }

  @Test
  public void filterMatchedPathRepeatedKey() throws Exception {
    when(request.getPathInfo()).thenReturn("/dev/index.html");

    // inject some data into the response for the filter to use
    writeToResponse("__MSG__REPLACE_ME__, __MSG__REPLACE_ME__", false);

    filter.doFilter(request, response, chain);

    ArgumentCaptor<ServletResponse> acResponse = ArgumentCaptor
        .forClass(ServletResponse.class);
    verify(chain).doFilter(eq(request), acResponse.capture());
    verify(response, atLeastOnce()).getWriter();

    assertTrue(acResponse.getValue() instanceof CapturingHttpServletResponse);
    String output = sw.toString();
    assertNotNull(output);
    assertFalse(output.contains("MSG__REPLACE_ME"));
    assertTrue(output.contains("Yay, In the language bundle!"));
  }

  @Test
  public void filterMatchedPathFailToOutputStream() throws Exception {
    when(request.getPathInfo()).thenReturn("/dev/index.html");

    // inject some data into the response for the filter to use
    writeToResponse("__MSG__REPLACE_ME__", true);

    // throw an exception when getting the writer so that an attempt is made to get the
    // output stream
    reset(response);
    when(response.getWriter()).thenThrow(new IllegalStateException());
    when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
      @Override
      public void write(int arg0) throws IOException {
        baos.write(arg0);
      }
    });

    filter.doFilter(request, response, chain);

    ArgumentCaptor<ServletResponse> acResponse = ArgumentCaptor
        .forClass(ServletResponse.class);
    verify(chain).doFilter(eq(request), acResponse.capture());
    verify(response, atLeastOnce()).getWriter();

    assertTrue(acResponse.getValue() instanceof CapturingHttpServletResponse);
    String output = sw.toString();
    assertTrue(StringUtils.isBlank(output));

    output = baos.toString("UTF-8");
    assertFalse(StringUtils.isBlank(output));
    assertFalse(output.contains("__MSG__REPLACE_ME__"));
    assertTrue(output.contains("Yay, In the language bundle!"));
  }

  @Test
  public void getLocaleFromRequest() throws Exception {
    // set the locale property in the authorizable
    when(request.getParameter(I18nFilter.PARAM_LANGUAGE)).thenReturn("de_DE");

    // setup a filtered path
    when(request.getPathInfo()).thenReturn("/dev/index.html");

    // inject some data into the response for the filter to use
    writeToResponse("__MSG__REPLACE_ME__", false);

    // run the filter
    filter.doFilter(request, response, chain);

    ArgumentCaptor<ServletResponse> acResponse = ArgumentCaptor
        .forClass(ServletResponse.class);
    // verify that the proper locale was searched for in JCR
    verify(bundlesNode).getNode("de_DE.json");
    verify(chain).doFilter(eq(request), acResponse.capture());
    verify(response, atLeastOnce()).getWriter();

    assertTrue(acResponse.getValue() instanceof CapturingHttpServletResponse);
    String output = sw.toString();
    assertNotNull(output);
    assertFalse(output.contains("MSG__REPLACE_ME"));
    assertTrue(output.contains("Wie geht es ihnen?"));
  }

  private void writeToResponse(final String key, final boolean useOutputStream) throws IOException, ServletException {
    doAnswer(new Answer<Object>() {
      public Object answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        ServletResponse response = (ServletResponse) args[1];
        try {
          if (!useOutputStream) {
            response.getWriter().write("<html><body>" + key + "</body></html>");
          } else {
            response.getOutputStream().write(
                ("<html><body>" + key + "</body></html>").getBytes("UTF-8"));
          }
        } catch (IOException e) {
          // doesn't matter
        }
        return null;
      }
    }).when(chain).doFilter(isA(ServletRequest.class), isA(ServletResponse.class));
  }
}
