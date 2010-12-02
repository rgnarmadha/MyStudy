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
package org.sakaiproject.nakamura.api.configuration;

import java.util.Map;

/**
 * The configuration service gives access to a set of properties managed by OSGi.
 */
public interface ConfigurationService {

  /**
   * @return get a set of properties for local use, this set is non modifiable, and will
   *         <b>not</b> be updated if the configuration changes.
   */
  Map<String, String> getProperties();

  /**
   * @param key
   *          the key of the property to get.
   * @return get a property from the configuration, this will reflect the current state of
   *         the configuration. If the configuration changes, calling this method will
   *         reflect the current state. The underlying implementation should aim to
   *         deliver properties directly from a map so there is no need for things that
   *         use this service to store these properties locally.
   */
  String getProperty(String key);
  
  
  /**
   * @param listener the listener to add.
   */
  void addListener(ConfigurationListener listener);
  
  /**
   * @param listener the listener to remove.
   */
  void removeListener(ConfigurationListener listener);
}
