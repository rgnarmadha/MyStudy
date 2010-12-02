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
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.principal.PrincipalIterator;
import org.apache.jackrabbit.core.security.principal.PrincipalIteratorAdapter;
import org.apache.jackrabbit.core.security.principal.PrincipalProvider;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.RulesBasedAce;

import java.security.Principal;
import java.util.Properties;

import javax.jcr.Session;

/**
 *
 */
@Component(immediate=true,description="Provides Principal resolution")
@Service(value=PrincipalProvider.class)
public class RulesPrincipalProvider implements PrincipalProvider {


  /**
   * {@inheritDoc}
   * @see org.apache.jackrabbit.core.security.principal.PrincipalProvider#canReadPrincipal(javax.jcr.Session, java.security.Principal)
   */
  public boolean canReadPrincipal(Session session, Principal principalToRead) {
    return (principalToRead instanceof RulesPrincipal);
  }

  /**
   * {@inheritDoc}
   * @see org.apache.jackrabbit.core.security.principal.PrincipalProvider#close()
   */
  public void close() {
  }

  /**
   * {@inheritDoc}
   * @see org.apache.jackrabbit.core.security.principal.PrincipalProvider#findPrincipals(java.lang.String)
   */
  public PrincipalIterator findPrincipals(String simpleFilter) {
    return PrincipalIteratorAdapter.EMPTY;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.jackrabbit.core.security.principal.PrincipalProvider#findPrincipals(java.lang.String, int)
   */
  public PrincipalIterator findPrincipals(String simpleFilter, int searchType) {
    return PrincipalIteratorAdapter.EMPTY;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.jackrabbit.core.security.principal.PrincipalProvider#getGroupMembership(java.security.Principal)
   */
  public PrincipalIterator getGroupMembership(Principal principal) {
    return PrincipalIteratorAdapter.EMPTY;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.jackrabbit.core.security.principal.PrincipalProvider#getPrincipal(java.lang.String)
   */
  public Principal getPrincipal(String principalName) {
    if ( principalName.startsWith(RulesBasedAce.SAKAI_RULES)) {
      return new RulesPrincipal(principalName);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.jackrabbit.core.security.principal.PrincipalProvider#getPrincipals(int)
   */
  public PrincipalIterator getPrincipals(int searchType) {
    return PrincipalIteratorAdapter.EMPTY;
  }

  /**
   * {@inheritDoc}
   * @see org.apache.jackrabbit.core.security.principal.PrincipalProvider#init(java.util.Properties)
   */
  public void init(Properties options) {
  }

}
