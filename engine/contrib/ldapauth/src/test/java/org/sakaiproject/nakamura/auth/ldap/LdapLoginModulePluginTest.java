/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sakaiproject.nakamura.auth.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.apache.sling.jcr.jackrabbit.server.security.LoginModulePlugin;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.security.auth.callback.CallbackHandler;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapLoginModulePluginTest {

  private LdapLoginModulePlugin loginPlugin;

  @Mock
  private CallbackHandler callbackHandler;

  @Mock
  private Session jcrSession;

  @Mock
  private Map<?, ?> options;

  @Mock
  private LdapAuthenticationPlugin authPlugin;

  @Before
  public void setUp() {
    loginPlugin = new LdapLoginModulePlugin(authPlugin);
    loginPlugin.activate(new HashMap<String, String>());
  }

  @Test
  public void defaultConstructor() {
    new LdapLoginModulePlugin();
  }

  @Test
  public void initDoesNothing() throws Exception {
    // when
    loginPlugin.doInit(callbackHandler, jcrSession, options);

    // then
    verifyZeroInteractions(callbackHandler, jcrSession, options);
  }

  @Test
  public void addPrincipalsDoesNothing() {
    Set<?> set = Mockito.mock(Set.class);

    loginPlugin.addPrincipals(set);

    verifyZeroInteractions(set);
  }

  @Test
  public void getAuthPluginAsProvided() throws Exception {
    assertEquals(authPlugin, loginPlugin.getAuthentication(null, null));

    verifyZeroInteractions(authPlugin);
  }

  @Test
  public void canHandleSimpleCredentials() {
    assertTrue(loginPlugin.canHandle(simpleCredentials()));
  }

  @Test
  public void cannotHandleOtherThanSimpleCredentials() {
    Credentials credentials = mock(Credentials.class);
    assertFalse(loginPlugin.canHandle(credentials));
  }

  @Test
  public void cannotHandleEmptyUser() {
    SimpleCredentials credentials = new SimpleCredentials(null, new char[] { 'a' });
    assertFalse(loginPlugin.canHandle(credentials));

    credentials = new SimpleCredentials("", new char[] { 'a' });
    assertFalse(loginPlugin.canHandle(credentials));
  }

  @Test
  public void cannotHandleEmptyPassword() {
    SimpleCredentials credentials = new SimpleCredentials("admin", new char[] {});
    assertFalse(loginPlugin.canHandle(credentials));
  }

  @Test
  public void defaultUserFilteredOut() {
    SimpleCredentials credentials = new SimpleCredentials("admin", new char[] { 'a' });
    assertFalse(loginPlugin.canHandle(credentials));
  }

  @Test
  public void customUserFilteredOut() {
    HashMap<String, String> props = new HashMap<String, String>();
    props.put(LdapLoginModulePlugin.USER_FILTER, "(^admin$|tstalex.+)");
    loginPlugin.modified(props);
  }

  @Test
  public void impersonateAsDefault() throws Exception {
    assertEquals(LoginModulePlugin.IMPERSONATION_DEFAULT, loginPlugin.impersonate(null,
        null));
  }

  @Test
  public void returnsPrincipalWithSameNameAsCredentials() throws Exception {
    // given
    String name = "joe";
    char[] password = {'f','o','o'};
    Credentials credentials = new SimpleCredentials(name, password);
    // when
    Principal principal = loginPlugin.getPrincipal(credentials);
    // then
    assertEquals(principal.getName(), name);
  }

  /**
   * Instantiates SimpleCredentials.
   *
   * @return
   */
  private SimpleCredentials simpleCredentials() {
    String name = "joe";
    char[] password = {'p', 'a', 's', 's'};
    return new SimpleCredentials(name, password);
  }
}
