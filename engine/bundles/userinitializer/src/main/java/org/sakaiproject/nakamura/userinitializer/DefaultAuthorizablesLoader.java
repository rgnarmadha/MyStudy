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
package org.sakaiproject.nakamura.userinitializer;

import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.base.util.AccessControlUtil;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostConstants;
import org.osgi.framework.Bundle;
import org.sakaiproject.nakamura.api.user.AuthorizablePostProcessService;
import org.sakaiproject.nakamura.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * Helper class to apply some initial Authorizable properties from
 * JSON files.
 */
public class DefaultAuthorizablesLoader {
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAuthorizablesLoader.class);

  public void initDefaultUsers(AuthorizablePostProcessService authorizablePostProcessService,
      Bundle bundle, SlingRepository repository) {
    Session session = null;
    try {
      session = repository.loginAdministrative(null);
      UserManager userManager = AccessControlUtil.getUserManager(session);

      // Apply default user properties from JSON files.
      Pattern fileNamePattern = Pattern.compile("/users/|\\.json");
      @SuppressWarnings("rawtypes")
      Enumeration entriesEnum = bundle.findEntries("users", "*.json", true);
      while (entriesEnum.hasMoreElements()) {
        Object entry = entriesEnum.nextElement();
        URL jsonUrl = new URL(entry.toString());
        String jsonFileName = jsonUrl.getFile();
        String authorizableId = fileNamePattern.matcher(jsonFileName).replaceAll("");
        Authorizable authorizable = userManager.getAuthorizable(authorizableId);
        if (authorizable != null) {
          applyJsonToAuthorizable(authorizablePostProcessService, jsonUrl, authorizable, session);
          LOGGER.info("Initialized default authorizable {}", authorizableId);
        } else {
          LOGGER.warn("Configured default authorizable {} not found", authorizableId);
        }
      }
    } catch (RepositoryException e) {
      LOGGER.error("Could not configure default authorizables", e);
    } catch (IOException e) {
      LOGGER.error("Could not configure default authorizables", e);
    } finally {
      if (session != null) {
        session.logout();
      }
    }
  }

  private void applyJsonToAuthorizable(AuthorizablePostProcessService authorizablePostProcessService,
      URL url, Authorizable authorizable, Session session) throws IOException, RepositoryException {
    String jsonString = IOUtils.readFully(url.openStream(), "UTF-8");
    if (jsonString != null) {
      Map<String, Object[]> postprocessParameters = new HashMap<String, Object[]>();
      try {
        JSONObject jsonObject = new JSONObject(jsonString);
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
          String key = keys.next();
          Object jsonValue = jsonObject.get(key);
          if (key.startsWith(SlingPostConstants.RP_PREFIX)) {
            postprocessParameters.put(key, new Object[] {jsonValue});
          } else {
            if (!authorizable.hasProperty(key)) {
              Value value = JcrResourceUtil.createValue(jsonValue, session);
              authorizable.setProperty(key, value);
            }
          }
        }
      } catch (JSONException e) {
        LOGGER.error("Faulty JSON at " + url, e);
      }
      try {
        authorizablePostProcessService.process(authorizable, session,
            ModificationType.CREATE, postprocessParameters);
      } catch (Exception e) {
        LOGGER.error("Could not configure default authorizable " + authorizable.getID(), e);
      }
    }
  }
}
