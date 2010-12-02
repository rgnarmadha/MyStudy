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
package org.sakaiproject.nakamura.pages;

import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.framework.Constants.SERVICE_VENDOR;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.sling.commons.osgi.OsgiUtil;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.sakaiproject.nakamura.api.personal.PersonalUtils;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;

/**
 * Initializes structured pages for new users and groups.
 */
@Component(immediate=true, metatype=true)
@Service
@Properties(value = {
    @Property(name=SERVICE_VENDOR, value="The Sakai Foundation"),
    @Property(name=SERVICE_DESCRIPTION, value="Initializes structured pages for new users and groups."),
    @Property(name=SERVICE_RANKING, intValue=100) })
public class PagesAuthorizablePostProcessor implements AuthorizablePostProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(PagesAuthorizablePostProcessor.class);

  @Property(value="/var/templates/pages/systemuser")
  public static final String DEFAULT_USER_PAGES_TEMPLATE = "default.user.template";
  private String defaultUserPagesTemplate;

  @Property(value="/var/templates/pages/systemgroup")
  public static final String DEFAULT_GROUP_PAGES_TEMPLATE = "default.group.template";
  private String defaultGroupPagesTemplate;

  /**
   * Path of the pages under a user or group's home folder.
   */
  public static final String PAGES_PATH = "pages";

  /**
   * Optional parameter containing the path of a Pages source that should be used instead of
   * the default template.
   */
  public static final String PAGES_TEMPLATE_PARAMETER = ":sakai:pages-template";

  public void process(Authorizable authorizable, Session session, Modification change,
      Map<String, Object[]> parameters) throws Exception {
    if (ModificationType.CREATE.equals(change.getType())) {
      final String pagesPath = PersonalUtils.getHomeFolder(authorizable) + "/" + PAGES_PATH;
      try {
        // Do not overwrite existing pages.
        if (!session.nodeExists(pagesPath)) {
          intitializeContent(authorizable, session, pagesPath, parameters);
        }
      } catch (RepositoryException e) {
        LOGGER.error("Could not create default pages for " + authorizable, e);
      }
    }
  }

  //----------- OSGi integration ----------------------------

  @Activate @Modified
  protected void init(Map<?, ?> properties) {
    defaultUserPagesTemplate = OsgiUtil.toString(properties.get(DEFAULT_USER_PAGES_TEMPLATE), "");
    defaultGroupPagesTemplate = OsgiUtil.toString(properties.get(DEFAULT_GROUP_PAGES_TEMPLATE), "");
  }

  private void intitializeContent(Authorizable authorizable, Session session,
      String pagesPath, Map<String, Object[]> parameters) throws RepositoryException {
    String templatePath = null;

    // Check for an explicit pages template path.
    Object[] templateParameterValues = parameters.get(PAGES_TEMPLATE_PARAMETER);
    if (templateParameterValues != null) {
      if ((templateParameterValues.length == 1) && templateParameterValues[0] instanceof String) {
        String templateParameterValue = (String) templateParameterValues[0];
        if (templateParameterValue.length() > 0) {
          templatePath = templateParameterValue;
        }
      } else {
        LOGGER.warn("Unexpected {} value = {}. Using defaults instead.", PAGES_TEMPLATE_PARAMETER, templateParameterValues);
      }
    }

    // If no template was specified, use the default.
    if (templatePath == null) {
      if (authorizable.isGroup()) {
        templatePath = defaultGroupPagesTemplate;
      } else {
        templatePath = defaultUserPagesTemplate;
      }
    }

    // Create pages based on the template.
    Workspace workspace = session.getWorkspace();
    workspace.copy(templatePath, pagesPath);
    LOGGER.debug("Copied template pages from {} to {}", templatePath, pagesPath);
  }
}
