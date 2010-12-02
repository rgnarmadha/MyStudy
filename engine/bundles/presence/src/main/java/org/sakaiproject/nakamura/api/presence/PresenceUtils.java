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
package org.sakaiproject.nakamura.api.presence;

import java.io.Writer;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.sakaiproject.nakamura.util.ExtendedJSONWriter;

/**
 * Utils to handle the repetetive bits of the presence services
 */
public class PresenceUtils {

  /**
   * Method to generate the JSON related to the presence of a single user (or at least the
   * common parts of it), this will generate the entire user and the JSON writer object
   * 
   * @param writer
   *          the writer to output the JSON into
   * @param userId
   *          the user to output presence data for
   * @param presenceService
   *          the presence service to get the user presence data from
   * @throws JSONException
   */
  public static void makePresenceJSON(Writer writer, String userId,
      PresenceService presenceService) throws JSONException {
    ExtendedJSONWriter output = new ExtendedJSONWriter(writer);
    makePresenceJSON(output, userId, presenceService, false);
  }

  /**
   * Method to generate the JSON related to the presence of a single user (or at least the
   * common parts of it), this will generate the entire user and the JSON writer object
   * 
   * @param writer
   *          the writer to output the JSON into
   * @param userId
   *          the user to output presence data for
   * @param presenceService
   *          the presence service to get the user presence data from
   * @param partialObject
   *          if true then only output the keys and values but do not open or close the
   *          object (good for appending this or allowing data to be appended), if false
   *          then output a complete JSON object (opened and closed)
   * @throws JSONException
   */
  public static void makePresenceJSON(JSONWriter output, String userId,
      PresenceService presenceService, boolean partialObject) throws JSONException {
    if (!partialObject) {
      output.object();
    }
    // insert the basic status stuff
    output.key("user");
    output.value(userId);
    output.key(PresenceService.PRESENCE_STATUS_PROP);
    String status = presenceService.getStatus(userId);
    output.value(status);
    output.key(PresenceService.PRESENCE_LOCATION_PROP);
    String location = presenceService.getLocation(userId);
    output.value(location);
    if (!partialObject) {
      output.endObject();
    }
  }

}
