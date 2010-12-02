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
package org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.core.security.authorization.acl.RulesPrincipalProvider;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

/**
 *
 */
public class RepositoryBase {
  private RepositoryImpl repository;
  private SakaiActivator sakaiActivator;
  private BundleContext bundleContext;

  /**
   *
   */
  public RepositoryBase(BundleContext bundleContext) {
    this.bundleContext = bundleContext;
  }


  public void start() throws IOException, RepositoryException {
    File home = new File("target/testrepo");
    if ( home.exists() ) {
      FileUtils.deleteDirectory(home);
    }
    InputStream ins = this.getClass().getClassLoader().getResourceAsStream("test-repository.xml");

    setupSakaiActivator();
    RepositoryConfig crc = RepositoryConfig.create(ins, home.getAbsolutePath());
    repository = RepositoryImpl.create(crc);
    Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
    session.getWorkspace().getNamespaceRegistry().registerNamespace("sakai", "http://www.sakaiproject.org/nakamura/2.0");
    session.getWorkspace().getNamespaceRegistry().registerNamespace("sling", "http://sling.apache.org/testing");
    if ( session.hasPendingChanges() ) {
      session.save();
    }
    session.logout();
   }

  /**
   *
   */
  private void setupSakaiActivator() {
    System.err.println("Bundle is "+bundleContext);
    DynamicPrincipalManagerFactoryImpl dynamicPrincipalManagerFactoryImpl = new DynamicPrincipalManagerFactoryImpl(bundleContext);
    RuleProcessorManagerImpl ruleProcessorManagerImpl = new RuleProcessorManagerImpl(bundleContext);
    PrincipalProviderRegistryManagerImpl principalProviderRegistryManagerImpl = new PrincipalProviderRegistryManagerImpl(bundleContext);
    principalProviderRegistryManagerImpl.addProvider(new RulesPrincipalProvider());
    SakaiActivator.setDynamicPrincipalManagerFactory(dynamicPrincipalManagerFactoryImpl);
    SakaiActivator.setRuleProcessorManager(ruleProcessorManagerImpl);
    SakaiActivator.setPrincipalProviderManager(principalProviderRegistryManagerImpl);
    sakaiActivator = new SakaiActivator();
    sakaiActivator.start(bundleContext);
  }

  public void stop() {
    repository.shutdown();
    sakaiActivator.stop(bundleContext);
  }

  /**
   * @return the repository
   */
  public RepositoryImpl getRepository() {
    return repository;
  }

}
