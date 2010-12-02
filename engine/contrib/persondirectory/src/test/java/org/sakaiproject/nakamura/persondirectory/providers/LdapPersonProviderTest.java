/*
 * Licensed to the Sakai Foundation (SF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership. The SF licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sakaiproject.nakamura.persondirectory.providers;

import static org.mockito.Matchers.anyInt;

import static org.mockito.Matchers.any;

import static org.mockito.Matchers.anyBoolean;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.defaultanswers.ReturnsDeepStubs;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.component.ComponentException;
import org.sakaiproject.nakamura.api.ldap.LdapConnectionManager;
import org.sakaiproject.nakamura.api.persondirectory.PersonProviderException;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;

@RunWith(MockitoJUnitRunner.class)
public class LdapPersonProviderTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Node node;

  /**
   * Test for the default constructor. Too simple to not have and boosts code coverage.
   *
   * @throws Exception
   */
  @Test(expected = NullPointerException.class)
  public void testDefaultConstructor() throws Exception {
    LdapPersonProvider provider = new LdapPersonProvider();
    provider.getProfileSection(hasAllProperties(node, "admin"));
    fail("Should fail when the connection manager isn't explicitly set or injected by OSGi reference.");
  }

  @Test
  public void testActivateWithAllProperties() throws Exception {
    LdapConnectionManager mgr = mock(LdapConnectionManager.class);

    LdapPersonProvider provider = new LdapPersonProvider(mgr);
    String[] attrMap = new String[] { "attr0=>wow wee", "attr1 => camera 1", "attr2" };
    provider.activate(buildMap(attrMap));
    Map<String, String> attributesMap = provider.getAttributesMap();
    assertTrue(attributesMap.containsKey("attr0"));
    assertEquals("wow wee", attributesMap.get("attr0"));
    assertTrue(attributesMap.containsKey("attr1"));
    assertEquals("camera 1", attributesMap.get("attr1"));
    assertTrue(attributesMap.containsKey("attr2"));
    assertEquals("attr2", attributesMap.get("attr2"));
  }

  @Test(expected = ComponentException.class)
  public void testActivateWithEmptySecondKey() {
    LdapPersonProvider provider = new LdapPersonProvider();
    String[] attrMap = new String[] { "attr0=> " };
    provider.activate(buildMap(attrMap));
    fail("Should fail on improper mapping syntax.");
  }

  /**
   * Test getting a person from an ldap provider.
   *
   * @throws Exception
   */
  @Test
  public void testGetProfileSection() throws Exception {
    String[] attrMap = new String[] { "firstname => called", "lastname" };
    LdapPersonProvider provider = setUpForPositiveTest(attrMap);
    Map<String, Object> person = provider.getProfileSection(hasAllProperties(node,
        "tUser"));
    assertNotNull(person);

    Set<String> keys = person.keySet();
    assertEquals(2, keys.size());

    assertTrue(keys.contains("called"));
    assertEquals("Tester", person.get("called"));

    assertTrue(keys.contains("lastname"));
    assertTrue(person.get("lastname") instanceof String[]);

    boolean hasUser = false;
    boolean hasLuser = false;
    for (String val : (String[]) person.get("lastname")) {
      if ("User".equals(val)) {
        hasUser = true;
      } else if ("Luser".equals(val)) {
        hasLuser = true;
      }
    }
    assertTrue(hasUser && hasLuser);
  }

  @Test
  public void testProfileSectionNotFound() throws Exception {
    String[] attrMap = new String[] { "firstname => called", "lastname" };

    LDAPConnection connection = mock(LDAPConnection.class);
    LDAPSearchResults results = mock(LDAPSearchResults.class);

    LdapConnectionManager mgr = mock(LdapConnectionManager.class);
    when(mgr.getBoundConnection(anyString(), anyString())).thenReturn(connection);
    when(
        connection.search(anyString(), anyInt(), anyString(), any(String[].class),
            anyBoolean())).thenReturn(results);

    LdapPersonProvider provider = new LdapPersonProvider(mgr);
    provider.activate(buildMap(attrMap));
    Map<String, Object> person = provider.getProfileSection(hasAllProperties(node,
        "tUser"));

    assertNotNull(person);

    assertEquals(0, person.size());
  }

  @Test
  public void testRecurseForInfo() throws Exception {
    String[] attrMap = new String[] { "firstname => called", "lastname" };
    LdapPersonProvider provider = setUpForPositiveTest(attrMap);
    Node n2 = Mockito.mock(Node.class, new ReturnsDeepStubs());
    hasAllProperties(n2, "huh");
    when(node.getParent()).thenReturn(n2);
    when(node.getPath()).thenReturn("/path/to/node");
    Map<String, Object> person = provider.getProfileSection(node);
    assertNotNull(person);

    Set<String> keys = person.keySet();
    assertEquals(2, keys.size());

    assertTrue(keys.contains("called"));
    assertEquals("Tester", person.get("called"));

    assertTrue(keys.contains("lastname"));
    assertTrue(person.get("lastname") instanceof String[]);

    boolean hasUser = false;
    boolean hasLuser = false;
    for (String val : (String[]) person.get("lastname")) {
      if ("User".equals(val)) {
        hasUser = true;
      } else if ("Luser".equals(val)) {
        hasLuser = true;
      }
    }
    assertTrue(hasUser && hasLuser);
  }

  @Test(expected = PersonProviderException.class)
  public void testAtRootNoInfo() throws Exception {
    String[] attrMap = new String[] { "firstname => called", "lastname" };
    LdapPersonProvider provider = setUpForPositiveTest(attrMap);
    when(node.getParent()).thenThrow(new RepositoryException());
    provider.getProfileSection(node);
    fail("Should bubble up internal exceptions.");
  }

  /**
   * Test getPerson() when LdapConnectionBroker.getBoundConnection(..) throws an
   * LdapException.
   *
   * @throws Exception
   */
  @Test(expected = PersonProviderException.class)
  public void testGetPersonThrowsLdapException() throws Exception {
    LdapConnectionManager mgr = mock(LdapConnectionManager.class);
    when(mgr.getBoundConnection(anyString(), anyString())).thenThrow(new LDAPException());

    LdapPersonProvider provider = new LdapPersonProvider(mgr);
    provider.activate(buildMap(null));
    provider.getProfileSection(hasAllProperties(node, "tUser"));
    fail("Should bubble up exceptions that are thrown internally.");
  }

  /**
   * Test getPerson() when LDAPConnection.search(..) throws an LDAPException.
   *
   * @throws Exception
   */
  @Test(expected = PersonProviderException.class)
  public void testGetPersonThrowsLDAPException() throws Exception {
    LdapConnectionManager mgr = mock(LdapConnectionManager.class);
    LDAPConnection connection = mock(LDAPConnection.class);
    when(mgr.getBoundConnection(anyString(), anyString())).thenReturn(connection);
    // replay(mgr);
    when(
        connection.search(Mockito.isA(String.class), anyInt(), isA(String.class),
            any(String[].class), anyBoolean())).thenThrow(new LDAPException());
    // EasyMock.replay(connection);

    LdapPersonProvider provider = new LdapPersonProvider(mgr);
    provider.activate(buildMap(null));
    provider.getProfileSection(hasAllProperties(node, "tUser"));
    fail("Should bubble up exceptions that are thrown internally.");
  }

  /**
   * Setup everything needed for a test that follows the most positive path of action.
   *
   * @return
   * @throws Exception
   */
  private LdapPersonProvider setUpForPositiveTest(String[] attributeMap) throws Exception {
    LDAPConnection connection = mock(LDAPConnection.class);
    LDAPSearchResults results = mock(LDAPSearchResults.class);
    LDAPEntry entry = mock(LDAPEntry.class);

    LdapConnectionManager mgr = mock(LdapConnectionManager.class);
    when(mgr.getBoundConnection(anyString(), anyString())).thenReturn(connection);
    when(
        connection.search(anyString(), anyInt(), anyString(), any(String[].class),
            anyBoolean())).thenReturn(results);

    // get a result
    when(results.hasMore()).thenReturn(true).thenReturn(false);
    when(results.next()).thenReturn(entry);

    // get the attributes and an iterator to them
    LDAPAttributeSet attrSet = mock(LDAPAttributeSet.class);
    when(entry.getAttributeSet()).thenReturn(attrSet);
    @SuppressWarnings("unchecked")
    Iterator<LDAPAttribute> attrIter = mock(Iterator.class);
    when(attrSet.iterator()).thenReturn(attrIter);

    when(attrIter.hasNext()).thenReturn(true).thenReturn(true).thenReturn(false);
    // first loop through
    LDAPAttribute attr1 = mock(LDAPAttribute.class);
    when(attr1.getName()).thenReturn("firstname");
    when(attr1.getStringValueArray()).thenReturn(new String[] { "Tester" });
    // second loop through
    LDAPAttribute attr2 = mock(LDAPAttribute.class);
    when(attr2.getName()).thenReturn("lastname");
    when(attr2.getStringValueArray()).thenReturn(new String[] { "User", "Luser" });
    when(attrIter.next()).thenReturn(attr1).thenReturn(attr2);

    LdapPersonProvider provider = new LdapPersonProvider(mgr);
    provider.activate(buildMap(attributeMap));
    return provider;
  }

  private Map<String, Object> buildMap(String[] attributeMap) {
    HashMap<String, Object> map = new HashMap<String, Object>();
    map.put(LdapPersonProvider.BASE_DN, "ou=accounts,dc=sakai");
    map.put(LdapPersonProvider.PROP_FILTER_PATTERN, "uid={}");
    if (attributeMap != null) {
      map.put(LdapPersonProvider.PROP_ATTRIBUTES_MAP, attributeMap);
    }
    return map;
  }

  private Node hasAllProperties(Node node, String uid) throws Exception {
    when(node.hasProperty(LdapPersonProvider.SLING_RESOURCE_TYPE)).thenReturn(true);
    when(node.getProperty(LdapPersonProvider.SLING_RESOURCE_TYPE).getString()).thenReturn(
        LdapPersonProvider.SAKAI_USER_PROFILE);
    when(node.hasProperty(LdapPersonProvider.REP_USER_ID)).thenReturn(true);

    Property prop = mock(Property.class);
    when(prop.getString()).thenReturn(uid);
    when(node.getProperty(LdapPersonProvider.REP_USER_ID)).thenReturn(prop);

    return node;
  }
}
