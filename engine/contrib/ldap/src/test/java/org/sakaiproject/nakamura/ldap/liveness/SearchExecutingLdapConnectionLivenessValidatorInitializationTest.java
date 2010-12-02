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
import static junit.framework.Assert.assertNull;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;

import org.junit.Before;
import org.junit.Test;
import org.sakaiproject.nakamura.api.configuration.ConfigurationService;
import org.sakaiproject.nakamura.ldap.liveness.SearchExecutingLdapConnectionLivenessValidator;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Tests for an unitialized {@link SearchExecutingLdapConnectionLivenessValidator}
 * fixture. That is, it tests the initialization logic itself. This is
 * as opposed to the fixture in {@link SearchExecutingLdapConnectionLivenessValidatorTest}
 * which initializes the fixture in
 * {@link SearchExecutingLdapConnectionLivenessValidatorTest#setUp()}.
 *
 * @author dmccallum
 *
 */
public class SearchExecutingLdapConnectionLivenessValidatorInitializationTest {

	private static final String UNIQUE_SEARCH_FILTER_TERM = "TESTING";
	private SearchExecutingLdapConnectionLivenessValidator validator;
  private ConfigurationService configService;

  @Before
  public void setUp() {
    validator = new SearchExecutingLdapConnectionLivenessValidator() {
      // we need this to be a predictable value
      @Override
      protected String generateUniqueToken() {
        return UNIQUE_SEARCH_FILTER_TERM;
      }
    };
    configService = createMock(ConfigurationService.class);
    validator.setConfigService(configService);
	}

  @Test
  public void testInitHonorsExplicitlyInjectedHostName() {
		final String EXPECTED_HOST_NAME = "EXPECTED_HOST_NAME";
		validator.setHostName(EXPECTED_HOST_NAME);
		validator.init();
		assertEquals(EXPECTED_HOST_NAME, validator.getHostName());
	}

  @Test
  public void testInitDefaultsHostNameToInetAddressLocalhostIfNoHostNameExplicitlyInjected()
	throws UnknownHostException {
		final String EXPECTED_HOST_NAME = InetAddress.getLocalHost().toString();
    validator.setConfigService(configService);
		validator.init();
		assertEquals(EXPECTED_HOST_NAME, validator.getHostName());
	}

  @Test
  public void testInitDefaultsHostNameToSakaiServerNameIfNoHostNameExplicitlyInjectedAndLocalHostLookupFails() {
		validator = new SearchExecutingLdapConnectionLivenessValidator() {
			// we need this to be a predictable value
			@Override
      protected String generateUniqueToken() {
				return UNIQUE_SEARCH_FILTER_TERM;
			}

			@Override
      protected String getLocalhostName() throws UnknownHostException {
				throw new UnknownHostException();
			}
		};
		final String EXPECTED_HOST_NAME = "EXPECTED_HOST_NAME";
    expect(configService.getProperty((String) anyObject())).andReturn(EXPECTED_HOST_NAME);
    replay(configService);
    validator.setConfigService(configService);
		validator.init();
		assertEquals(EXPECTED_HOST_NAME, validator.getHostName());
	}

  @Test
  public void testInitDefaultsHostNameToInetAddressLocalhostIfNoHostNameExplicitlyInjectedAndNoSakaiServerNameInjected()
	throws UnknownHostException {
    validator.setConfigService(null);
		assertNull(validator.getServerConfigService()); // sanity check
		validator.init();
		assertEquals(validator.getLocalhostName(),
				validator.getHostName());
	}

  @Test
  public void testDefaultsHostNameToConstantDefaultIfNeitherInitNorSetHostNameCalled() {
		assertEquals(SearchExecutingLdapConnectionLivenessValidator.DEFAULT_HOST_NAME,
				validator.getHostName());
	}

}
