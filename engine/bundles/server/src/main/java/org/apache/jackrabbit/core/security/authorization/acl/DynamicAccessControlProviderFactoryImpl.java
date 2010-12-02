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
package org.apache.jackrabbit.core.security.authorization.acl;

import org.apache.jackrabbit.core.RepositoryImpl; // Nakamura Change
import org.apache.jackrabbit.core.SessionImpl; // Nakamura Change
import org.apache.jackrabbit.core.config.BeanConfig;
import org.apache.jackrabbit.core.config.WorkspaceSecurityConfig;
// Start Nakamura Change
// import org.apache.jackrabbit.core.security.authorization.acl.ACLProvider;
// import org.apache.jackrabbit.core.security.user.UserAccessControlProvider;
// import org.apache.jackrabbit.core.SessionImpl;
// import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.AccessControlProviderFactory;
import org.apache.jackrabbit.core.security.user.DelegatedUserAccessControlProvider;
import org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic.RuleProcessorManager;
import org.apache.sling.jcr.jackrabbit.server.impl.security.dynamic.SakaiActivator;
import org.apache.sling.jcr.jackrabbit.server.security.dynamic.DynamicPrincipalManager;
// End Nakamura Change
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * NB: This class is based closely on org.apache.jackrabbit.core.security.authorization.AccessControlProviderFactory
 * DO NOT REFORMAT, and please mark any edits with Nakamura Change. 
 * Default implementation of the AccessControlProviderFactory a copy of the Jackrabbit
 * core version, since the respoitory config does not allow configuration of more than one
 * workspace with different AccessControlProviders.
 */
public class DynamicAccessControlProviderFactoryImpl implements AccessControlProviderFactory {

    /**
     * the default logger
     */
    private static final Logger log = LoggerFactory.getLogger(DynamicAccessControlProviderFactoryImpl.class);

    /**
     * The name of the security workspace (containing users...)
     */
    private String secWorkspaceName;
    private String defaultWorkspaceName;

    private DynamicPrincipalManager dynamicPrincipalManager; // Nakamura Change

    private RuleProcessorManager ruleProcessorManager; // Nakamura Change
    
    // ---------------------------------------< AccessControlProviderFactory >---
    /**
     * @see AccessControlProviderFactory#init(Session)
     */
    public void init(Session securitySession) throws RepositoryException {
        secWorkspaceName = securitySession.getWorkspace().getName();
        if (securitySession instanceof SessionImpl) {
            defaultWorkspaceName = ((RepositoryImpl) securitySession.getRepository()).getConfig().getDefaultWorkspaceName();
        } // else: unable to determine default workspace name
        // Start Nakamura Change
        dynamicPrincipalManager = SakaiActivator.getDynamicPrincipalManagerFactory().getDynamicPrincipalManager();
        ruleProcessorManager = SakaiActivator.getRuleProcessorManager();
        // End Nakamura Change
    }

    /**
     * @see AccessControlProviderFactory#close()
     */
    public void close() throws RepositoryException {
        // nothing to do
    }

    /**
     * @see AccessControlProviderFactory#createProvider(Session, WorkspaceSecurityConfig)
     */
    public AccessControlProvider createProvider(Session systemSession, WorkspaceSecurityConfig config)
            throws RepositoryException {
        String workspaceName = systemSession.getWorkspace().getName();
        AccessControlProvider prov;
        Map props;
        if (config != null && config.getAccessControlProviderConfig() != null) {
            BeanConfig bc = config.getAccessControlProviderConfig();
            prov = bc.newInstance(AccessControlProvider.class);
            props = bc.getParameters();
        } else {
            log.debug("No ac-provider configuration for workspace " + workspaceName + " -> using defaults.");
            if (workspaceName.equals(secWorkspaceName) && !workspaceName.equals(defaultWorkspaceName)) {
                // UserAccessControlProvider is designed to work with an extra
                // workspace storing user and groups. therefore avoid returning
                // this ac provider for the default workspace.
                prov = new DelegatedUserAccessControlProvider(); // Nakamura Change
            } else {
                prov = new DynamicACLProvider(dynamicPrincipalManager, ruleProcessorManager); // Nakamura Change
            }
            log.debug("Default provider for workspace " + workspaceName + " = " + prov.getClass().getName());
            props = Collections.emptyMap();
        }

        prov.init(systemSession, props);
        return prov;
    }
}
