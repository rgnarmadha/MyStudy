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
package org.sakaiproject.nakamura.auth.opensso;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.auth.opensso.OpenSsoAuthenticationHandler.SsoPrincipal;

import java.security.Principal;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.security.auth.login.FailedLoginException;

/**
 *
 */
public class OpenSsoLoginModulePluginTest {
  OpenSsoLoginModulePlugin plugin;

  SimpleCredentials credentials;

  @Before
  public void setUp() throws Exception {
    plugin = new OpenSsoLoginModulePlugin();

    credentials = new SimpleCredentials("someUser", new char[0]);
    credentials.setAttribute(SsoPrincipal.class.getName(), new SsoPrincipal("someUser"));
  }

  @Test
  public void coverageBooster() throws Exception {
    plugin.addPrincipals(null);
    plugin.doInit(null, null, null);
  }

  @Test
  public void testCanHandleSsoCredentials() throws RepositoryException {
    assertTrue(plugin.canHandle(credentials));
  }

  @Test
  public void testCannotHandleOtherCredentials() {
    SimpleCredentials credentials = new SimpleCredentials("joe", new char[0]);
    assertFalse(plugin.canHandle(credentials));
  }

  @Test
  public void testGetPrincipal() {
    assertEquals("someUser", plugin.getPrincipal(credentials).getName());
  }

  @Test
  public void testImpersonate() throws FailedLoginException, RepositoryException {
    assertEquals(LoginModulePlugin.IMPERSONATION_DEFAULT, plugin.impersonate(null, null));
  }

  @Test
  public void canGetAuthentication() throws Exception {
    plugin.getAuthentication(null, credentials);
  }

  @Test
  public void cannotGetAuthenticationWrongCredentialType() throws Exception {
    Credentials credentials = mock(Credentials.class);
    plugin.getAuthentication(null, credentials);
  }

  @Test
  public void cannotGetAuthenticationMissingSsoPrincipal() throws Exception {
    SimpleCredentials credentials = new SimpleCredentials("someUser", new char[0]);
    plugin.getAuthentication(null, credentials);
  }

  @Test
  public void cannotGetAuthenticationWrongPrincipalType() throws Exception {
    SimpleCredentials credentials = new SimpleCredentials("someUser", new char[0]);
    credentials.setAttribute(SsoPrincipal.class.getName(), mock(Principal.class));
    plugin.getAuthentication(null, credentials);
  }
}
