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
package org.sakaiproject.nakamura.personal;

import static org.sakaiproject.nakamura.api.personal.PersonalConstants.PROFILE_JSON_IMPORT_PARAMETER;

import org.apache.sling.jcr.contentloader.ContentImporter;
import org.apache.sling.jcr.contentloader.ImportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Helper class to use a JSON string to initialize the contents of an authprofile
 * node.
 */
public class ProfileImporter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileImporter.class);
  /**
   * When passed to importContent, this string means "Do not create a new root
   * node. Use the JSON reader."
   */
  public static final String CONTENT_ROOT_NAME = ".json";

  private final static ImportOptions importOptions = new ImportOptions() {
    @Override
    public boolean isOverwrite() {
      return false;
    }
    @Override
    public boolean isCheckin() {
      return false;
    }
    @Override
    public boolean isIgnoredImportProvider(String extension) {
      return false;
    }
    @Override
    public boolean isPropertyOverwrite() {
      return true;
    }
  };

  /**
   * Import a profile using a parameter provided in the request.
   * @param profileNode
   * @param parameters
   * @param contentImporter
   * @param session
   * @param defaultJson
   */
  public static void importFromParameters(Node profileNode, Map<String, Object[]> parameters, ContentImporter contentImporter,
      Session session, String defaultJson) {
    String json = defaultJson;
    Object[] profileParameterValues = parameters.get(PROFILE_JSON_IMPORT_PARAMETER);
    if (profileParameterValues != null) {
      if ((profileParameterValues.length == 1) && (profileParameterValues[0] instanceof String)) {
        json = (String) profileParameterValues[0];
      } else {
        LOGGER.warn(
            "Improperly formatted profile import parameter: {}; using default: {}",
            profileParameterValues, defaultJson);
      }
    }

    if (json != null) {
      try {
        importFromJsonString(profileNode, json, contentImporter, session);
      } catch (RepositoryException e) {
        LOGGER.error("Unable to import content for profile node " + profileNode, e);
      } catch (IOException e) {
        LOGGER.error("Unable to import content for profile node " + profileNode, e);
      }
    }
  }

  private static void importFromJsonString(Node profileNode, String json, ContentImporter contentImporter,
      Session session) throws RepositoryException, IOException {
    ByteArrayInputStream contentStream = new ByteArrayInputStream(json.getBytes());
    contentImporter.importContent(profileNode, CONTENT_ROOT_NAME, contentStream, importOptions, null);
    LOGGER.debug("Imported content to {} from JSON string '{}'", profileNode.getPath(), json);
  }
}
