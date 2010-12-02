/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.sakaiproject.nakamura.version.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.sakaiproject.nakamura.version.VersionService;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionManager;

/**
 * Service for doing operations with versions.
 */
@Component(immediate = true, label = "Sakai Versioning Service", description = "Service for doing operations with versions.")
@Service
public class VersionServiceImpl implements VersionService {

  @Property(value = "The Sakai Foundation")
  static final String SERVICE_VENDOR = "service.vendor";

  public Version saveNode(Node node, String savingUsername) throws RepositoryException {
    if (node.canAddMixin("sakai:propertiesmix")) {
      node.addMixin("sakai:propertiesmix");
    }
    node.setProperty(SAVED_BY, savingUsername);
    Session session = node.getSession();
    session.save();
    Version version = null;
    VersionManager versionManager = session.getWorkspace().getVersionManager();
    try {
      version = versionManager.checkin(node.getPath());
    } catch ( UnsupportedRepositoryOperationException e) {
      node.addMixin(JcrConstants.MIX_VERSIONABLE);
      session.save();
      version = versionManager.checkin(node.getPath());
    }
    versionManager.checkout(node.getPath());
    if ( node.getSession().hasPendingChanges() ) {
      node.getSession().save();
    }
    return version;
  }

}
