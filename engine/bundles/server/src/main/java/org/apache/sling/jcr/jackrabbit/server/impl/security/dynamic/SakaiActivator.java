/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sling.jcr.jackrabbit.server.impl.Activator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * The <code>Activator</code> 
 */
public class SakaiActivator extends Activator {
  
  
  private static final Logger LOGGER = LoggerFactory.getLogger(SakaiActivator.class);
  private static DynamicPrincipalManagerFactoryImpl dynamicPrincipalManagerFactory;
  private static RuleProcessorManagerImpl ruleProcessorManager;
  private static PrincipalProviderRegistryManagerImpl principalProviderRegistryManager;

  /**
   * {@inheritDoc}
   * @see org.apache.sling.jcr.jackrabbit.server.impl.Activator#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext bundleContext) {
    
    try {
      File homeDir = getHomeDir(bundleContext);
      File indexingConfig = new File(homeDir, "indexing_configuration.xml");
      if (!indexingConfig.exists()) {
        InputStream in = this.getClass().getClassLoader()
            .getResourceAsStream("indexing_configuration.xml");
        FileOutputStream out = new FileOutputStream(indexingConfig);
        IOUtils.copy(in, out);
        in.close();
        out.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(
          "Failed to initialize indexing configuration, repository will not start", e);
    }
    
    super.start(bundleContext);
    
    if (dynamicPrincipalManagerFactory == null) {
      dynamicPrincipalManagerFactory = new DynamicPrincipalManagerFactoryImpl(
          bundleContext);
    }
    dynamicPrincipalManagerFactory.open();

    if (ruleProcessorManager == null) {
      ruleProcessorManager = new RuleProcessorManagerImpl(
          bundleContext);
    }
    ruleProcessorManager.open();

    if (principalProviderRegistryManager == null) {
      principalProviderRegistryManager = new PrincipalProviderRegistryManagerImpl(
          bundleContext);
    }
    principalProviderRegistryManager.open();

  }
  
  
  // lifted from super, becuase it was not exposed.
  private File getHomeDir(BundleContext bundleContext) throws IOException {
    File homeDir;

    String repoHomePath = bundleContext.getProperty("sling.repository.home");
    String slingHomePath = bundleContext.getProperty("sling.home");

    String repoName = bundleContext.getProperty("sling.repository.name");
    if (repoName == null) {
       repoName = "jackrabbit";
    }
    if (repoHomePath != null) {
      homeDir = new File(repoHomePath, repoName);
    } else if (slingHomePath != null) {
      homeDir = new File(slingHomePath, repoName);
    } else {
      homeDir = new File(repoName);
    }

    // make sure jackrabbit home exists
      LOGGER.info("Creating default config for Jackrabbit in " + homeDir);
      if (!homeDir.isDirectory()) {
          if (!homeDir.mkdirs()) {
            LOGGER.info("verifyConfiguration: Cannot create Jackrabbit home "
                  + homeDir + ", failed creating default configuration");
              return null;
          }
      }

    return homeDir;
  }
  
  
  /**
   * {@inheritDoc}
   * @see org.apache.sling.jcr.jackrabbit.server.impl.Activator#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext arg0) {
    if (dynamicPrincipalManagerFactory != null) {
      dynamicPrincipalManagerFactory.close();
      dynamicPrincipalManagerFactory = null;
    }
    if (ruleProcessorManager != null) {
      ruleProcessorManager.close();
      ruleProcessorManager = null;
    }
    if (principalProviderRegistryManager != null) {
      principalProviderRegistryManager.close();
      principalProviderRegistryManager = null;
    }
    super.stop(arg0);
  }
  /**
   * @return
   */
  public static DynamicPrincipalManagerFactory getDynamicPrincipalManagerFactory() {
    return dynamicPrincipalManagerFactory;
  }


  /**
   * @return
   */
  public static RuleProcessorManager getRuleProcessorManager() {
    return ruleProcessorManager;
  }
  /**
   * @return
   */
  public static PrincipalProviderRegistryManager getPrincipalProviderRegistryManager() {
    return principalProviderRegistryManager;
  }

  protected static void setDynamicPrincipalManagerFactory(DynamicPrincipalManagerFactoryImpl dynamicPrincipalManagerFactory) {
      SakaiActivator.dynamicPrincipalManagerFactory = dynamicPrincipalManagerFactory;
  }

  protected static void setRuleProcessorManager(RuleProcessorManagerImpl ruleProcessorManagerImpl) {
    SakaiActivator.ruleProcessorManager = ruleProcessorManagerImpl;
  }
  protected static void setPrincipalProviderManager(PrincipalProviderRegistryManagerImpl principalProviderRegistryManagerImpl) {
    SakaiActivator.principalProviderRegistryManager = principalProviderRegistryManagerImpl;
  }
}
