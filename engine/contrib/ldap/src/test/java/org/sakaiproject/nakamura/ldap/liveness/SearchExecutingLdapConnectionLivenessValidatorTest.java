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
package org.sakaiproject.nakamura.ldap.liveness;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;
import static org.junit.Assert.assertTrue;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchConstraints;
import com.novell.ldap.LDAPSearchResults;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.ldap.PooledLDAPConnection;
import org.sakaiproject.nakamura.ldap.liveness.SearchExecutingLdapConnectionLivenessValidator;

import java.text.MessageFormat;

public class SearchExecutingLdapConnectionLivenessValidatorTest {
  private static final String UNIQUE_SEARCH_FILTER_TERM = "TESTING";
  private SearchExecutingLdapConnectionLivenessValidator validator;
  private LDAPConnection conn;
  private LDAPSearchResults searchResults;
  private LDAPEntry ldapEntry;

  @Before
  public void setUp() {
    validator = new SearchExecutingLdapConnectionLivenessValidator() {
      // we need this to be a predictable value
      @Override
      protected String generateUniqueToken() {
        return UNIQUE_SEARCH_FILTER_TERM;
      }
    };
    validator.init();
    conn = createMock(PooledLDAPConnection.class);
    searchResults = createMock(LDAPSearchResults.class);
    ldapEntry = createMock(LDAPEntry.class);
  }

  @Test
  public void testIssuesLivenessSearch() throws Exception {
    expectStandardSearch();
    expect(searchResults.hasMore()).andReturn(true);
    expect(searchResults.next()).andReturn(ldapEntry);
    replay(searchResults, conn);
    assertTrue(validator.isConnectionAlive(conn));
  }

  @Test
  public void testUniqueSearchFilterTermIncludesHostName() {
    final String EXPECTED_TERM = UNIQUE_SEARCH_FILTER_TERM + "-" + validator.getHostName();
    assertEquals(EXPECTED_TERM, validator.generateUniqueSearchFilterTerm());
  }

  /**
   * Identical to {@link #testIssuesLivenessSearch()}, but expects search to
   * return no results.
   */
  @Test
  public void testLivenessTestConvertsEmptySearchResultsToFalseReturnValue() throws Exception {
    expectStandardSearch();
    expect(searchResults.hasMore()).andReturn(false);
    replay(searchResults, conn);
    assertFalse(validator.isConnectionAlive(conn));
  }

  /**
   * Not entirely sure that this could actually happen in the wild.
   */
  @Test
  public void testLivenessTestConvertsNullLDAPEntryToFalseReturnValue() throws Exception {
    expectStandardSearch();
    expect(searchResults.hasMore()).andReturn(true);
    expect(searchResults.next()).andReturn(null);
    replay(searchResults, conn);
    assertFalse(validator.isConnectionAlive(conn));
  }

  /**
   * Same as
   * {@link #testIssuesLivenessSearchConvertsEmptySearchResultsToFalseReturnValue()}
   * , but verifies that exceptional searches are handled properly.
   */
  @Test
  public void testTransformsSearchExceptionToFalseReturnValue() throws Exception {
    expectStandardSearchToThrowLDAPException();
    replay(conn);
    assertFalse(validator.isConnectionAlive(conn));
  }

  @Test
  public void testTransformsSearchResultsIterationExceptionToFalseReturnValue() throws Exception {
    expectStandardSearch();
    expect(searchResults.hasMore()).andReturn(true);
    expect(searchResults.next()).andThrow(new LDAPException());
    replay(searchResults, conn);
    assertFalse(validator.isConnectionAlive(conn));
  }

  @Test
  public void testInterpolatesUniqueTermInFormattedSearchFilter() {
    String rawSearchFilter = validator.getSearchFilter();

    // relies on setUp() having overriden generateUniqueSearchFilterTerm()
    // to return a predictable value
    String expectedFormattedSearchFilter = MessageFormat.format(rawSearchFilter, validator
        .generateUniqueSearchFilterTerm());

    assertEquals(expectedFormattedSearchFilter, validator.formatSearchFilter());
  }

  private void expectStandardSearch() throws Exception {
    final String BASE_DN = "some-dn";
    // final LDAPSearchConstraints expectedConstraints =
    // validator.getSearchConstraints();
    // String expectedFilterString = validator.formatSearchFilter();
    validator.setBaseDn(BASE_DN);
    // expect(
    // conn.search(BASE_DN, LDAPConnection.SCOPE_BASE, expectedFilterString,
    // new String[] { validator.getSearchAttributeName() }, false,
    // expectedConstraints))
    // .andReturn(searchResults);
    expect(
        conn.search((String) anyObject(), anyInt(), (String) anyObject(), (String[]) anyObject(),
            anyBoolean(), (LDAPSearchConstraints) anyObject())).andReturn(searchResults);
  }

  private void expectStandardSearchToThrowLDAPException() throws Exception {
    final String BASE_DN = "some-dn";
    // final LDAPSearchConstraints expectedConstraints =
    // validator.getSearchConstraints();
    // String expectedFilterString = validator.formatSearchFilter();
    validator.setBaseDn(BASE_DN);
    // expect(
    // conn.search(BASE_DN, LDAPConnection.SCOPE_BASE, expectedFilterString,
    // new String[] { validator.getSearchAttributeName() }, false,
    // expectedConstraints))
    // .andThrow(new LDAPException());
    expect(
        conn.search((String) anyObject(), anyInt(), (String) anyObject(), (String[]) anyObject(),
            anyBoolean(), (LDAPSearchConstraints) anyObject())).andThrow(new LDAPException());
  }

}
