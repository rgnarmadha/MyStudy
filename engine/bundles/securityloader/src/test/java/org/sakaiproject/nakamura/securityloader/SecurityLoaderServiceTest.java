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

import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.sling.engine.SlingSettingsService;
import org.apache.sling.jcr.api.SlingRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.EventAdmin;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.jcr.Node;
import javax.jcr.RepositoryException;


/**
 *
 */
public class SecurityLoaderServiceTest {
  private static final String TEST_SECURITY_HEADER = "SLING-INF/acl/personal-acl.json;path:=/a;overwrite:=false;uninstall:=false;,"
    + "SLING-INF/acl2/personal-acl.json;overwrite:=true;uninstall:=false;path:=/b,"
    + "SLING-INF/acl3/personal-acl.json;overwrite:=false;uninstall:=true;path:=/c";
  @Mock private ComponentContext componentContext;
  @Mock private SlingSettingsService settingsService;
  @Mock private SlingRepository repository;
  protected SecurityLoaderService securityLoaderService;
  @Mock private EventAdmin eventAdmin;
  @Mock private BundleEvent event;
  @Mock private BundleContext bundleContext;
  @Mock
  protected JackrabbitSession session;
  @Mock
  protected Node rootNode;
  @Mock private Node varNode;
  @Mock private Node slingNode;
  @Mock private Node bundleSecurityNode;
  @Mock
  protected Bundle bundle1;
  @Mock
  protected Bundle bundle2;
  @Mock protected Node b1node;
  @Mock protected Node b2node;

  @Before
  public void before() throws RepositoryException {
    MockitoAnnotations.initMocks(this);
    Mockito.when(componentContext.getBundleContext()).thenReturn(bundleContext);
    Mockito.when(repository.loginAdministrative(null)).thenReturn(session);
    Mockito.when(session.getRootNode()).thenReturn(rootNode);
    Mockito.when(rootNode.hasNode("var")).thenReturn(true);
    Mockito.when(rootNode.getNode("var")).thenReturn(varNode);
    Mockito.when(varNode.hasNode("sling")).thenReturn(true);
    Mockito.when(varNode.getNode("sling")).thenReturn(slingNode);
    Mockito.when(slingNode.hasNode("bundle-security")).thenReturn(true);
    Mockito.when(slingNode.getNode("bundle-security")).thenReturn(bundleSecurityNode);
    Dictionary<String, Object> properties = new Hashtable<String, Object>();
    Mockito.when(componentContext.getProperties()).thenReturn(properties);
    
    
    Bundle[] bundles = new Bundle[] {
        bundle1,
        bundle2
    };
    Mockito.when(bundleContext.getBundles()).thenReturn(bundles);
    Dictionary<String, String> headers = new Hashtable<String, String>();
    headers.put(PathEntry.SECURITY_HEADER, TEST_SECURITY_HEADER);
    Mockito.when(bundle1.getHeaders()).thenReturn(headers);
    Mockito.when(bundle1.getSymbolicName()).thenReturn("bundle1");
    Mockito.when(bundle2.getHeaders()).thenReturn(headers);
    Mockito.when(bundle2.getSymbolicName()).thenReturn("bundle2");
    
    Mockito.when(session.getItem(SecurityLoaderService.BUNDLE_SECURITY_NODE)).thenReturn(bundleSecurityNode);
    Mockito.when(bundleSecurityNode.hasNode("bundle1")).thenReturn(false);
    Mockito.when(bundleSecurityNode.hasNode("bundle2")).thenReturn(false);
    Mockito.when(bundleSecurityNode.addNode("bundle1", "nt:unstructured")).thenReturn(b1node);
    Mockito.when(bundleSecurityNode.addNode("bundle2", "nt:unstructured")).thenReturn(b2node);
    Mockito.when(bundleSecurityNode.getNode("bundle1")).thenReturn(b1node);
    Mockito.when(bundleSecurityNode.getNode("bundle2")).thenReturn(b2node);
    Mockito.when(event.getBundle()).thenReturn(bundle1);
    
    securityLoaderService = new SecurityLoaderService();
    securityLoaderService.settingsService = settingsService;
    securityLoaderService.repository = repository;
    securityLoaderService.bindEventAdmin(eventAdmin);
    securityLoaderService.activate(componentContext);
    
    Mockito.verify(rootNode).hasNode("var");
    Mockito.verify(rootNode).getNode("var");
    Mockito.verify(varNode).hasNode("sling");
    Mockito.verify(varNode).getNode("sling");
    Mockito.verify(slingNode).hasNode("bundle-security");    
  }
  
  @After
  public void after() {
    securityLoaderService.unbindEventAdmin(eventAdmin);
    securityLoaderService.deactivate(componentContext);
    
  }
  
  @Test
  public void test() throws RepositoryException {
    
    Mockito.when(event.getType()).thenReturn(BundleEvent.STARTING);
    securityLoaderService.bundleChanged(event);
    Mockito.when(event.getType()).thenReturn(BundleEvent.UPDATED);
    securityLoaderService.bundleChanged(event);
    Mockito.when(event.getType()).thenReturn(BundleEvent.UNINSTALLED);
    securityLoaderService.bundleChanged(event);
    Mockito.when(bundleSecurityNode.hasNode("bundle1")).thenReturn(true);
    securityLoaderService.contentIsUninstalled(session, bundle1);
    
    securityLoaderService.digestPassword("testPassword");
    
  }

}
