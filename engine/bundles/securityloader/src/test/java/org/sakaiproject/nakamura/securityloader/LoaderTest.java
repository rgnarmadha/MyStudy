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
package org.sakaiproject.nakamura.securityloader;

import org.apache.jackrabbit.api.security.principal.PrincipalManager;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;

import java.io.IOException;
import java.net.URL;
import java.security.Principal;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFactory;


/**
 *
 */
public class LoaderTest extends SecurityLoaderServiceTest {

  @Mock private Node aNode;
  @Mock private Node bNode;
  @Mock private Node cNode;
  @Mock private UserManager userManager;
  @Mock private PrincipalManager principalManager;
  @Mock private Group group;
  @Mock private ValueFactory valueFactory;
  @Mock private AuthorizablePostProcessService authorizablePostProcessService;;

  @Before
  public void before() throws RepositoryException {
    super.before();
  }

  @Test
  public void testLoader() throws JSONException, IOException, RepositoryException {
    if ( true ) {
      System.err.println("Test disabled as AccessControlUtil no longer works with a JackrabbitSession, needs full integation");
      return;
    }
    Mockito.when(userManager.createGroup((Principal) Mockito.anyObject())).thenReturn(group);
    Mockito.when(session.getUserManager()).thenReturn(userManager);
    Mockito.when(session.getPrincipalManager()).thenReturn(principalManager);
    Mockito.when(session.getValueFactory()).thenReturn(valueFactory);
    Mockito.when(session.itemExists("/a")).thenReturn(true);
    Mockito.when(session.itemExists("/b")).thenReturn(true);
    Mockito.when(session.itemExists("/c")).thenReturn(true);
    Mockito.when(session.getItem("/a")).thenReturn(aNode);
    Mockito.when(session.getItem("/b")).thenReturn(bNode);
    Mockito.when(session.getItem("/c")).thenReturn(cNode);
    Mockito.when(rootNode.addNode(Mockito.anyString())).thenReturn(aNode);
    Mockito.when(rootNode.getNode(Mockito.anyString())).thenReturn(aNode);
    Mockito.when(aNode.addNode(Mockito.anyString())).thenReturn(aNode);
    Mockito.when(aNode.getNode(Mockito.anyString())).thenReturn(aNode);
    Mockito.when(aNode.isNode()).thenReturn(true);

    URL u = this.getClass().getResource("testacl.json");

    Mockito.when(bundle1.getEntry("SLING-INF/acl/personal-acl.json")).thenReturn(u);
    Mockito.when(bundle1.getEntry("SLING-INF/acl2/personal-acl.json")).thenReturn(u);
    Mockito.when(bundle1.getEntry("SLING-INF/acl3/personal-acl.json")).thenReturn(u);
    Mockito.when(bundle2.getEntry("SLING-INF/acl/personal-acl.json")).thenReturn(u);
    Mockito.when(bundle2.getEntry("SLING-INF/acl2/personal-acl.json")).thenReturn(u);
    Mockito.when(bundle2.getEntry("SLING-INF/acl3/personal-acl.json")).thenReturn(u);

    Loader loader = new Loader(securityLoaderService, authorizablePostProcessService);
    loader.registerBundle(session, bundle1, false);
    loader.registerBundle(session, bundle1, false);
    Mockito.when(b1node.isLocked()).thenReturn(true);
    loader.registerBundle(session, bundle1, false);
    Mockito.when(b2node.isLocked()).thenReturn(true);
    loader.registerBundle(session, bundle2, false);
    Mockito.when(b1node.isLocked()).thenReturn(false);
    Mockito.when(b2node.isLocked()).thenReturn(false);
    loader.registerBundle(session, bundle1, false);
    loader.unregisterBundle(session, bundle2);
    loader.registerBundle(session, bundle2, true);
    loader.unregisterBundle(session, bundle1);
    loader.unregisterBundle(session, bundle2);
  }
}
